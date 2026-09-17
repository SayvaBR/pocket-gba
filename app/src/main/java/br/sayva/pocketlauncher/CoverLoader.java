package br.sayva.pocketlauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Automatic, persistent artwork: exact file name, verified catalog title, then conservative title match. */
public final class CoverLoader {
    private static final String BASE = "https://thumbnails.libretro.com/";
    private static final String TAG = "PocketCovers";
    private static final Pattern LINKS = Pattern.compile("href=[\"']([^\"']+\\.png)[\"']",Pattern.CASE_INSENSITIVE);
    private static final long RETRY_MILLIS = 5*60*1000L;
    private static final long INDEX_AGE = 30L*24*60*60*1000;
    private final Context context;
    private final File directory;
    private final File catalogs;
    private final ExecutorService workers = Executors.newFixedThreadPool(4);
    private final Map<String,Long> misses = new HashMap<>();
    private final Map<String,List<String>> indexes = new HashMap<>();

    CoverLoader(Context context) {
        this.context=context.getApplicationContext();
        // CacheDir is disposable; a handheld library must keep its downloaded covers when offline.
        directory=new File(this.context.getFilesDir(),"automatic-artwork");
        catalogs=new File(directory,"catalogs");
        if(!directory.isDirectory()&&!directory.mkdirs())Log.e(TAG,"Cannot create artwork storage");
        if(!catalogs.isDirectory()&&!catalogs.mkdirs())Log.e(TAG,"Cannot create catalog storage");
    }
    public void clearFailure(LibraryStore.Game game) {
        synchronized(misses){misses.remove(game.id);}
    }
    public void into(ImageView target, LibraryStore.Game game) {
        String identity=game.id+"|"+game.coverUri+"|"+game.fileName;
        target.setTag(identity);
        target.setImageDrawable(null);
        workers.execute(()->{
            Bitmap found=null;
            try{found=find(game);}catch(Exception e){Log.w(TAG,"Artwork lookup failed for "+game.system,e);}
            Bitmap finalImage=found;
            target.post(()->{
                if(identity.equals(target.getTag())&&finalImage!=null)target.setImageBitmap(finalImage);
            });
        });
    }
    private static String system(LibraryStore.Game game){
        return switch(game.system){
            case "GBA" -> "Nintendo - Game Boy Advance";
            case "GB" -> "Nintendo - Game Boy";
            case "GBC" -> "Nintendo - Game Boy Color";
            case "SNES" -> "Nintendo - Super Nintendo Entertainment System";
            case "NDS" -> "Nintendo - Nintendo DS";
            default -> null;
        };
    }
    private Bitmap find(LibraryStore.Game game) {
        if(game.coverUri!=null&&!game.coverUri.isBlank()&&!game.coverUri.equals("null")){
            Bitmap custom=readCustom(game.coverUri);
            if(custom!=null)return custom;
        }
        String console=system(game);
        if(console==null)return null;
        File result=new File(directory,hash(game.id)+".png");
        if(result.isFile()) {
            Bitmap cached=decode(result);
            if(cached!=null)return cached;
            result.delete();
        }
        synchronized(misses){
            Long at=misses.get(game.id);
            if(at!=null&&System.currentTimeMillis()-at<RETRY_MILLIS)return null;
        }
        String original=game.fileName.replaceFirst("(?i)\\.(gba|gbc|gb|sfc|smc|nds)$","");
        ArrayList<String> choices=new ArrayList<>();
        choices.add(sanitize(original));
        choices.add(sanitize(stripTags(original)));
        String match=bestMatch(index(console),original,game.title);
        if(match!=null)choices.add(0,match);
        HashSet<String> attempted=new HashSet<>();
        for(String title:choices){
            if(title==null||title.isBlank()||!attempted.add(title))continue;
            Bitmap downloaded=download(console,title,result);
            if(downloaded!=null)return downloaded;
        }
        synchronized(misses){misses.put(game.id,System.currentTimeMillis());}
        return null;
    }
    private Bitmap download(String console,String title,File result){
        HttpURLConnection http=null;
        File part=new File(directory,hash(result.getName())+".part");
        try {
            URL url=new URL(BASE+encode(console)+"/Named_Boxarts/"+encode(title)+".png");
            http=(HttpURLConnection)url.openConnection();
            http.setConnectTimeout(6000);http.setReadTimeout(9000);
            http.setInstanceFollowRedirects(true);
            if(http.getResponseCode()!=200)return null;
            String mime=http.getContentType();
            if(mime==null||!mime.toLowerCase(Locale.ROOT).startsWith("image/"))return null;
            try(InputStream in=http.getInputStream();FileOutputStream out=new FileOutputStream(part)){
                byte[] buffer=new byte[16384];long count=0;int length;
                while((length=in.read(buffer))!=-1){
                    count+=length;if(count>6_000_000)throw new IllegalStateException("Artwork exceeded size limit");
                    out.write(buffer,0,length);
                }
                out.getFD().sync();
            }
            Bitmap bitmap=decode(part);
            if(bitmap==null)return null;
            if(!part.renameTo(result))Log.w(TAG,"Could not persist downloaded cover");
            return bitmap;
        }catch(Exception e){Log.d(TAG,"No artwork: "+title,e);return null;}
        finally{if(http!=null)http.disconnect();part.delete();}
    }
    private Bitmap decode(File file){
        try{
            BitmapFactory.Options bounds=new BitmapFactory.Options();
            bounds.inJustDecodeBounds=true;
            BitmapFactory.decodeFile(file.getAbsolutePath(),bounds);
            if(bounds.outWidth<1||bounds.outHeight<1)return null;
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize=1;
            while(Math.max(bounds.outWidth/options.inSampleSize,bounds.outHeight/options.inSampleSize)>1200)
                options.inSampleSize*=2;
            return BitmapFactory.decodeFile(file.getAbsolutePath(),options);
        }catch(Exception e){Log.w(TAG,"Invalid cover bitmap",e);return null;}
    }
    private Bitmap readCustom(String uri) {
        try {
            Uri location=Uri.parse(uri);
            BitmapFactory.Options bounds=new BitmapFactory.Options();
            bounds.inJustDecodeBounds=true;
            try(InputStream in=context.getContentResolver().openInputStream(location)){
                if(in==null)return null;BitmapFactory.decodeStream(in,null,bounds);
            }
            if(bounds.outWidth<1||bounds.outHeight<1)return null;
            BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=1;
            while(Math.max(bounds.outWidth/opts.inSampleSize,bounds.outHeight/opts.inSampleSize)>1200)
                opts.inSampleSize*=2;
            try(InputStream in=context.getContentResolver().openInputStream(location)){
                return in==null?null:BitmapFactory.decodeStream(in,null,opts);
            }
        }catch(Exception e){Log.w(TAG,"Custom cover could not be read",e);return null;}
    }
    private synchronized List<String> index(String console) {
        List<String> resident=indexes.get(console);
        if(resident!=null)return resident;
        File disk=new File(catalogs,hash(console)+".txt");
        if(disk.isFile())try {
            List<String> names=Files.readAllLines(disk.toPath(),StandardCharsets.UTF_8);
            if(!names.isEmpty()){
                List<String> value=Collections.unmodifiableList(names);
                indexes.put(console,value);
                // Use existing index even when stale; covers remain available offline.
                if(System.currentTimeMillis()-disk.lastModified()<INDEX_AGE)return value;
            }
        }catch(Exception e){Log.w(TAG,"Catalog cache invalid",e);}
        ArrayList<String> downloaded=new ArrayList<>();
        HttpURLConnection http=null;
        try{
            http=(HttpURLConnection)new URL(BASE+encode(console)+"/Named_Boxarts/").openConnection();
            http.setConnectTimeout(6500);http.setReadTimeout(12000);
            if(http.getResponseCode()!=200)throw new IllegalStateException("Catalog HTTP "+http.getResponseCode());
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(http.getInputStream(),StandardCharsets.UTF_8))){
                String line;long bytes=0;
                while((line=reader.readLine())!=null){
                    bytes+=line.length();if(bytes>16_000_000)throw new IllegalStateException("Catalog too large");
                    Matcher matcher=LINKS.matcher(line);
                    while(matcher.find()){
                        try{
                            String encoded=matcher.group(1).replace("&amp;","&");
                            String filename=URLDecoder.decode(encoded.replace("+","%2B"),StandardCharsets.UTF_8);
                            if(!filename.contains("/")&&filename.toLowerCase(Locale.ROOT).endsWith(".png"))
                                downloaded.add(filename.substring(0,filename.length()-4));
                        }catch(IllegalArgumentException ignored){}
                    }
                }
            }
            if(!downloaded.isEmpty()){
                File tmp=new File(catalogs,hash(console)+".part");
                Files.write(tmp.toPath(),downloaded,StandardCharsets.UTF_8);
                if(!tmp.renameTo(disk))tmp.delete();
                List<String> immutable=Collections.unmodifiableList(downloaded);
                indexes.put(console,immutable);
                return immutable;
            }
        }catch(Exception error){Log.w(TAG,"Catalog lookup unavailable for "+console,error);}
        finally{if(http!=null)http.disconnect();}
        return indexes.getOrDefault(console,Collections.emptyList());
    }
    /** Never choose artwork solely by sharing a word; false covers are worse than placeholders. */
    static String bestMatch(List<String> names,String filename,String display){
        String full=norm(filename),shortName=norm(stripTags(filename));
        String shortDisplay=norm(stripTags(display));
        String winner=null;int winnerScore=0;
        for(String entry:names){
            String actual=norm(entry),clean=norm(stripTags(entry));
            String rotated=norm(fixArticle(stripTags(entry)));
            int score=0;
            if(actual.equals(full))score=120;
            else if(clean.equals(shortName)||rotated.equals(shortName))score=105;
            else if(clean.equals(shortDisplay)||rotated.equals(shortDisplay))score=100;
            else if(shortName.length()>=12&&clean.length()>=12&&
                (clean.startsWith(shortName+" ")||shortName.startsWith(clean+" "))&&
                Math.min(shortName.length(),clean.length())*1.0/Math.max(shortName.length(),clean.length())>.80)
                score=78;
            if(score==0)continue;
            if(entry.contains("(USA)"))score+=4;
            if(entry.contains("(USA, Europe)"))score+=3;
            if(entry.contains("(Europe)"))score+=2;
            if(entry.contains("(Beta)")||entry.contains("(Proto)")||entry.contains("(Demo)"))score-=12;
            if(score>winnerScore){winner=entry;winnerScore=score;}
        }
        return winner;
    }
    private static String stripTags(String name){
        return name.replaceAll("\\s*\\([^)]*\\)","").replaceAll("\\s*\\[[^]]*]","").trim();
    }
    private static String fixArticle(String name){
        return name.replaceFirst("(?i)^(.+?), The(\\s*-.*)?$","The $1$2");
    }
    private static String sanitize(String name){
        return name.replaceAll("[&*/:`<>?\\\\|\"]","_").trim();
    }
    private static String norm(String name){
        return Normalizer.normalize(name,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();
    }
    private static String encode(String name){
        return URLEncoder.encode(name,StandardCharsets.UTF_8).replace("+","%20");
    }
    private static String hash(String text){
        try{
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result=new StringBuilder();
            for(byte b:bytes)result.append(String.format(Locale.ROOT,"%02x",b&255));
            return result.toString();
        }catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);}
    }
}

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
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Manual image URI -> cached image -> indexed Libretro titles -> visible placeholder. */
public final class CoverLoader {
    private static final String BASE="https://thumbnails.libretro.com/";
    private static final Pattern LINKS=Pattern.compile("href=\"([^\"]+\\.png)\"",Pattern.CASE_INSENSITIVE);
    private static final long RETRY_AFTER_MILLIS=5*60*1000L;
    private final Context context;
    private final File directory;
    private final ExecutorService pool=Executors.newFixedThreadPool(3);
    private final Map<String,Long> misses=new HashMap<>();
    private final Map<String,List<String>> indexes=new HashMap<>();
    CoverLoader(Context context) {
        this.context=context.getApplicationContext();
        directory=new File(context.getCacheDir(),"artwork");directory.mkdirs();
    }
    public void clearFailure(LibraryStore.Game game) {
        synchronized(misses){misses.remove(game.id);}
    }
    public void into(ImageView target,LibraryStore.Game game) {
        final String requested=game.id+"|"+game.coverUri+"|"+game.title;
        target.setTag(requested);
        target.setImageDrawable(null);
        pool.execute(()->{
            Bitmap bitmap=find(game);
            target.post(()->{if(requested.equals(target.getTag())&&bitmap!=null)target.setImageBitmap(bitmap);});
        });
    }
    private Bitmap readCustom(String uri) {
        try {
            Uri location=Uri.parse(uri);
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
            try(InputStream in=context.getContentResolver().openInputStream(location)){
                if(in==null)return null;
                BitmapFactory.decodeStream(in,null,bounds);
            }
            if(bounds.outWidth<1||bounds.outHeight<1)return null;
            BitmapFactory.Options options=new BitmapFactory.Options();
            while(Math.max(bounds.outWidth/options.inSampleSize,bounds.outHeight/options.inSampleSize)>1024)
                options.inSampleSize*=2;
            try(InputStream in=context.getContentResolver().openInputStream(location)){
                return in==null?null:BitmapFactory.decodeStream(in,null,options);
            }
        }catch(Exception e){Log.w("PocketCovers","Could not read selected cover",e);return null;}
    }
    private Bitmap find(LibraryStore.Game game) {
        if(game.coverUri!=null&&!game.coverUri.isEmpty()) {
            Bitmap custom=readCustom(game.coverUri);
            if(custom!=null)return custom;
        }
        String system=switch(game.system){
            case "GBA" -> "Nintendo - Game Boy Advance";
            case "GB" -> "Nintendo - Game Boy";
            case "GBC" -> "Nintendo - Game Boy Color";
            case "SNES" -> "Nintendo - Super Nintendo Entertainment System";
            case "NDS" -> "Nintendo - Nintendo DS";
            default -> null;
        };
        if(system==null)return null;
        String key=game.id;
        File dest=new File(directory,hash(key)+".png");
        if(dest.isFile()) {
            Bitmap image=BitmapFactory.decodeFile(dest.getAbsolutePath());
            if(image!=null)return image;
            dest.delete();
        }
        synchronized(misses){
            Long failedAt=misses.get(key);
            if(failedAt!=null && System.currentTimeMillis()-failedAt<RETRY_AFTER_MILLIS)return null;
        }
        String raw=game.fileName.replaceFirst("(?i)\\.(gba|gbc|gb|smc|sfc|nds)$","");
        String cleaned=sanitize(raw);
        String shortTitle=sanitize(stripTags(raw));
        String indexed=bestMatch(system,raw,game.title);
        HashSet<String> attempted=new HashSet<>();
        ArrayList<String> candidates=new ArrayList<>();
        if(indexed!=null)candidates.add(indexed);
        candidates.add(cleaned);candidates.add(shortTitle);
        for(String title:candidates){
            if(title.isBlank()||!attempted.add(title))continue;
            HttpURLConnection connection=null;
            File temp=new File(directory,hash(key)+".part");
            try {
                String path=encode(system)+"/Named_Boxarts/"+encode(title)+".png";
                connection=(HttpURLConnection)new URL(BASE+path).openConnection();
                connection.setConnectTimeout(6500);connection.setReadTimeout(6500);
                connection.setInstanceFollowRedirects(true);
                String mime=connection.getContentType();
                if(connection.getResponseCode()!=200||mime==null||!mime.startsWith("image/"))continue;
                try(InputStream input=connection.getInputStream();FileOutputStream output=new FileOutputStream(temp)){
                    byte[] chunk=new byte[8192];int count,total=0;
                    while((count=input.read(chunk))!=-1){
                        total+=count;if(total>3_000_000)throw new IllegalStateException("Oversized art");
                        output.write(chunk,0,count);
                    }
                }
                Bitmap result=BitmapFactory.decodeFile(temp.getAbsolutePath());
                if(result!=null){
                    if(!temp.renameTo(dest))Log.w("PocketCovers","Could not cache "+title);
                    return result;
                }
            }catch(Exception error){Log.d("PocketCovers","Art lookup failed for "+title,error);}
            finally{if(connection!=null)connection.disconnect();temp.delete();}
        }
        synchronized(misses){misses.put(key,System.currentTimeMillis());}
        return null;
    }
    /** Load public directory listing once per system; filenames follow No-Intro naming, not arbitrary ROM names. */
    private synchronized List<String> catalog(String system) {
        if(indexes.containsKey(system))return indexes.get(system);
        ArrayList<String> names=new ArrayList<>();
        HttpURLConnection connection=null;
        try {
            URL url=new URL(BASE+encode(system)+"/Named_Boxarts/");
            connection=(HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(6500);connection.setReadTimeout(9500);
            if(connection.getResponseCode()!=200)throw new IllegalStateException("Index unavailable");
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),StandardCharsets.UTF_8))){
                String line;int total=0;
                while((line=reader.readLine())!=null){
                    total+=line.length();if(total>4_000_000)throw new IllegalStateException("Oversized cover index");
                    Matcher matcher=LINKS.matcher(line);
                    while(matcher.find()){
                        String path=matcher.group(1).replace("&amp;","&");
                        try {
                            String filename=URLDecoder.decode(path.replace("+","%2B"),StandardCharsets.UTF_8);
                            if(filename.endsWith(".png")&&!filename.contains("/"))
                                names.add(filename.substring(0,filename.length()-4));
                        }catch(IllegalArgumentException ignored){}
                    }
                }
            }
        }catch(Exception e){Log.w("PocketCovers","Catalog fetch unavailable for "+system,e);}
        finally{if(connection!=null)connection.disconnect();}
        // Do not cache a network failure forever; a later request can retry after connectivity returns.
        if(!names.isEmpty())indexes.put(system,Collections.unmodifiableList(names));
        return names;
    }
    private String bestMatch(String system,String filename,String displayTitle) {
        String full=norm(filename),shortName=norm(stripTags(filename));
        String display=norm(displayTitle),displayShort=norm(stripTags(displayTitle));
        String article=norm(fixArticle(stripTags(filename)));
        String best=null;int bestScore=0;
        for(String entry:catalog(system)){
            String normal=norm(entry),shortEntry=norm(stripTags(entry));
            int score=0;
            if(normal.equals(full)||normal.equals(display))score=110;
            else if(shortEntry.equals(shortName)||shortEntry.equals(displayShort)||shortEntry.equals(article))score=100;
            else if(shortName.length()>=9 && shortEntry.length()>=9 &&
                (shortEntry.startsWith(shortName+" ")||shortName.startsWith(shortEntry+" ")) &&
                Math.min(shortName.length(),shortEntry.length())*1.0/Math.max(shortName.length(),shortEntry.length())>=0.91)
                score=80;
            if(score==0)continue;
            if(entry.contains("(USA)"))score+=4;
            if(entry.contains("(Europe)"))score+=2;
            if(entry.contains("(Beta)")||entry.contains("(Proto)")||entry.contains("(Demo)"))score-=10;
            if(score>bestScore){best=entry;bestScore=score;}
        }
        return best;
    }
    private static String stripTags(String title){
        return title.replaceAll("\\s*\\([^)]*\\)","").replaceAll("\\s*\\[[^]]*]","").trim();
    }
    private static String fixArticle(String title){
        // No-Intro often uses "Legend of Zelda, The - ..." while user files start "The Legend...".
        return title.replaceFirst("(?i)^(.+?), The(\\s*-.*)?$","The $1$2");
    }
    private static String sanitize(String title){
        return title.replaceAll("[&*/:`<>?\\\\|\"]","_").trim();
    }
    private static String norm(String title){
        return Normalizer.normalize(title,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
            .toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();
    }
    private String encode(String text){return URLEncoder.encode(text,StandardCharsets.UTF_8).replace("+","%20");}
    private String hash(String key){
        try {byte[] digest=MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder result=new StringBuilder();for(byte b:digest)result.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return result.toString();
        }catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);}
    }
}

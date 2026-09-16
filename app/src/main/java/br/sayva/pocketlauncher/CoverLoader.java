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

/** Local override first; otherwise match exact/normalized catalog titles, never unrelated game art. */
public final class CoverLoader {
    private static final String BASE="https://thumbnails.libretro.com/";
    private static final Pattern LINKS=Pattern.compile("href=\"([^\"]+\\.png)\"",Pattern.CASE_INSENSITIVE);
    private static final long RETRY_AFTER_MILLIS=5*60*1000L;
    private final Context context;
    private final File directory;
    private final ExecutorService pool=Executors.newFixedThreadPool(3);
    private final Map<String,Long> misses=new HashMap<>();
    private final Map<String,List<String>> indexes=new HashMap<>();
    CoverLoader(Context context){
        this.context=context.getApplicationContext();
        directory=new File(context.getCacheDir(),"artwork");directory.mkdirs();
    }
    public void clearFailure(LibraryStore.Game game){synchronized(misses){misses.remove(game.id);}}
    public void into(ImageView target,LibraryStore.Game game){
        String requested=game.id+"|"+game.coverUri+"|"+game.title;
        target.setTag(requested);target.setImageDrawable(null);
        pool.execute(()->{
            Bitmap bitmap=null;
            try{bitmap=find(game);}catch(Exception error){Log.w("PocketCovers","Artwork not available",error);}
            Bitmap result=bitmap;
            target.post(()->{if(requested.equals(target.getTag())&&result!=null)target.setImageBitmap(result);});
        });
    }
    private Bitmap readCustom(String uri){
        try {
            Uri location=Uri.parse(uri);
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
            try(InputStream in=context.getContentResolver().openInputStream(location)){
                if(in==null)return null;BitmapFactory.decodeStream(in,null,bounds);
            }
            if(bounds.outWidth<1||bounds.outHeight<1)return null;
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize=1; // Android defaults this int to zero: do not divide by zero.
            while(Math.max(bounds.outWidth/options.inSampleSize,bounds.outHeight/options.inSampleSize)>1024)
                options.inSampleSize*=2;
            try(InputStream in=context.getContentResolver().openInputStream(location)){
                return in==null?null:BitmapFactory.decodeStream(in,null,options);
            }
        }catch(Exception e){Log.w("PocketCovers","Selected cover cannot be read",e);return null;}
    }
    private Bitmap find(LibraryStore.Game game){
        if(game.coverUri!=null&&!game.coverUri.isEmpty()&&!game.coverUri.equals("null")){
            Bitmap custom=readCustom(game.coverUri);if(custom!=null)return custom;
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
        File destination=new File(directory,hash(key)+".png");
        if(destination.isFile()){
            Bitmap cached=BitmapFactory.decodeFile(destination.getAbsolutePath());
            if(cached!=null)return cached;
            destination.delete();
        }
        synchronized(misses){
            Long lastMiss=misses.get(key);
            if(lastMiss!=null&&System.currentTimeMillis()-lastMiss<RETRY_AFTER_MILLIS)return null;
        }
        String original=game.fileName.replaceFirst("(?i)\\.(gba|gbc|gb|smc|sfc|nds)$","");
        String catalogTitle=bestMatch(system,original,game.title);
        ArrayList<String> candidates=new ArrayList<>();
        if(catalogTitle!=null)candidates.add(catalogTitle);
        candidates.add(sanitize(original));candidates.add(sanitize(stripTags(original)));
        HashSet<String> attempted=new HashSet<>();
        for(String title:candidates){
            if(title.isBlank()||!attempted.add(title))continue;
            HttpURLConnection connection=null;
            File temporary=new File(directory,hash(key)+".part");
            try {
                connection=(HttpURLConnection)new URL(BASE+encode(system)+"/Named_Boxarts/"+encode(title)+".png").openConnection();
                connection.setConnectTimeout(6500);connection.setReadTimeout(6500);
                connection.setInstanceFollowRedirects(true);
                int response=connection.getResponseCode();
                String mime=connection.getContentType();
                if(response!=200||mime==null||!mime.toLowerCase(java.util.Locale.ROOT).startsWith("image/"))continue;
                try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(temporary)){
                    byte[] chunk=new byte[8192];int count;long total=0;
                    while((count=in.read(chunk))!=-1){
                        total+=count;if(total>3_000_000)throw new IllegalStateException("Art exceeds size limit");
                        out.write(chunk,0,count);
                    }
                }
                Bitmap image=BitmapFactory.decodeFile(temporary.getAbsolutePath());
                if(image!=null){
                    if(!temporary.renameTo(destination))Log.w("PocketCovers","Cache write failed for "+title);
                    return image;
                }
            }catch(Exception error){Log.d("PocketCovers","Cover fetch failed for "+title,error);}
            finally{if(connection!=null)connection.disconnect();temporary.delete();}
        }
        synchronized(misses){misses.put(key,System.currentTimeMillis());}
        return null;
    }
    private synchronized List<String> catalog(String system){
        if(indexes.containsKey(system))return indexes.get(system);
        ArrayList<String> names=new ArrayList<>();HttpURLConnection connection=null;
        try {
            connection=(HttpURLConnection)new URL(BASE+encode(system)+"/Named_Boxarts/").openConnection();
            connection.setConnectTimeout(6500);connection.setReadTimeout(9500);
            if(connection.getResponseCode()!=200)throw new IllegalStateException("Cover index unavailable");
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),StandardCharsets.UTF_8))){
                String line;int total=0;
                while((line=reader.readLine())!=null){
                    total+=line.length();if(total>4_000_000)throw new IllegalStateException("Cover index too large");
                    Matcher matcher=LINKS.matcher(line);
                    while(matcher.find()){
                        try {
                            String raw=matcher.group(1).replace("&amp;","&");
                            String filename=URLDecoder.decode(raw.replace("+","%2B"),StandardCharsets.UTF_8);
                            if(filename.toLowerCase(java.util.Locale.ROOT).endsWith(".png")&&!filename.contains("/"))
                                names.add(filename.substring(0,filename.length()-4));
                        }catch(IllegalArgumentException ignored){}
                    }
                }
            }
        }catch(Exception e){Log.w("PocketCovers","Catalog unavailable for "+system,e);}
        finally{if(connection!=null)connection.disconnect();}
        if(!names.isEmpty())indexes.put(system,Collections.unmodifiableList(names));
        return names;
    }
    private String bestMatch(String system,String filename,String title){
        String full=norm(filename),shortName=norm(stripTags(filename));
        String display=norm(title),displayShort=norm(stripTags(title));
        String best=null;int bestScore=0;
        for(String entry:catalog(system)){
            String normal=norm(entry),shortEntry=norm(stripTags(entry));
            String rotated=norm(fixArticle(stripTags(entry)));
            int score=0;
            if(normal.equals(full)||normal.equals(display))score=110;
            else if(shortEntry.equals(shortName)||shortEntry.equals(displayShort)||rotated.equals(shortName)||rotated.equals(displayShort))score=100;
            else if(shortName.length()>=9&&shortEntry.length()>=9&&
                (shortEntry.startsWith(shortName+" ")||shortName.startsWith(shortEntry+" "))&&
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
    private static String stripTags(String text){
        return text.replaceAll("\\s*\\([^)]*\\)","").replaceAll("\\s*\\[[^]]*]","").trim();
    }
    private static String fixArticle(String title){
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
        try{
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder value=new StringBuilder();
            for(byte b:digest)value.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            return value.toString();
        }catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);}
    }
}

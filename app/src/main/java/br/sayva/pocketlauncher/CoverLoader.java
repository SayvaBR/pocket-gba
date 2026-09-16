package br.sayva.pocketlauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Only automatic covers: local cache -> public Libretro thumbnails -> graceful placeholder. */
public final class CoverLoader {
    private final File directory;
    private final ExecutorService pool=Executors.newFixedThreadPool(3);
    private final HashSet<String> misses=new HashSet<>();
    CoverLoader(Context context) {directory=new File(context.getCacheDir(),"artwork");directory.mkdirs();}
    public void into(ImageView target,LibraryStore.Game game) {
        final String key=game.system+"|"+game.fileName;
        target.setTag(key);
        pool.execute(() -> {
            Bitmap bitmap=find(key,game);
            target.post(() -> {if(key.equals(target.getTag()) && bitmap!=null)target.setImageBitmap(bitmap);});
        });
    }
    private Bitmap find(String key,LibraryStore.Game game) {
        String system=switch(game.system) {
            case "GBA" -> "Nintendo - Game Boy Advance";
            case "GB" -> "Nintendo - Game Boy";
            case "GBC" -> "Nintendo - Game Boy Color";
            case "SNES" -> "Nintendo - Super Nintendo Entertainment System";
            case "NDS" -> "Nintendo - Nintendo DS";
            default -> null;
        };
        if(system==null)return null;
        File dest=new File(directory,hash(key)+".png");
        if(dest.isFile())return BitmapFactory.decodeFile(dest.getAbsolutePath());
        synchronized(misses){if(misses.contains(key))return null;}
        String raw=game.fileName.replaceFirst("(?i)\\.(gba|gbc|gb|smc|sfc|nds)$","");
        String cleaned=raw.replaceAll("[&*/:`<>?\\\\|]","_");
        String shortTitle=cleaned.replaceAll("\\s*\\([^)]*\\)","").replaceAll("\\s*\\[[^]]*]","").trim();
        for(String title:new String[]{cleaned,shortTitle}) {
            if(title.isEmpty())continue;
            HttpURLConnection connection=null;
            File temp=new File(directory,hash(key)+".part");
            try {
                String path=encode(system)+"/Named_Boxarts/"+encode(title)+".png";
                connection=(HttpURLConnection)new URL("https://thumbnails.libretro.com/"+path).openConnection();
                connection.setConnectTimeout(6500);connection.setReadTimeout(6500);
                connection.setInstanceFollowRedirects(true);
                if(connection.getResponseCode()!=200 || !connection.getContentType().startsWith("image/"))continue;
                try(InputStream input=connection.getInputStream();FileOutputStream output=new FileOutputStream(temp)) {
                    byte[] chunk=new byte[8192];int count,total=0;
                    while((count=input.read(chunk))!=-1) {
                        total+=count;if(total>3_000_000)throw new IllegalStateException("Oversized art");
                        output.write(chunk,0,count);
                    }
                }
                Bitmap result=BitmapFactory.decodeFile(temp.getAbsolutePath());
                if(result!=null) {
                    if(!temp.renameTo(dest))dest.delete();
                    return result;
                }
            }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();temp.delete();}
        }
        synchronized(misses){misses.add(key);}
        return null;
    }
    private String encode(String text){try{return URLEncoder.encode(text,"UTF-8").replace("+","%20");}catch(Exception e){return text;}}
    private String hash(String key){
        try {byte[] digest=MessageDigest.getInstance("SHA-256").digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder result=new StringBuilder();for(byte b:digest)result.append(String.format("%02x",b&0xff));return result.toString();
        }catch(Exception e){return Integer.toHexString(key.hashCode());}
    }
}

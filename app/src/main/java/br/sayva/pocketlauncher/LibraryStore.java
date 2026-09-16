package br.sayva.pocketlauncher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Local-first library. An unreadable source never deletes its previously indexed games. */
public final class LibraryStore {
    public static final class Root {
        public String uri, label;
        Root(String uri, String label) { this.uri=uri; this.label=label; }
    }
    public static final class Game {
        public String id, uri, fileName, title, system, coverUri;
        public boolean favorite, hidden;
        public long lastPlayed;
        Game(String id, String uri, String fileName, String title, String system) {
            this.id=id; this.uri=uri; this.fileName=fileName; this.title=title; this.system=system;
        }
    }
    public static final class ScanReport {
        public int found, added, errors;
        public final ArrayList<String> warnings = new ArrayList<>();
    }
    private final Context context;
    private final SharedPreferences prefs;
    public final ArrayList<Root> roots = new ArrayList<>();
    public final ArrayList<Game> games = new ArrayList<>();
    public LibraryStore(Context context) {
        this.context=context.getApplicationContext();
        prefs=this.context.getSharedPreferences("pocket_library_v1", Context.MODE_PRIVATE);
        load();
    }
    private void load() {
        try {
            JSONObject data=new JSONObject(prefs.getString("library", "{}"));
            JSONArray folders=data.optJSONArray("roots");
            if(folders!=null) for(int i=0;i<folders.length();i++) {
                JSONObject r=folders.getJSONObject(i);
                roots.add(new Root(r.getString("uri"),r.optString("label","Jogos")));
            }
            JSONArray entries=data.optJSONArray("games");
            if(entries!=null) for(int i=0;i<entries.length();i++) {
                JSONObject g=entries.getJSONObject(i);
                Game e=new Game(g.getString("id"),g.getString("uri"),g.getString("file"),g.getString("title"),g.getString("system"));
                e.favorite=g.optBoolean("favorite"); e.hidden=g.optBoolean("hidden"); e.lastPlayed=g.optLong("played");
                e.coverUri=g.optString("cover",null);
                games.add(e);
            }
        } catch(Exception e) { android.util.Log.e("PocketLibrary","Invalid saved library; keeping original preference untouched",e); }
    }
    private synchronized void save() {
        try {
            JSONObject data=new JSONObject(); JSONArray folders=new JSONArray(); JSONArray entries=new JSONArray();
            for(Root r:roots) folders.put(new JSONObject().put("uri",r.uri).put("label",r.label));
            for(Game g:games) entries.put(new JSONObject().put("id",g.id).put("uri",g.uri)
                .put("file",g.fileName).put("title",g.title).put("system",g.system)
                .put("favorite",g.favorite).put("hidden",g.hidden).put("played",g.lastPlayed)
                .put("cover",g.coverUri==null?JSONObject.NULL:g.coverUri));
            data.put("roots",folders).put("games",entries);
            if(!prefs.edit().putString("library",data.toString()).commit())
                throw new IllegalStateException("Android did not persist the library");
        } catch(Exception e) { android.util.Log.e("PocketLibrary","Could not persist library",e); }
    }
    public synchronized void addFolder(Uri treeUri, int resultFlags) {
        int granted = resultFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if((granted & Intent.FLAG_GRANT_READ_URI_PERMISSION)==0) throw new SecurityException("Permissão de leitura não concedida");
        context.getContentResolver().takePersistableUriPermission(treeUri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
        for(Root root:roots) if(root.uri.equals(treeUri.toString())) return;
        String name=DocumentsContract.getTreeDocumentId(treeUri);
        name=name.substring(name.lastIndexOf('/')+1).replace(':',' ');
        roots.add(new Root(treeUri.toString(),name.isEmpty()?"Jogos":name));
        save();
    }
    public synchronized void removeFolder(Root root) { roots.removeIf(r -> r.uri.equals(root.uri)); save(); }
    public synchronized void edit(Game game,String title,Boolean favorite,Boolean hidden) {
        for(Game current:games)if(current.id.equals(game.id)) {
            if(title!=null && !title.trim().isEmpty()) current.title=title.trim();
            if(favorite!=null) current.favorite=favorite;
            if(hidden!=null) current.hidden=hidden;
            save();return;
        }
    }
    public synchronized void setCover(Game game,String contentUri){
        for(Game current:games)if(current.id.equals(game.id)) {current.coverUri=contentUri;save();return;}
    }
    public synchronized void played(Game game) {
        for(Game current:games)if(current.id.equals(game.id)) {current.lastPlayed=System.currentTimeMillis();save();return;}
    }
    public synchronized List<Game> snapshot() { return new ArrayList<>(games); }
    public static String systemFor(String name) {
        String n=name.toLowerCase(Locale.ROOT);
        if(n.endsWith(".gba"))return "GBA";
        if(n.endsWith(".gbc"))return "GBC";
        if(n.endsWith(".gb"))return "GB";
        if(n.endsWith(".sfc")||n.endsWith(".smc"))return "SNES";
        if(n.endsWith(".nds"))return "NDS";
        return null;
    }
    public static String cleanTitle(String file) {
        String base=file.replaceFirst("(?i)\\.(gba|gbc|gb|sfc|smc|nds)$","");
        return base.replace('_',' ').replaceAll("\\s+"," ").trim();
    }
    public ScanReport scan() {
        ScanReport report=new ScanReport();
        List<Root> sources;
        synchronized(this) { sources=new ArrayList<>(roots); }
        HashSet<String> grants=new HashSet<>();
        for(android.content.UriPermission p:context.getContentResolver().getPersistedUriPermissions())
            if(p.isReadPermission()) grants.add(p.getUri().toString());
        for(Root source:sources) {
            if(!grants.contains(source.uri)) {
                report.errors++;report.warnings.add("Sem acesso à pasta: "+source.label+". Jogos antigos preservados.");
                continue;
            }
            try { scanTree(Uri.parse(source.uri),report); }
            catch(Exception e) {
                report.errors++;report.warnings.add("Falha em "+source.label+": jogos existentes preservados.");
                android.util.Log.w("PocketLibrary","Cannot index "+source.label,e);
            }
        }
        return report;
    }
    private void scanTree(Uri tree,ScanReport report) {
        ArrayDeque<String> pending=new ArrayDeque<>(); HashSet<String> visited=new HashSet<>();
        pending.add(DocumentsContract.getTreeDocumentId(tree));
        final String[] projection={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE};
        while(!pending.isEmpty() && visited.size()<20000) {
            String parent=pending.removeFirst(); if(!visited.add(parent))continue;
            Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parent);
            Cursor cursor=context.getContentResolver().query(children,projection,null,null,null);
            if(cursor==null)throw new IllegalStateException("Android provider returned no directory cursor");
            try(cursor) {
                int idColumn=cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
                int nameColumn=cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                int typeColumn=cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);
                while(cursor.moveToNext()) {
                    String id=cursor.getString(idColumn), name=cursor.getString(nameColumn), type=cursor.getString(typeColumn);
                    if(id==null||name==null)continue;
                    if(DocumentsContract.Document.MIME_TYPE_DIR.equals(type)) { pending.add(id);continue; }
                    String system=systemFor(name);if(system==null)continue;
                    report.found++;
                    Uri fileUri=DocumentsContract.buildDocumentUriUsingTree(tree,id);
                    String identity=tree.getAuthority()+"|"+id;
                    synchronized(this) {
                        Game previous=null;
                        for(Game g:games) if(g.id.equals(identity)) { previous=g;break; }
                        if(previous==null) {
                            games.add(new Game(identity,fileUri.toString(),name,cleanTitle(name),system));
                            report.added++;
                        } else previous.uri=fileUri.toString();
                    }
                }
            }
            synchronized(this) { save(); }
        }
        if(!pending.isEmpty()) { report.errors++;report.warnings.add("Limite de pastas atingido. Entradas existentes preservadas."); }
    }
}

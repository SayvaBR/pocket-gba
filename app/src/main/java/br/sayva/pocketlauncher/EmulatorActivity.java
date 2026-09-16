package br.sayva.pocketlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.trebuchetdynamics.emulator.mgba.MgbaSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/** Own playable GBA / Game Boy / Color screen. Core is built from pinned upstream source. */
public final class EmulatorActivity extends Activity {
    static final String GAME_ID="pocket_game_id";
    private static final String TAG="PocketGameplay";
    private static final long FRAME_NANOS=16_743_000L;
    private final AtomicInteger keys=new AtomicInteger();
    private final AtomicInteger command=new AtomicInteger(); // 1 save, 2 load
    private volatile boolean running;
    private Thread worker;
    private LibraryStore.Game game;
    private TextView status;
    private SurfaceView screen;
    private File temporaryRom, batterySave, quickSave, autoSave;
    private volatile boolean loaded;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(PocketUi.BG);
        getWindow().setNavigationBarColor(PocketUi.BG);
        String id=getIntent().getStringExtra(GAME_ID);
        for(LibraryStore.Game entry:new LibraryStore(this).snapshot()) {
            if(entry.id.equals(id)){game=entry;break;}
        }
        if(game==null || !(game.system.equals("GBA")||game.system.equals("GB")||game.system.equals("GBC"))) {
            new AlertDialog.Builder(this).setTitle("Jogo indisponível")
                .setMessage("Não foi possível localizar uma ROM compatível na biblioteca.")
                .setPositiveButton("Voltar",(d,w)->finish()).setCancelable(false).show();
            return;
        }
        File saves=new File(getFilesDir(),"game-saves");
        if(!saves.exists()&&!saves.mkdirs()) {fail("Não foi possível criar a pasta de saves.");return;}
        String token=digest(game.id);
        batterySave=new File(saves,token+".sav");
        quickSave=new File(saves,token+".state");
        autoSave=new File(saves,token+".auto");
        File cache=new File(getCacheDir(),"playable-roms");
        if(!cache.exists()&&!cache.mkdirs()) {fail("Não foi possível criar a pasta temporária da ROM.");return;}
        temporaryRom=new File(cache,token+"."+game.system.toLowerCase(java.util.Locale.ROOT));
        buildLayout();
    }

    private void buildLayout() {
        LinearLayout root=PocketUi.vertical(this);
        root.setBackgroundColor(PocketUi.BG);
        root.setPadding(PocketUi.dp(this,12),PocketUi.dp(this,9),PocketUi.dp(this,12),PocketUi.dp(this,12));
        root.setOnApplyWindowInsetsListener((v,insets)-> {
            if(android.os.Build.VERSION.SDK_INT>=30){
                android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                v.setPadding(PocketUi.dp(this,12)+safe.left,PocketUi.dp(this,9)+safe.top,
                    PocketUi.dp(this,12)+safe.right,PocketUi.dp(this,12)+safe.bottom);
            }else v.setPadding(PocketUi.dp(this,12)+insets.getSystemWindowInsetLeft(),
                PocketUi.dp(this,9)+insets.getSystemWindowInsetTop(),
                PocketUi.dp(this,12)+insets.getSystemWindowInsetRight(),
                PocketUi.dp(this,12)+insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout toolbar=PocketUi.horizontal(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=PocketUi.action(this,"‹  Voltar",false);
        back.setOnClickListener(v->showMenu());toolbar.addView(back,PocketUi.lp(this,94,45,0,0,7,0));
        TextView title=PocketUi.text(this,game.title,15,PocketUi.TEXT,true);title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        toolbar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView menu=PocketUi.action(this,"☰",false);menu.setContentDescription("Menu do jogo");
        menu.setOnClickListener(v->showMenu());toolbar.addView(menu,PocketUi.lp(this,53,45,6,0,0,0));
        root.addView(toolbar);
        status=PocketUi.text(this,"Carregando ROM no mGBA...",12,PocketUi.MUTED,false);
        status.setGravity(Gravity.CENTER);root.addView(status,PocketUi.lp(this,-1,34,0,3,0,9));
        screen=new SurfaceView(this);
        screen.setBackgroundColor(Color.BLACK);
        int width=getResources().getDisplayMetrics().widthPixels-PocketUi.dp(this,24);
        int height=Math.min((int)(width*0.667f),getResources().getDisplayMetrics().heightPixels/3);
        FrameLayout viewport=new FrameLayout(this);viewport.setBackgroundColor(Color.BLACK);
        viewport.addView(screen,new FrameLayout.LayoutParams(-1,-1));
        root.addView(viewport,PocketUi.lp(this,-1,Math.max(PocketUi.dp(this,140),height),0,0,0,10));
        LinearLayout shoulders=PocketUi.horizontal(this);
        addKey(shoulders,"L",MgbaSession.KEY_L,58);
        shoulders.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
        addKey(shoulders,"R",MgbaSession.KEY_R,58);
        root.addView(shoulders,PocketUi.lp(this,-1,58,10,0,10,3));
        LinearLayout controls=PocketUi.horizontal(this);controls.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout dpad=PocketUi.vertical(this);dpad.setGravity(Gravity.CENTER);
        LinearLayout up=PocketUi.horizontal(this);up.setGravity(Gravity.CENTER);
        addKey(up,"▲",MgbaSession.KEY_UP,60);dpad.addView(up);
        LinearLayout middle=PocketUi.horizontal(this);middle.setGravity(Gravity.CENTER);
        addKey(middle,"◀",MgbaSession.KEY_LEFT,60);addKey(middle,"▼",MgbaSession.KEY_DOWN,60);
        addKey(middle,"▶",MgbaSession.KEY_RIGHT,60);dpad.addView(middle);
        controls.addView(dpad,new LinearLayout.LayoutParams(0,-2,3));
        controls.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
        LinearLayout actions=PocketUi.horizontal(this);actions.setGravity(Gravity.CENTER);
        addKey(actions,"B",MgbaSession.KEY_B,68);addKey(actions,"A",MgbaSession.KEY_A,68);
        controls.addView(actions,new LinearLayout.LayoutParams(0,-2,3));
        root.addView(controls,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout meta=PocketUi.horizontal(this);meta.setGravity(Gravity.CENTER);
        addKey(meta,"SELECT",MgbaSession.KEY_SELECT,100);
        addKey(meta,"START",MgbaSession.KEY_START,100);
        root.addView(meta,PocketUi.lp(this,-1,55,5,2,5,8));
        TextView hint=PocketUi.text(this,"mGBA integrado • saves automáticos • menu ☰",11,PocketUi.MUTED,false);
        hint.setGravity(Gravity.CENTER);root.addView(hint,PocketUi.lp(this,-1,26,0,0,0,0));
        setContentView(root);root.requestApplyInsets();
    }

    private void addKey(LinearLayout row,String label,int mask,int size) {
        TextView button=PocketUi.action(this,label,false);
        button.setTextSize(label.length()>3?11:20);
        button.setContentDescription("Botão "+label);
        LinearLayout.LayoutParams p=PocketUi.lp(this,size,size,3,3,3,3);
        row.addView(button,p);
        button.setOnTouchListener((view,event)->{
            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN -> {setKey(mask,true);view.setPressed(true);return true;}
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {setKey(mask,false);view.setPressed(false);return true;}
                default -> {return true;}
            }
        });
    }
    private void setKey(int key,boolean pressed){
        keys.getAndUpdate(previous->pressed?previous|key:previous&~key);
    }
    private int hardwareKey(int code) {
        return switch(code) {
            case KeyEvent.KEYCODE_DPAD_UP -> MgbaSession.KEY_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN -> MgbaSession.KEY_DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT -> MgbaSession.KEY_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT -> MgbaSession.KEY_RIGHT;
            case KeyEvent.KEYCODE_BUTTON_A -> MgbaSession.KEY_A;
            case KeyEvent.KEYCODE_BUTTON_B -> MgbaSession.KEY_B;
            case KeyEvent.KEYCODE_BUTTON_L1 -> MgbaSession.KEY_L;
            case KeyEvent.KEYCODE_BUTTON_R1 -> MgbaSession.KEY_R;
            case KeyEvent.KEYCODE_BUTTON_START -> MgbaSession.KEY_START;
            case KeyEvent.KEYCODE_BUTTON_SELECT -> MgbaSession.KEY_SELECT;
            default -> 0;
        };
    }
    @Override public boolean onKeyDown(int code,KeyEvent event){
        int key=hardwareKey(code);if(key!=0){setKey(key,true);return true;}
        return super.onKeyDown(code,event);
    }
    @Override public boolean onKeyUp(int code,KeyEvent event){
        int key=hardwareKey(code);if(key!=0){setKey(key,false);return true;}
        return super.onKeyUp(code,event);
    }
    @Override public void onBackPressed(){showMenu();}

    private void showMenu() {
        String[] choices={"Continuar","Salvar estado rápido","Carregar estado rápido","Sair (salvar progresso)"};
        new AlertDialog.Builder(this).setTitle(game==null?"Pocket":game.title)
            .setItems(choices,(dialog,which)->{
                switch(which){
                    case 1 -> {if(loaded)command.set(1);else toast("Espere o jogo carregar.");}
                    case 2 -> {if(loaded)command.set(2);else toast("Espere o jogo carregar.");}
                    case 3 -> finish();
                }
            }).setNegativeButton("Fechar",null).show();
    }
    private void toast(String message){runOnUiThread(()->android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_SHORT).show());}
    private void fail(String message) {
        runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Não foi possível jogar")
            .setMessage(message+"\n\nA ROM não foi removida da sua biblioteca.")
            .setPositiveButton("Voltar",(d,w)->finish()).setCancelable(false).show());
    }
    private static String digest(String input) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            return out.toString();
        } catch(Exception ignored){throw new IllegalStateException("SHA-256 unavailable");}
    }
    private static void writeFile(File file,byte[] data) throws java.io.IOException {
        File part=new File(file.getAbsolutePath()+".part");
        try(FileOutputStream output=new FileOutputStream(part)) {output.write(data);output.getFD().sync();}
        if(!part.renameTo(file)) {part.delete();throw new java.io.IOException("Unable to commit save file");}
    }
    private static byte[] readFile(File file) throws java.io.IOException {
        try(FileInputStream in=new FileInputStream(file)) {return in.readAllBytes();}
    }
    private void stageRom() throws java.io.IOException {
        // SAF can expose non-seekable documents; make a bounded private copy before native loading.
        try(InputStream input=getContentResolver().openInputStream(Uri.parse(game.uri))) {
            if(input==null)throw new java.io.IOException("O Android não concedeu leitura do arquivo. Adicione a pasta novamente.");
            File part=new File(temporaryRom.getAbsolutePath()+".part");
            long copied=0;
            try(FileOutputStream output=new FileOutputStream(part)) {
                byte[] buffer=new byte[32*1024];int amount;
                while((amount=input.read(buffer))!=-1) {
                    copied+=amount;
                    if(copied>32L*1024*1024)throw new java.io.IOException("ROM maior que 32 MiB; arquivo não suportado.");
                    output.write(buffer,0,amount);
                }
                output.getFD().sync();
            }catch(Exception e){part.delete();throw e;}
            if(copied==0){part.delete();throw new java.io.IOException("A ROM está vazia.");}
            if(!part.renameTo(temporaryRom)){part.delete();throw new java.io.IOException("Não consegui preparar a ROM.");}
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if(game==null||temporaryRom==null||worker!=null)return;
        running=true;
        worker=new Thread(this::runGame,"Pocket-mGBA-"+game.system);
        worker.start();
    }
    @Override protected void onPause() {
        running=false;
        keys.set(0);
        Thread current=worker;
        if(current!=null) {
            current.interrupt();
            try {current.join(3000);}catch(InterruptedException e){Thread.currentThread().interrupt();}
            if(current.isAlive())Log.w(TAG,"mGBA worker still stopping");
            else worker=null;
        }
        super.onPause();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        if(worker==null && temporaryRom!=null)temporaryRom.delete();
    }
    private AudioTrack audioTrack() {
        int minimum=AudioTrack.getMinBufferSize(MgbaSession.AUDIO_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);
        return new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(new AudioFormat.Builder().setSampleRate(MgbaSession.AUDIO_SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setBufferSizeInBytes(Math.max(32768,minimum*2)).setTransferMode(AudioTrack.MODE_STREAM).build();
    }
    private void runGame() {
        AudioTrack track=null;
        loaded=false;
        try {
            stageRom();
            if(!running)return;
            int platform=game.system.equals("GBA")?MgbaSession.PLATFORM_GBA:MgbaSession.PLATFORM_GB;
            try(MgbaSession session=new MgbaSession(platform)) {
                session.loadRom(temporaryRom);
                int width=session.videoWidth(),height=session.videoHeight();
                if(width<=0||height<=0||width*height>500_000)throw new IllegalStateException("Invalid mGBA video size");
                if(batterySave.exists()) {
                    try {session.restoreSavedata(readFile(batterySave));}
                    catch(Exception e){Log.w(TAG,"Cartridge save was not restored",e);}
                }
                if(autoSave.exists()) {
                    try {session.loadState(readFile(autoSave));}
                    catch(Exception e){Log.w(TAG,"Auto state was not restored",e);}
                }
                if(!running)return;
                track=audioTrack();track.play();
                int[] pixels=new int[width*height];
                short[] audio=new short[MgbaSession.MIN_AUDIO_BUFFER_SAMPLES];
                Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
                Paint paint=new Paint();paint.setFilterBitmap(false);
                loaded=true;
                new LibraryStore(this).played(findLiveGame());
                runOnUiThread(()->status.setText("● Jogando • "+game.system));
                long nextFrame=SystemClock.elapsedRealtimeNanos();
                try {
                    while(running) {
                        int operation=command.getAndSet(0);
                        if(operation==1){
                            try {writeFile(quickSave,session.saveState());toast("Estado salvo.");}
                            catch(Exception e){toast("Falha ao salvar: "+e.getMessage());}
                        }else if(operation==2){
                            try {if(!quickSave.exists())throw new java.io.IOException("Nenhum estado salvo.");
                                session.loadState(readFile(quickSave));toast("Estado carregado.");}
                            catch(Exception e){toast("Falha ao carregar: "+e.getMessage());}
                        }
                        int audioFrames=session.runFrame(keys.get(),pixels,audio);
                        if(audioFrames<0)throw new IllegalStateException("mGBA não conseguiu executar o quadro.");
                        if(track.getPlayState()==AudioTrack.PLAYSTATE_PLAYING && audioFrames>0)
                            track.write(audio,0,Math.min(audio.length,audioFrames*2),AudioTrack.WRITE_NON_BLOCKING);
                        image.setPixels(pixels,0,width,0,0,width,height);
                        SurfaceHolder holder=screen.getHolder();
                        Canvas canvas=null;
                        try {
                            if(holder.getSurface().isValid())canvas=holder.lockCanvas();
                            if(canvas!=null) {
                                canvas.drawColor(Color.BLACK);
                                int sw=canvas.getWidth(),sh=canvas.getHeight();
                                float scale=Math.min((float)sw/width,(float)sh/height);
                                int dw=Math.round(width*scale),dh=Math.round(height*scale);
                                canvas.drawBitmap(image,null,new Rect((sw-dw)/2,(sh-dh)/2,(sw+dw)/2,(sh+dh)/2),paint);
                            }
                        }finally {if(canvas!=null)holder.unlockCanvasAndPost(canvas);}
                        nextFrame+=FRAME_NANOS;
                        long wait=nextFrame-SystemClock.elapsedRealtimeNanos();
                        if(wait>0)LockSupport.parkNanos(wait);
                        else if(wait<-FRAME_NANOS*4)nextFrame=SystemClock.elapsedRealtimeNanos();
                    }
                }finally {
                    // The worker owns the native session and writes saves BEFORE closing it.
                    try {writeFile(autoSave,session.saveState());}
                    catch(Exception e){Log.e(TAG,"Could not persist auto state",e);}
                    try {byte[] ram=session.copySavedata();if(ram.length>0)writeFile(batterySave,ram);}
                    catch(Exception e){Log.e(TAG,"Could not persist cartridge save",e);}
                    loaded=false;
                }
            }
        }catch(Throwable error) {
            Log.e(TAG,"Built-in emulation failed",error);
            if(running)fail(error.getMessage()==null?error.getClass().getSimpleName():error.getMessage());
        }finally {
            loaded=false;
            if(track!=null){try {track.pause();track.flush();track.release();}catch(Exception ignored){}}
            if(Thread.currentThread()==worker)worker=null;
        }
    }
    private LibraryStore.Game findLiveGame() {
        // LibraryStore stores the full library synchronously; this lookup cannot mark another game.
        LibraryStore store=new LibraryStore(this);
        for(LibraryStore.Game entry:store.snapshot())if(entry.id.equals(game.id))return entry;
        throw new IllegalStateException("O jogo foi removido da biblioteca.");
    }
}

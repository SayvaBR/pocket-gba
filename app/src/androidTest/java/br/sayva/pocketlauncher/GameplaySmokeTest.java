package br.sayva.pocketlauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.trebuchetdynamics.emulator.mgba.MgbaSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import org.junit.Test;
import org.junit.runner.RunWith;

/** End-to-end engine tests. Source-verified MIT mGBA test ROM lives ONLY in androidTest assets. */
@RunWith(AndroidJUnit4.class)
public final class GameplaySmokeTest {
    private File rom(Context context) throws Exception {
        File destination=new File(context.getCacheDir(),"mgba-suite-smoke.gba");
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open("core-smoke.gba");FileOutputStream out=new FileOutputStream(destination)) {
            byte[] buffer=new byte[16384];int n;
            while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
        }
        assertTrue("MIT GBA test fixture not staged",destination.length()>32768);
        return destination;
    }

    @Test public void realGbaRomBootsAndRendersFrames() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File fixture=rom(context);
        try(MgbaSession core=new MgbaSession(MgbaSession.PLATFORM_GBA)) {
            core.loadRom(fixture); // Real ROM, not merely a JNI library load.
            int width=core.videoWidth(),height=core.videoHeight();
            assertEquals(240,width);assertEquals(160,height);
            int[] pixels=new int[width*height];
            short[] sound=new short[MgbaSession.MIN_AUDIO_BUFFER_SAMPLES];
            for(int frame=0;frame<240;frame++) {
                int audioFrames=core.runFrame(0,pixels,sound);
                assertTrue("mGBA returned invalid PCM frame count",audioFrames>=0);
            }
            assertTrue("The ROM did not advance 240 frames",core.frameCounter()>=240);
            HashSet<Integer> colors=new HashSet<>();
            for(int pixel:pixels) {
                colors.add(pixel);
                if(colors.size()>2)break;
            }
            assertTrue("ROM rendered a blank/uniform frame",colors.size()>1);
            byte[] state=core.saveState();
            assertNotNull(state);
            assertTrue("Save state empty",state.length>100);
            core.loadState(state);
            assertTrue("Frame after state restore failed",core.runFrame(0,pixels,sound)>=0);
        }finally{fixture.delete();}
    }

    @Test public void pocketPlayButtonPathLaunchesRomAndPersistsProgress() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File fixture=rom(context);
        LibraryStore store=new LibraryStore(context);
        String id="pocket-android-smoke-"+SystemClock.elapsedRealtimeNanos();
        LibraryStore.Game entry=new LibraryStore.Game(id,Uri.fromFile(fixture).toString(),
            "mgba-suite-smoke.gba","mGBA Test Suite","GBA");
        store.games.add(entry);
        Method save=LibraryStore.class.getDeclaredMethod("save");save.setAccessible(true);
        save.invoke(store);
        Activity playing=null;
        try {
            Intent intent=new Intent(context,EmulatorActivity.class)
                .putExtra(EmulatorActivity.GAME_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            playing=InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
            assertNotNull("Gameplay Activity was not started",playing);
            assertTrue(playing instanceof EmulatorActivity);
            boolean started=false;
            for(int attempt=0;attempt<50&&!started;attempt++) {
                SystemClock.sleep(200);
                for(LibraryStore.Game game:new LibraryStore(context).snapshot())
                    if(game.id.equals(id)&&game.lastPlayed>0){started=true;break;}
            }
            assertTrue("Pocket Gameplay did not accept real ROM within 10 s",started);
            final Activity close=playing;
            InstrumentationRegistry.getInstrumentation().runOnMainSync(close::finish);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            SystemClock.sleep(1200);
            File directory=new File(context.getFilesDir(),"game-saves");
            File[] saves=directory.listFiles((folder,name)->name.endsWith(".auto"));
            assertTrue("Automatic game state not persisted after exiting",saves!=null&&saves.length>0);
        }finally {
            if(playing!=null&&!playing.isFinishing()){
                final Activity close=playing;
                InstrumentationRegistry.getInstrumentation().runOnMainSync(close::finish);
            }
            LibraryStore cleanup=new LibraryStore(context);
            cleanup.games.removeIf(game->game.id.equals(id));
            save.invoke(cleanup);
            fixture.delete();
        }
    }
}

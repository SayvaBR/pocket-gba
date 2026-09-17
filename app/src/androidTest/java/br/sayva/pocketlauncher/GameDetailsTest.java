package br.sayva.pocketlauncher;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;

/** A real Android test for the portrait game hub, not a Java-only mock. */
@RunWith(AndroidJUnit4.class)
public final class GameDetailsTest {
    private static TextView find(View node,String text) {
        if(node instanceof TextView label && label.getText().toString().contains(text))return label;
        if(node instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){
            TextView found=find(group.getChildAt(i),text);
            if(found!=null)return found;
        }
        return null;
    }
    @Test public void gameHubDisplaysRealMetadataAndPersistsFavorite() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String id="detail-smoke-"+SystemClock.elapsedRealtimeNanos();
        LibraryStore store=new LibraryStore(context);
        LibraryStore.Game entry=new LibraryStore.Game(id,Uri.parse("content://android.test/rom").toString(),
            "pocket-qa.gba","Pocket QA","GBA");
        store.games.add(entry);
        Method save=LibraryStore.class.getDeclaredMethod("save");save.setAccessible(true);
        save.invoke(store);
        Activity activity=null;
        try {
            Intent intent=new Intent(context,GameDetailsActivity.class)
                .putExtra(GameDetailsActivity.GAME_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity=InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
            assertTrue(activity instanceof GameDetailsActivity);
            View root=activity.getWindow().getDecorView();
            assertNotNull("Real game title missing",find(root,"Pocket QA"));
            assertNotNull("Real platform missing",find(root,"GAME BOY ADVANCE"));
            assertNotNull("Game must offer a real play action",find(root,"JOGAR AGORA"));
            TextView favorite=find(root,"Favoritar");
            assertNotNull("Favorite action missing",favorite);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(favorite::performClick);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            boolean persisted=false;
            for(LibraryStore.Game live:new LibraryStore(context).snapshot())if(live.id.equals(id)){
                persisted=live.favorite;break;
            }
            assertTrue("Favoriting game was not written to disk",persisted);
            assertNotNull("UI did not update after favoriting",find(root,"Remover favorito"));
        }finally {
            if(activity!=null){Activity close=activity;
                InstrumentationRegistry.getInstrumentation().runOnMainSync(close::finish);
            }
            LibraryStore cleanup=new LibraryStore(context);
            cleanup.games.removeIf(g->g.id.equals(id));
            save.invoke(cleanup);
        }
    }
}

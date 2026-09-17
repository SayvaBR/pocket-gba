package br.sayva.pocketlauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Prevent false covers and regression where scanning a new folder erases previous consoles. */
@RunWith(AndroidJUnit4.class)
public final class LibraryAndArtworkTest {
    @Test public void coverMatchesKnownRegionalAndArticleVariants() {
        assertEquals("Final Fantasy Tactics Advance (USA)",CoverLoader.bestMatch(
            Arrays.asList("Final Fantasy Tactics Advance (USA)","Final Fantasy I & II - Dawn of Souls (USA)"),
            "Final Fantasy Tactics Advance (Europe)","Final Fantasy Tactics Advance"));
        assertEquals("Legend of Zelda, The - The Minish Cap (USA)",CoverLoader.bestMatch(
            Arrays.asList("Legend of Zelda, The - The Minish Cap (USA)"),
            "The Legend of Zelda - The Minish Cap (USA)","The Minish Cap"));
    }

    @Test public void coverNeverPicksUnrelatedGameArt() {
        assertNull(CoverLoader.bestMatch(
            Arrays.asList("Pokemon Mystery Dungeon - Red Rescue Team (USA)","Mario Kart - Super Circuit (USA)"),
            "Pokemon Emerald Version (USA)","Pokemon Emerald Version"));
    }

    @Test public void inaccessibleFolderDoesNotEraseOtherSystemsOrSources() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs=context.getSharedPreferences("pocket_library_v1",Context.MODE_PRIVATE);
        String previous=prefs.getString("library",null);
        try {
            LibraryStore store=new LibraryStore(context);
            store.roots.clear();store.games.clear();
            store.roots.add(new LibraryStore.Root("content://missing.example/tree/SNES","SNES"));
            store.roots.add(new LibraryStore.Root("content://missing.example/tree/NDS","NDS"));
            store.games.add(new LibraryStore.Game("snes", "content://missing.example/snes", "sample.sfc","Sample SNES","SNES"));
            store.games.add(new LibraryStore.Game("nds", "content://missing.example/nds", "sample.nds","Sample DS","NDS"));
            Method save=LibraryStore.class.getDeclaredMethod("save");save.setAccessible(true);
            save.invoke(store);
            LibraryStore fresh=new LibraryStore(context);
            assertEquals(2,fresh.roots.size());assertEquals(2,fresh.games.size());
            LibraryStore.ScanReport scan=fresh.scan();
            assertTrue("Missing permissions should produce visible scan errors",scan.errors>=2);
            LibraryStore restored=new LibraryStore(context);
            assertEquals("Both folder grants must remain registered",2,restored.roots.size());
            assertEquals("Games are never deleted after unreadable scans",2,restored.games.size());
            assertTrue(restored.games.stream().anyMatch(game->game.system.equals("SNES")));
            assertTrue(restored.games.stream().anyMatch(game->game.system.equals("NDS")));
        }finally {
            SharedPreferences.Editor restore=prefs.edit();
            if(previous==null)restore.remove("library");else restore.putString("library",previous);
            assertTrue("Could not restore library after instrumentation",restore.commit());
        }
    }
}

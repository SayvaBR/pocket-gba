package br.sayva.pocketlauncher;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

/** A player is an external Android app and its documented launch contract, never a bundled core. */
final class EmulatorRouter {
    private static final String PREFS="pocket_emulator_profiles_v1";
    private static final String[] SYSTEMS={"GBA","GB","GBC","SNES","NDS"};
    private final MainActivity activity;
    private final LibraryStore.Game game;
    private final Uri rom;
    private final SharedPreferences preferences;
    private final List<Player> installed=new ArrayList<>();

    private static final class Player {
        final String title, pkg, activityName, extra;
        final boolean zipMime, melonAction;
        Player(String title,String pkg,String activityName,String extra,boolean zipMime,boolean melonAction) {
            this.title=title;this.pkg=pkg;this.activityName=activityName;
            this.extra=extra;this.zipMime=zipMime;this.melonAction=melonAction;
        }
    }

    EmulatorRouter(MainActivity activity,LibraryStore.Game game) {
        this.activity=activity;this.game=game;this.rom=Uri.parse(game.uri);
        this.preferences=activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        switch(game.system) {
            case "NDS" -> {
                add("melonDS", "me.magnum.melonds", "me.magnum.melonds.ui.emulator.EmulatorActivity","uri",false,true);
                add("melonDS Nightly", "me.magnum.melonds.nightly", "me.magnum.melonds.ui.emulator.EmulatorActivity","uri",false,true);
                add("DraStic", "com.dsemu.drastic", "com.dsemu.drastic.DraSticActivity",null,false,false);
                add("SkyEmu", "com.sky.SkyEmu", "com.sky.SkyEmu.EnhancedNativeActivity",null,false,false);
            }
            case "SNES" -> add("Snes9x EX+", "com.explusalpha.Snes9xPlus","com.imagine.BaseActivity",null,true,false);
            case "GBA" -> {
                add("Pizza Boy GBA Basic", "it.dbtecno.pizzaboygba", "it.dbtecno.pizzaboygba.MainActivity","rom_uri",false,false);
                add("Pizza Boy GBA Pro", "it.dbtecno.pizzaboygbapro", "it.dbtecno.pizzaboygbapro.MainActivity","rom_uri",false,false);
                add("GBA.emu", "com.explusalpha.GbaEmu", "com.imagine.BaseActivity",null,true,false);
                add("SkyEmu", "com.sky.SkyEmu", "com.sky.SkyEmu.EnhancedNativeActivity",null,false,false);
            }
            case "GB", "GBC" -> {
                add("Pizza Boy GB Basic", "it.dbtecno.pizzaboy", "it.dbtecno.pizzaboy.MainActivity","rom_uri",false,false);
                add("Pizza Boy GB Pro", "it.dbtecno.pizzaboypro", "it.dbtecno.pizzaboypro.MainActivity","rom_uri",false,false);
                add("GBC.emu", "com.explusalpha.GbcEmu", "com.imagine.BaseActivity",null,true,false);
                add("SkyEmu", "com.sky.SkyEmu", "com.sky.SkyEmu.EnhancedNativeActivity",null,false,false);
            }
            default -> { }
        }
    }

    private void add(String title,String pkg,String cls,String extra,boolean mime,boolean melon) {
        try {
            activity.getPackageManager().getPackageInfo(pkg,0);
            installed.add(new Player(title,pkg,cls,extra,mime,melon));
        } catch(android.content.pm.PackageManager.NameNotFoundException ignored) { }
    }
    private String key(){return "default_player_"+game.system;}

    /** Use the saved player only while it remains installed; otherwise ask the user again. */
    void choose() {
        String preferred=preferences.getString(key(),null);
        if(preferred!=null) {
            for(Player player:installed) if(player.pkg.equals(preferred)) {open(player,false);return;}
            preferences.edit().remove(key()).apply();
            activity.toast("Seu emulador padrão não está instalado. Escolha outro.");
        }
        chooseManual();
    }

    /** Long-press a game to change its platform player without clearing the whole library. */
    void chooseManual() {
        ArrayList<String> labels=new ArrayList<>();
        for(Player player:installed)labels.add(player.title);
        labels.add("Outro aplicativo compatível...");
        String preferred=preferences.getString(key(),null);
        String subtitle=installed.isEmpty()
            ? "Nenhum emulador conhecido instalado. O seletor genérico pode não iniciar ROMs em todos os aplicativos."
            : "Emuladores detectados para "+game.system+". Selecione um para jogar.";
        if(preferred!=null)subtitle+="\nO padrão salvo pode ser substituído aqui.";
        new AlertDialog.Builder(activity).setTitle("Emulador • "+game.system)
            .setMessage(subtitle).setItems(labels.toArray(new String[0]),(dialog,index)->{
                if(index==installed.size()) {openGeneric();return;}
                Player selected=installed.get(index);
                new AlertDialog.Builder(activity).setTitle(selected.title)
                    .setMessage("Deseja usar este emulador automaticamente para os próximos jogos de "+game.system+"?")
                    .setPositiveButton("Sempre",(d,w)->open(selected,true))
                    .setNeutralButton("Só desta vez",(d,w)->open(selected,false))
                    .setNegativeButton("Voltar",(d,w)->chooseManual()).show();
            }).setNegativeButton("Cancelar",null).show();
    }

    private void prepare(Intent intent) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setClipData(ClipData.newUri(activity.getContentResolver(),game.fileName,rom));
    }
    private void open(Player player,boolean makeDefault) {
        Intent intent=new Intent(Intent.ACTION_VIEW);
        intent.setComponent(new ComponentName(player.pkg,player.activityName));
        if(player.melonAction)intent.setAction(player.pkg+".LAUNCH_ROM");
        if(player.zipMime)intent.setDataAndType(rom,"application/zip");
        else intent.setData(rom);
        if(player.extra!=null)intent.putExtra(player.extra,game.uri);
        prepare(intent);
        try {
            activity.startActivity(intent);
            if(makeDefault)preferences.edit().putString(key(),player.pkg).apply();
            // Android confirms only activity dispatch here, not that the target loaded the ROM.
            activity.library.played(game);
            activity.refreshAfterLaunch();
        } catch(ActivityNotFoundException|SecurityException exception) {
            if(preferences.getString(key(),"").equals(player.pkg))preferences.edit().remove(key()).apply();
            new AlertDialog.Builder(activity).setTitle("Emulador não iniciou")
                .setMessage(player.title+" não aceitou o comando. A instalação, a atividade exportada e o acesso à ROM dependem da versão do emulador.")
                .setPositiveButton("Escolher outro",(d,w)->chooseManual())
                .setNegativeButton("Cancelar",null).show();
        }
    }
    private void openGeneric() {
        Intent intent=new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(rom,"application/octet-stream");prepare(intent);
        try {activity.startActivity(Intent.createChooser(intent,"Escolher emulador"));}
        catch(ActivityNotFoundException|SecurityException exception) {
            activity.toast("Nenhum aplicativo compatível disponível. Instale e configure um emulador.");
        }
    }

    /** Change and clear saved defaults without touching imported games or folder permissions. */
    static void showProfiles(MainActivity activity) {
        SharedPreferences prefs=activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String[] rows=new String[SYSTEMS.length];
        for(int i=0;i<SYSTEMS.length;i++) {
            String selected=prefs.getString("default_player_"+SYSTEMS[i],null);
            rows[i]=SYSTEMS[i]+"   •   "+(selected==null?"Perguntar ao jogar":selected);
        }
        new AlertDialog.Builder(activity).setTitle("Emuladores por console")
            .setMessage("Escolha um console para limpar o padrão. Para definir outro, abra um jogo e selecione Sempre. Nenhum jogo será removido.")
            .setItems(rows,(d,index)->{
                String id=SYSTEMS[index];
                String saved=prefs.getString("default_player_"+id,null);
                if(saved==null){activity.toast("Nenhum emulador padrão para "+id+".");return;}
                new AlertDialog.Builder(activity).setTitle(id+" • "+saved)
                    .setMessage("Remover a preferência? Na próxima vez o Pocket mostrará a escolha de emuladores.")
                    .setPositiveButton("Limpar padrão",(dialog,which)->{
                        prefs.edit().remove("default_player_"+id).apply();
                        activity.toast("Preferência de "+id+" limpa.");
                    }).setNegativeButton("Cancelar",null).show();
            }).setPositiveButton("Fechar",null).show();
    }
}

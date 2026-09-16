package br.sayva.pocketlauncher;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

/** Explicit external emulator adapters. No emulator engines or ROMs are bundled. */
final class EmulatorRouter {
    private final MainActivity activity;
    private final LibraryStore.Game game;
    private final Uri rom;
    private final List<Adapter> candidates=new ArrayList<>();

    private static final class Adapter {
        final String label,packageName,activityName,extra;
        final boolean mime;
        Adapter(String label,String pkg,String activity,String extra,boolean mime) {
            this.label=label;packageName=pkg;activityName=activity;this.extra=extra;this.mime=mime;
        }
    }
    EmulatorRouter(MainActivity activity,LibraryStore.Game game) {
        this.activity=activity;this.game=game;rom=Uri.parse(game.uri);
        if(game.system.equals("NDS")) {
            add("melonDS", "me.magnum.melonds", "me.magnum.melonds.ui.emulator.EmulatorActivity", null,false);
            add("melonDS Nightly", "me.magnum.melonds.nightly", "me.magnum.melonds.ui.emulator.EmulatorActivity",null,false);
            add("DraStic", "com.dsemu.drastic", "com.dsemu.drastic.DraSticActivity",null,false);
        } else if(game.system.equals("SNES")) {
            add("Snes9x EX+", "com.explusalpha.Snes9xPlus","com.imagine.BaseActivity",null,true);
        } else {
            if(game.system.equals("GBA")) {
                add("Pizza Boy GBA Basic", "it.dbtecno.pizzaboygba", "it.dbtecno.pizzaboygba.MainActivity","rom_uri",false);
                add("Pizza Boy GBA Pro", "it.dbtecno.pizzaboygbapro", "it.dbtecno.pizzaboygbapro.MainActivity","rom_uri",false);
                add("GBA.emu", "com.explusalpha.GbaEmu", "com.imagine.BaseActivity",null,true);
            } else {
                add("Pizza Boy GB Basic", "it.dbtecno.pizzaboy","it.dbtecno.pizzaboy.MainActivity","rom_uri",false);
                add("Pizza Boy GB Pro", "it.dbtecno.pizzaboypro","it.dbtecno.pizzaboypro.MainActivity","rom_uri",false);
                add("GBC.emu", "com.explusalpha.GbcEmu","com.imagine.BaseActivity",null,true);
            }
            add("SkyEmu", "com.sky.SkyEmu","com.sky.SkyEmu.EnhancedNativeActivity",null,false);
        }
    }
    private void add(String name,String pkg,String cls,String extra,boolean mime) {
        try {
            activity.getPackageManager().getPackageInfo(pkg,0);
            candidates.add(new Adapter(name,pkg,cls,extra,mime));
        } catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
    }
    void choose() {
        ArrayList<String> names=new ArrayList<>();
        for(Adapter adapter:candidates)names.add(adapter.label);
        names.add("Outro aplicativo compatível...");
        String[] labels=names.toArray(new String[0]);
        new AlertDialog.Builder(activity).setTitle("Abrir "+game.title)
            .setMessage(candidates.isEmpty()?"Nenhum emulador conhecido encontrado. Você pode tentar outro aplicativo que aceite arquivos de ROM.":"Selecione um emulador instalado. A ROM permanece no seu dispositivo.")
            .setItems(labels,(dialog,index)->{
                if(index==candidates.size())openGeneric();else open(candidates.get(index));
            }).setNegativeButton("Cancelar",null).show();
    }
    private void prepare(Intent intent) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setClipData(ClipData.newUri(activity.getContentResolver(),game.fileName,rom));
    }
    private void open(Adapter adapter) {
        Intent intent=new Intent(Intent.ACTION_VIEW);
        intent.setComponent(new ComponentName(adapter.packageName,adapter.activityName));
        if(game.system.equals("NDS") && adapter.packageName.startsWith("me.magnum.melonds"))
            intent.setAction(adapter.packageName+".LAUNCH_ROM");
        if(adapter.mime)intent.setDataAndType(rom,"application/zip");
        else intent.setData(rom);
        if(adapter.extra!=null)intent.putExtra(adapter.extra,game.uri);
        prepare(intent);
        try {
            activity.startActivity(intent);
            activity.library.played(game);activity.refreshAfterLaunch();
        } catch(ActivityNotFoundException | SecurityException error) {
            new AlertDialog.Builder(activity).setTitle("Não foi possível abrir")
                .setMessage(adapter.label+" não aceitou a inicialização. Confira se o emulador está atualizado, se importou a pasta e se concede acesso à ROM.")
                .setPositiveButton("Tentar outro",(d,w)->choose()).setNegativeButton("Cancelar",null).show();
        }
    }
    private void openGeneric() {
        Intent view=new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(rom,"application/octet-stream");prepare(view);
        try {
            if(view.resolveActivity(activity.getPackageManager())==null) {
                activity.toast("Nenhum app registrado para abrir esta ROM. Instale um emulador compatível.");return;
            }
            activity.startActivity(Intent.createChooser(view,"Emulador"));
        } catch(Exception exception) {activity.toast("Nenhum emulador compatível disponível.");}
    }
}

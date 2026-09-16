package br.sayva.pocketlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends Activity {
    private static final int PICK_TREE=401;
    LibraryStore library;
    CoverLoader covers;
    int accent=PocketUi.ACCENT;
    String page="Início",filter="Todos";
    private final ArrayDeque<String> previous=new ArrayDeque<>();
    private final AtomicBoolean scanning=new AtomicBoolean(false);
    private FrameLayout root;
    private LinearLayout column;
    private FrameLayout container;
    private SharedPreferences navigation;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(0x100|0x200|0x400);
        library=new LibraryStore(this);covers=new CoverLoader(this);
        navigation=getSharedPreferences("pocket_navigation_v1",MODE_PRIVATE);
        String restored=navigation.getString("page","Início");
        if(restored.equals("Início")||restored.equals("Biblioteca")||restored.equals("Buscar")||restored.equals("Ajustes"))page=restored;
        accent=navigation.getInt("accent",PocketUi.ACCENT);
        root=new FrameLayout(this);root.setBackground(PocketUi.dots(this));
        root.setOnApplyWindowInsetsListener((view,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30){
                android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                view.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            }else{
                view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        column=PocketUi.vertical(this);
        root.addView(column,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);root.requestApplyInsets();render();
        if(!library.roots.isEmpty()&&library.games.isEmpty())scan();
    }
    void navigate(String next){
        if(next.equals(page))return;
        previous.push(page);page=next;
        navigation.edit().putString("page",next).apply();render();
    }
    void setFilter(String system){filter=system;navigate("Biblioteca");if(page.equals("Biblioteca"))render();}
    @Override public void onBackPressed(){
        if(!previous.isEmpty()){
            page=previous.pop();navigation.edit().putString("page",page).apply();render();
        }else if(!page.equals("Início")){
            page="Início";navigation.edit().putString("page",page).apply();render();
        }else super.onBackPressed();
    }
    private void render(){
        column.removeAllViews();
        LinearLayout header=PocketUi.horizontal(this);
        header.setPadding(PocketUi.dp(this,22),PocketUi.dp(this,17),PocketUi.dp(this,18),PocketUi.dp(this,14));
        LinearLayout brand=PocketUi.vertical(this);
        TextView brandName=PocketUi.text(this,"P O C K E T",24,PocketUi.TEXT,true);
        TextView hint=PocketUi.text(this,"SUA BIBLIOTECA DE JOGOS",10,accent,true);hint.setLetterSpacing(.14f);
        brand.addView(brandName);brand.addView(hint);
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1f));
        TextView emulators=PocketUi.action(this,"▶",false);emulators.setTextSize(17);
        emulators.setContentDescription("Emuladores padrão por console");
        emulators.setOnClickListener(v->EmulatorRouter.showProfiles(this));
        header.addView(emulators,PocketUi.lp(this,46,45,0,0,7,0));
        TextView add=PocketUi.action(this,"＋",false);add.setTextSize(22);
        add.setContentDescription("Adicionar pasta de ROMs");add.setOnClickListener(v->chooseFolder());
        header.addView(add,PocketUi.lp(this,46,45,0,0,0,0));
        column.addView(header,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout navigationBar=PocketUi.horizontal(this);
        navigationBar.setPadding(PocketUi.dp(this,13),0,PocketUi.dp(this,13),PocketUi.dp(this,10));
        for(String target:new String[]{"Início","Biblioteca","Buscar","Ajustes"}){
            TextView tab=PocketUi.action(this,target.equals("Início")?"⌂ Início":target.equals("Biblioteca")?"▦ Jogos":target.equals("Buscar")?"⌕ Buscar":"⚙ Ajustes",page.equals(target));
            tab.setTextSize(11);tab.setPadding(2,PocketUi.dp(this,13),2,PocketUi.dp(this,13));
            tab.setOnClickListener(v->navigate(target));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,PocketUi.dp(this,44),1f);
            p.setMargins(PocketUi.dp(this,3),0,PocketUi.dp(this,3),0);navigationBar.addView(tab,p);
        }
        column.addView(navigationBar);
        View divider=new View(this);divider.setBackgroundColor(0xff313746);
        column.addView(divider,new LinearLayout.LayoutParams(-1,PocketUi.dp(this,1)));
        container=new FrameLayout(this);column.addView(container,new LinearLayout.LayoutParams(-1,0,1f));
        new Screens(this).show(container,page);
    }
    void refreshAfterLaunch(){render();}
    void chooseFolder(){
        Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try{startActivityForResult(picker,PICK_TREE);}catch(Exception e){toast("O seletor de pastas não está disponível neste aparelho.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=PICK_TREE||result!=RESULT_OK||data==null||data.getData()==null)return;
        try{
            library.addFolder(data.getData(),data.getFlags());
            toast("Pasta adicionada. Indexando jogos...");scan();
        }catch(Exception e){toast("Não consegui manter acesso a esta pasta: "+e.getMessage());}
    }
    void scan(){
        if(!scanning.compareAndSet(false,true))return;
        toast("Procurando jogos em todas as pastas...");
        new Thread(()->{
            LibraryStore.ScanReport report;
            try{report=library.scan();}
            catch(Exception exception){
                android.util.Log.e("PocketLibrary","Unexpected scanner error",exception);
                report=new LibraryStore.ScanReport();report.errors=1;
                report.warnings.add("A leitura foi interrompida. Seus jogos existentes foram preservados.");
            }finally{scanning.set(false);}
            LibraryStore.ScanReport complete=report;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                render();
                String message=complete.added+" novo(s), "+complete.found+" encontrado(s).";
                if(complete.errors>0)message+=" "+complete.errors+" falha(s); biblioteca preservada.";
                new AlertDialog.Builder(this).setTitle("Importação concluída")
                    .setMessage(message+(complete.warnings.isEmpty()?"":"\n\n"+String.join("\n",complete.warnings)))
                    .setPositiveButton("OK",null).show();
            });
        },"Pocket-Rom-Scan").start();
    }
    void showActions(LibraryStore.Game game){
        String[] options={"Jogar","Informações","Renomear","Favoritar / desfavoritar",
            game.hidden?"Restaurar à biblioteca":"Remover da lista","Escolher outro emulador"};
        new AlertDialog.Builder(this).setTitle(game.title).setItems(options,(dialog,which)->{
            switch(which){
                case 0 -> launch(game);
                case 1 -> details(game);
                case 2 -> rename(game);
                case 3 -> {library.edit(game,null,!game.favorite,null);render();}
                case 4 -> {library.edit(game,null,null,!game.hidden);render();}
                case 5 -> new EmulatorRouter(this,game).chooseManual();
            }
        }).show();
    }
    void rename(LibraryStore.Game game){
        EditText editor=new EditText(this);editor.setSingleLine(true);editor.setText(game.title);editor.selectAll();
        new AlertDialog.Builder(this).setTitle("Renomear jogo").setView(editor)
            .setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(dialog,which)->{
                library.edit(game,editor.getText().toString(),null,null);render();
            }).show();
    }
    void details(LibraryStore.Game game){
        String info="Sistema: "+game.system+"\nArquivo: "+game.fileName+"\n\n"
            +(game.lastPlayed==0?"Ainda não enviado ao emulador":"Último envio: "+android.text.format.DateFormat.format("dd/MM/yyyy HH:mm",game.lastPlayed))
            +"\n\nO Pocket não exclui ROMs ou saves ao ocultar jogos.";
        new AlertDialog.Builder(this).setTitle(game.title).setMessage(info)
            .setPositiveButton("Jogar",(d,w)->launch(game))
            .setNeutralButton("Editar",(d,w)->showActions(game))
            .setNegativeButton("Fechar",null).show();
    }
    void launch(LibraryStore.Game game){new EmulatorRouter(this,game).choose();}
    void removeRoot(LibraryStore.Root folder){
        new AlertDialog.Builder(this).setTitle("Desvincular pasta?")
            .setMessage("Isso interrompe novas leituras desta pasta, mas preserva os jogos já indexados, as ROMs e os saves.")
            .setNegativeButton("Cancelar",null).setPositiveButton("Desvincular",(d,w)->{
                library.removeFolder(folder);render();
            }).show();
    }
    void color(int newColor){accent=newColor;navigation.edit().putInt("accent",accent).apply();render();}
    void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
}

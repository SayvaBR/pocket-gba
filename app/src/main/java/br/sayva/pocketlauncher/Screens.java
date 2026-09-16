package br.sayva.pocketlauncher;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Portrait launcher screens, written independently from every emulator frontend. */
final class Screens {
    private final MainActivity host;
    private final Context c;
    private LinearLayout content;
    Screens(MainActivity host){this.host=host;c=host;}
    void show(FrameLayout container,String page) {
        container.removeAllViews();
        ScrollView scroller=new ScrollView(c);scroller.setFillViewport(true);scroller.setClipToPadding(false);
        scroller.setVerticalScrollBarEnabled(false);
        content=PocketUi.vertical(c);content.setPadding(PocketUi.dp(c,18),PocketUi.dp(c,12),PocketUi.dp(c,18),PocketUi.dp(c,35));
        scroller.addView(content,new ScrollView.LayoutParams(-1,-2));
        container.addView(scroller,new FrameLayout.LayoutParams(-1,-1));
        switch(page){
            case "Biblioteca" -> library();
            case "Buscar" -> search();
            case "Ajustes" -> settings();
            default -> home();
        }
    }
    private List<LibraryStore.Game> all() {
        List<LibraryStore.Game> games=host.library.snapshot();
        games.sort(Comparator.comparingLong((LibraryStore.Game game)->game.lastPlayed).reversed()
            .thenComparing(game->game.title.toLowerCase(Locale.ROOT)));
        return games;
    }
    private List<LibraryStore.Game> visible() {
        List<LibraryStore.Game> games=all();games.removeIf(g->g.hidden);return games;
    }
    private void title(String small,String large) {
        TextView eyebrow=PocketUi.text(c,small.toUpperCase(Locale.ROOT),10,host.accent,true);eyebrow.setLetterSpacing(.18f);
        content.addView(eyebrow,PocketUi.lp(c,-1,-2,6,12,0,5));
        content.addView(PocketUi.text(c,large,27,PocketUi.TEXT,true),PocketUi.lp(c,-1,-2,6,0,0,17));
    }
    private TextView label(String text,int size,int color,boolean bold){return PocketUi.text(c,text,size,color,bold);}
    private void heading(String text,String side) {
        LinearLayout row=PocketUi.horizontal(c);
        row.addView(label(text,20,PocketUi.TEXT,true),new LinearLayout.LayoutParams(0,-2,1));
        if(side!=null){TextView go=label(side+"  ›",12,host.accent,true);go.setOnClickListener(v->host.navigate("Biblioteca"));row.addView(go);}
        content.addView(row,PocketUi.lp(c,-1,-2,6,22,6,15));
    }
    private TextView button(String text,Runnable action,boolean active) {
        TextView b=PocketUi.action(c,text,active);b.setOnClickListener(v->action.run());return b;
    }
    private void home() {
        List<LibraryStore.Game> games=visible();
        title("Launcher / Início","Seu universo de jogos");
        LibraryStore.Game recent=null;for(LibraryStore.Game g:games)if(g.lastPlayed>0){recent=g;break;}
        if(recent!=null){
            heading("Continuar jogando",null);
            LibraryStore.Game current=recent;
            LinearLayout hero=PocketUi.horizontal(c);hero.setBackground(PocketUi.gradient(0xff292b46,0xff151c2e,24,c));
            ImageView cover=new ImageView(c);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            cover.setBackground(PocketUi.shape(0xff343c58,16,c));cover.setClipToOutline(true);
            host.covers.into(cover,current);
            hero.addView(cover,PocketUi.lp(c,91,120,13,13,12,13));
            LinearLayout copy=PocketUi.vertical(c);copy.setGravity(Gravity.CENTER_VERTICAL);
            copy.addView(label(current.system+"  •  ÚLTIMO JOGO",10,host.accent,true));
            copy.addView(label(current.title,19,PocketUi.TEXT,true),PocketUi.lp(c,-1,-2,0,7,0,13));
            copy.addView(button("▶  Jogar",()->host.launch(current),true),PocketUi.lp(c,130,45,0,0,0,0));
            hero.addView(copy,new LinearLayout.LayoutParams(0,-1,1));
            content.addView(hero,PocketUi.lp(c,-1,148,2,0,2,0));
        } else if(games.isEmpty()) {
            LinearLayout empty=PocketUi.vertical(c);empty.setPadding(PocketUi.dp(c,20),PocketUi.dp(c,28),PocketUi.dp(c,20),PocketUi.dp(c,26));
            empty.setBackground(PocketUi.gradient(0xff30354b,0xff171d2b,24,c));
            empty.addView(label("▣",34,host.accent,true));
            empty.addView(label("Nenhum jogo importado",22,PocketUi.TEXT,true),PocketUi.lp(c,-1,-2,0,12,0,6));
            empty.addView(label("Adicione uma pasta. O Pocket identificará os consoles e buscará as capas automaticamente.",14,PocketUi.MUTED,false),PocketUi.lp(c,-1,-2,0,0,0,19));
            empty.addView(button("＋  Adicionar pasta",host::chooseFolder,true),PocketUi.lp(c,-1,48,0,0,0,0));
            content.addView(empty,PocketUi.lp(c,-1,-2,2,4,2,0));
        } else {
            LinearLayout hero=PocketUi.vertical(c);hero.setPadding(PocketUi.dp(c,20),PocketUi.dp(c,19),PocketUi.dp(c,20),PocketUi.dp(c,19));
            hero.setBackground(PocketUi.gradient(0xff30354b,0xff171d2b,24,c));
            hero.addView(label(games.size()+" JOGOS ENCONTRADOS",11,host.accent,true));
            hero.addView(label("Sua coleção está pronta",21,PocketUi.TEXT,true),PocketUi.lp(c,-1,-2,0,10,0,12));
            hero.addView(button("Abrir biblioteca  →",()->host.navigate("Biblioteca"),true));
            content.addView(hero,PocketUi.lp(c,-1,-2,2,0,2,0));
        }
        heading("Consoles",null);
        platformGrid(games);
        if(!games.isEmpty()){
            heading("Sua coleção","Ver tudo");
            gameGrid(games.subList(0,Math.min(6,games.size())));
        }
        TextView foot=label("POCKET  •  GAME LIBRARY",10,0xff626e80,true);
        foot.setGravity(Gravity.CENTER);
        content.addView(foot,PocketUi.lp(c,-1,36,0,31,0,8));
    }
    private void platformGrid(List<LibraryStore.Game> games) {
        String[][] platforms={{"GBA","Game Boy Advance","GBA"},{"GB","Game Boy / Color","GB"},
            {"SNES","Super Nintendo","SNES"},{"NDS","Nintendo DS","DS"}};
        for(int row=0;row<2;row++) {
            LinearLayout line=PocketUi.horizontal(c);
            for(int index=0;index<2;index++){
                int n=row*2+index;String[] p=platforms[n];int count=0;
                for(LibraryStore.Game g:games)if(p[0].equals("GB")?(g.system.equals("GB")||g.system.equals("GBC")):p[0].equals(g.system))count++;
                int tint=PocketUi.PLATFORM[n];
                LinearLayout card=PocketUi.vertical(c);
                card.setPadding(PocketUi.dp(c,15),PocketUi.dp(c,14),PocketUi.dp(c,8),PocketUi.dp(c,12));
                card.setBackground(PocketUi.gradient(PocketUi.RAISED,PocketUi.SURFACE,19,c));
                card.addView(label(p[2],29,tint,true));
                PocketUi.space(card,c,13);
                card.addView(label(p[1],15,PocketUi.TEXT,true));
                card.addView(label(count+" jogo(s)  ›",11,PocketUi.MUTED,false),PocketUi.lp(c,-1,-2,0,5,0,0));
                card.setOnClickListener(v->host.setFilter(p[0]));
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,PocketUi.dp(c,145),1);
                cp.setMargins(PocketUi.dp(c,3),PocketUi.dp(c,3),PocketUi.dp(c,7),PocketUi.dp(c,8));line.addView(card,cp);
            }
            content.addView(line,PocketUi.lp(c,-1,-2,0,0,0,0));
        }
    }
    private void library() {
        List<LibraryStore.Game> games=visible();
        title("Todos os sistemas","Biblioteca");
        LinearLayout line=PocketUi.horizontal(c);
        line.addView(label(games.size()+" jogos  •  "+host.library.roots.size()+" pastas",13,PocketUi.MUTED,false),new LinearLayout.LayoutParams(0,-2,1));
        line.addView(button("＋ Importar",host::chooseFolder,false));
        content.addView(line,PocketUi.lp(c,-1,-2,6,0,6,17));
        filters();
        if(!host.filter.equals("Todos"))games.removeIf(g->!host.filter.equals("GB")? !g.system.equals(host.filter):!(g.system.equals("GB")||g.system.equals("GBC")));
        if(games.isEmpty())empty("Nenhum jogo nesta coleção","Importe uma pasta ou escolha outro console.","＋ Adicionar jogos",host::chooseFolder);
        else gameGrid(games);
    }
    private void filters() {
        HorizontalScrollView scroll=new HorizontalScrollView(c);scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row=PocketUi.horizontal(c);
        for(String id:new String[]{"Todos","GBA","GB","SNES","NDS"}){
            TextView chip=button(id,()->{host.filter=id;host.navigate("Biblioteca");showCurrentLibrary();},host.filter.equals(id));
            row.addView(chip,PocketUi.lp(c,-2,40,2,0,7,0));
        }
        scroll.addView(row);
        content.addView(scroll,PocketUi.lp(c,-1,48,0,0,0,15));
    }
    private void showCurrentLibrary(){host.setFilter(host.filter);}
    private void search() {
        title("Encontre seus jogos","Buscar");
        EditText box=new EditText(c);box.setSingleLine(true);box.setTextColor(PocketUi.TEXT);box.setHintTextColor(PocketUi.MUTED);
        box.setHint("⌕  Nome do jogo ou console");box.setTextSize(16);
        box.setPadding(PocketUi.dp(c,17),0,PocketUi.dp(c,17),0);
        box.setBackground(PocketUi.outlined(PocketUi.SURFACE,0xff40485b,16,c));
        content.addView(box,PocketUi.lp(c,-1,55,3,0,3,22));
        LinearLayout results=PocketUi.vertical(c);content.addView(results);
        Runnable refresh=()->{
            results.removeAllViews();
            List<LibraryStore.Game> matches=visible();String query=box.getText().toString().toLowerCase(Locale.ROOT).trim();
            if(!query.isEmpty())matches.removeIf(g->!g.title.toLowerCase(Locale.ROOT).contains(query)&&!g.system.toLowerCase(Locale.ROOT).contains(query));
            if(matches.isEmpty()){
                results.addView(label("Nenhum resultado. Tente outro nome.",14,PocketUi.MUTED,false));
            }else results.addView(grid(matches));
        };
        box.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){refresh.run();}
            public void afterTextChanged(android.text.Editable editable){}
        });refresh.run();
    }
    private void settings() {
        title("Preferências e biblioteca","Ajustes");
        heading("Pastas de jogos",null);
        TextView help=label("Pastas principais e individuais podem coexistir. Escanear nunca elimina automaticamente um jogo já encontrado.",13,PocketUi.MUTED,false);
        content.addView(help,PocketUi.lp(c,-1,-2,6,0,6,15));
        content.addView(button("＋  Adicionar pasta",host::chooseFolder,true),PocketUi.lp(c,-1,52,2,0,2,10));
        content.addView(button("⟳  Escanear todas as pastas",host::scan,false),PocketUi.lp(c,-1,52,2,0,2,12));
        for(LibraryStore.Root root:new ArrayList<>(host.library.roots)){
            LinearLayout folder=PocketUi.horizontal(c);folder.setPadding(PocketUi.dp(c,14),0,PocketUi.dp(c,14),0);
            folder.setBackground(PocketUi.outlined(PocketUi.SURFACE,0xff344052,15,c));
            folder.addView(label("▤  "+root.label,14,PocketUi.TEXT,true),new LinearLayout.LayoutParams(0,-2,1));
            TextView remove=button("✕",()->host.removeRoot(root),false);remove.setContentDescription("Desvincular "+root.label);
            folder.addView(remove);
            content.addView(folder,PocketUi.lp(c,-1,61,2,3,2,7));
        }
        if(host.library.roots.isEmpty())content.addView(label("Nenhuma pasta adicionada.",13,PocketUi.MUTED,false),PocketUi.lp(c,-1,-2,8,8,0,0));
        heading("Aparência",null);
        content.addView(label("Cor de destaque",13,PocketUi.MUTED,false),PocketUi.lp(c,-1,-2,7,0,0,10));
        LinearLayout swatches=PocketUi.horizontal(c);
        for(int tint:new int[]{PocketUi.ACCENT,0xffb69bff,0xff76c7f5,0xff53d8b1,0xffff8baf}){
            TextView circle=button(tint==host.accent?"✓":"●",()->host.color(tint),tint==host.accent);
            circle.setTextColor(tint==host.accent?PocketUi.BG:tint);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,PocketUi.dp(c,48),1);p.setMargins(2,0,PocketUi.dp(c,8),0);swatches.addView(circle,p);
        }
        content.addView(swatches,PocketUi.lp(c,-1,49,2,0,2,9));
        heading("Capas",null);
        content.addView(label("Busca automática via catálogo Libretro. Imagens encontradas ficam em cache no dispositivo. Não é necessário configurar uma API ou selecionar capas.",13,PocketUi.MUTED,false),PocketUi.lp(c,-1,-2,7,0,7,11));
        heading("Jogos ocultos",null);
        int hidden=0;for(LibraryStore.Game game:host.library.snapshot())if(game.hidden){hidden++;
            content.addView(button("Restaurar  •  "+game.title,()->{host.library.edit(game,null,null,false);host.navigate("Biblioteca");},false),PocketUi.lp(c,-1,44,2,0,2,7));
        }
        if(hidden==0)content.addView(label("Nenhum jogo oculto.",13,PocketUi.MUTED,false),PocketUi.lp(c,-1,-2,7,0,7,5));
        heading("Sobre",null);
        content.addView(label("Pocket Launcher • versão independente\nSem ROMs ou motores de emulação incluídos. Inspirado nas funcionalidades documentadas do ZNeko; sem código ou assets do projeto original.",12,PocketUi.MUTED,false),PocketUi.lp(c,-1,-2,7,0,7,0));
    }
    private void empty(String headline,String explanation,String cta,Runnable action){
        LinearLayout card=PocketUi.vertical(c);card.setPadding(PocketUi.dp(c,21),PocketUi.dp(c,32),PocketUi.dp(c,21),PocketUi.dp(c,25));
        card.setBackground(PocketUi.gradient(PocketUi.RAISED,PocketUi.SURFACE,22,c));
        card.addView(label("▧",39,host.accent,true));
        card.addView(label(headline,21,PocketUi.TEXT,true),PocketUi.lp(c,-1,-2,0,13,0,8));
        card.addView(label(explanation,14,PocketUi.MUTED,false),PocketUi.lp(c,-1,-2,0,0,0,20));
        card.addView(button(cta,action,true),PocketUi.lp(c,-1,47,0,0,0,0));
        content.addView(card,PocketUi.lp(c,-1,-2,2,12,2,0));
    }
    private void gameGrid(List<LibraryStore.Game> games){content.addView(grid(games));}
    private LinearLayout grid(List<LibraryStore.Game> games){
        LinearLayout root=PocketUi.vertical(c);
        for(int i=0;i<games.size();i+=2){
            LinearLayout row=PocketUi.horizontal(c);row.setGravity(Gravity.TOP);
            for(int j=i;j<i+2;j++){
                View card=j<games.size()?gameCard(games.get(j)):new View(c);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
                p.setMargins(PocketUi.dp(c,3),0,PocketUi.dp(c,8),PocketUi.dp(c,15));row.addView(card,p);
            }
            root.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }return root;
    }
    private View gameCard(LibraryStore.Game game){
        LinearLayout outer=PocketUi.vertical(c);outer.setBackground(PocketUi.shape(PocketUi.SURFACE,18,c));
        outer.setPadding(PocketUi.dp(c,5),PocketUi.dp(c,5),PocketUi.dp(c,5),PocketUi.dp(c,11));
        FrameLayout artwork=new FrameLayout(c);artwork.setBackground(PocketUi.gradient(0xff354258,0xff202737,14,c));
        artwork.setClipToOutline(true);
        TextView placeholder=label(game.system,26,host.accent,true);placeholder.setGravity(Gravity.CENTER);
        artwork.addView(placeholder,new FrameLayout.LayoutParams(-1,-1));
        ImageView image=new ImageView(c);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        artwork.addView(image,new FrameLayout.LayoutParams(-1,-1));host.covers.into(image,game);
        outer.addView(artwork,PocketUi.lp(c,-1,175,0,0,0,10));
        outer.addView(label(game.title,13,PocketUi.TEXT,true),PocketUi.lp(c,-1,44,7,0,7,2));
        LinearLayout metadata=PocketUi.horizontal(c);
        metadata.addView(label(game.system,10,PocketUi.MUTED,true),new LinearLayout.LayoutParams(0,-2,1));
        metadata.addView(label(game.favorite?"♥":"⋮",17,game.favorite?host.accent:PocketUi.MUTED,true));
        outer.addView(metadata,PocketUi.lp(c,-1,23,7,0,8,0));
        outer.setOnClickListener(v->host.details(game));
        outer.setOnLongClickListener(v->{host.showActions(game);return true;});
        return outer;
    }
}

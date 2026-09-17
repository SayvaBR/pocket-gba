package br.sayva.pocketlauncher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.TextUtils;
import java.util.Locale;

/** A Pocket-owned portrait game hub. Never fabricate artwork, metadata or achievements. */
public final class GameDetailsActivity extends Activity {
    static final String GAME_ID="game_id";
    static final String RESULT_ACTION="detail_action";
    static final String ACTION_PLAY="play";
    static final String ACTION_EDIT="edit";
    private LibraryStore store;
    private LibraryStore.Game game;
    private CoverLoader covers;
    private int accent;
    private LinearLayout body;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(PocketUi.BG);
        getWindow().setNavigationBarColor(PocketUi.BG);
        store=new LibraryStore(this);
        covers=new CoverLoader(this);
        accent=getSharedPreferences("pocket_navigation_v1",MODE_PRIVATE).getInt("accent",PocketUi.ACCENT);
        String id=getIntent().getStringExtra(GAME_ID);
        for(LibraryStore.Game found:store.snapshot())if(found.id.equals(id)){game=found;break;}
        if(game==null){finish();return;}
        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(PocketUi.BG);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30){
                android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                v.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            }else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        body=PocketUi.vertical(this);
        body.setPadding(dp(20),dp(16),dp(20),dp(38));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);root.requestApplyInsets();draw();
    }
    private int dp(int amount){return PocketUi.dp(this,amount);}
    private TextView label(String text,int size,int color,boolean bold){return PocketUi.text(this,text,size,color,bold);}
    private LinearLayout horizontal(){return PocketUi.horizontal(this);}
    private LinearLayout vertical(){return PocketUi.vertical(this);}
    private void action(String value){
        Intent result=new Intent().putExtra(GAME_ID,game.id).putExtra(RESULT_ACTION,value);
        setResult(RESULT_OK,result);finish();
    }
    private TextView button(String caption,boolean primary,Runnable onClick){
        TextView button=PocketUi.action(this,caption,primary);
        button.setTextSize(15);
        button.setBackground(PocketUi.shape(primary?accent:PocketUi.RAISED,16,this));
        button.setTextColor(primary?PocketUi.BG:PocketUi.TEXT);
        button.setOnClickListener(v->onClick.run());
        return button;
    }
    private static String platform(String code){
        return switch(code){
            case "GBA" -> "GAME BOY ADVANCE";
            case "GB" -> "GAME BOY";
            case "GBC" -> "GAME BOY COLOR";
            case "SNES" -> "SUPER NINTENDO";
            case "NDS" -> "NINTENDO DS";
            default -> code;
        };
    }
    private void chip(LinearLayout row,String text,int background){
        TextView view=label(text,11,PocketUi.TEXT,true);
        view.setPadding(dp(11),dp(8),dp(11),dp(8));
        view.setBackground(PocketUi.shape(background,14,this));
        row.addView(view,PocketUi.lp(this,-2,-2,0,0,7,8));
    }
    private void draw(){
        body.removeAllViews();
        LinearLayout header=horizontal();
        TextView back=button("‹  Voltar",false,this::finish);
        header.addView(back,PocketUi.lp(this,-2,43,0,0,12,0));
        TextView brand=label("POCKET / JOGO",11,accent,true);brand.setLetterSpacing(.17f);
        header.addView(brand,new LinearLayout.LayoutParams(0,-1,1));
        TextView menu=button("⋯",false,()->action(ACTION_EDIT));
        menu.setContentDescription("Opções do jogo");
        header.addView(menu,PocketUi.lp(this,48,43,0,0,0,0));
        body.addView(header,PocketUi.lp(this,-1,47,0,0,0,22));

        FrameLayout hero=new FrameLayout(this);
        GradientDrawable heroBackground=PocketUi.gradient(0xff383957,0xff131828,26,this);
        heroBackground.setStroke(dp(1),0xff4b4551);
        hero.setBackground(heroBackground);
        hero.setClipToOutline(true);
        LinearLayout heroContent=vertical();heroContent.setGravity(Gravity.CENTER);
        heroContent.setPadding(dp(18),dp(24),dp(18),dp(21));
        TextView overline=label(platform(game.system),12,accent,true);overline.setLetterSpacing(.14f);
        overline.setGravity(Gravity.CENTER);
        heroContent.addView(overline,PocketUi.lp(this,-1,-2,0,0,0,17));
        FrameLayout artwork=new FrameLayout(this);
        artwork.setBackground(PocketUi.gradient(0xff52536a,0xff242a39,20,this));
        artwork.setClipToOutline(true);
        TextView empty=label(game.system,29,accent,true);empty.setGravity(Gravity.CENTER);
        artwork.addView(empty,new FrameLayout.LayoutParams(-1,-1));
        ImageView cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cover.setContentDescription("Capa automática de "+game.title);
        artwork.addView(cover,new FrameLayout.LayoutParams(-1,-1));
        covers.into(cover,game);
        heroContent.addView(artwork,PocketUi.lp(this,174,230,0,0,0,20));
        TextView title=label(game.title,25,PocketUi.TEXT,true);
        title.setGravity(Gravity.CENTER);title.setMaxLines(3);title.setEllipsize(TextUtils.TruncateAt.END);
        heroContent.addView(title,PocketUi.lp(this,-1,-2,0,0,0,4));
        TextView subtitle=label("Sua coleção pessoal",12,PocketUi.MUTED,false);
        subtitle.setGravity(Gravity.CENTER);heroContent.addView(subtitle);
        hero.addView(heroContent,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));
        body.addView(hero,PocketUi.lp(this,-1,-2,0,0,0,18));

        LinearLayout facts=horizontal();facts.setGravity(Gravity.CENTER);
        chip(facts,game.system,0xff34374a);
        chip(facts,game.favorite?"♥ Favorito":"♡ Coleção",0xff34374a);
        body.addView(facts,PocketUi.lp(this,-1,-2,0,0,0,11));
        TextView play=button("▶   JOGAR AGORA",true,()->action(ACTION_PLAY));
        play.setTextSize(17);
        body.addView(play,PocketUi.lp(this,-1,58,0,0,0,11));
        LinearLayout secondary=horizontal();
        secondary.addView(button(game.favorite?"♥  Remover favorito":"♡  Favoritar",false,()->{
            // LibraryStore.edit mutates the same in-memory Game object; do NOT invert a second time.
            store.edit(game,null,!game.favorite,null);draw();
        }),new LinearLayout.LayoutParams(0,dp(49),1));
        secondary.addView(button("✎  Editar",false,()->action(ACTION_EDIT)),PocketUi.lp(this,-2,49,9,0,0,0));
        body.addView(secondary,PocketUi.lp(this,-1,-2,0,0,0,25));

        body.addView(label("INFORMAÇÕES DO JOGO",11,accent,true),PocketUi.lp(this,-1,-2,1,0,0,10));
        LinearLayout panel=vertical();
        panel.setPadding(dp(17),dp(12),dp(17),dp(14));
        panel.setBackground(PocketUi.outlined(PocketUi.SURFACE,0xff393d4d,20,this));
        info(panel,"Console",platform(game.system));
        info(panel,"Arquivo",game.fileName);
        info(panel,"Último acesso",game.lastPlayed==0?"Ainda não jogado":
            android.text.format.DateFormat.format("dd/MM/yyyy • HH:mm",game.lastPlayed).toString());
        boolean nativeCore=game.system.equals("GBA")||game.system.equals("GB")||game.system.equals("GBC");
        info(panel,"Como jogar",nativeCore?"Motor mGBA integrado":"Emulador externo necessário");
        body.addView(panel,PocketUi.lp(this,-1,-2,0,0,0,17));
        TextView note=label("As capas são buscadas automaticamente quando há correspondência no catálogo. O Pocket não remove sua ROM nem seus saves ao ocultar um jogo.",12,PocketUi.MUTED,false);
        body.addView(note,PocketUi.lp(this,-1,-2,4,0,4,0));
    }
    private void info(LinearLayout panel,String heading,String value){
        LinearLayout row=vertical();
        TextView key=label(heading.toUpperCase(Locale.ROOT),10,PocketUi.MUTED,true);key.setLetterSpacing(.08f);
        row.addView(key,PocketUi.lp(this,-1,-2,0,0,0,3));
        TextView data=label(value,14,PocketUi.TEXT,true);data.setMaxLines(3);
        row.addView(data);
        panel.addView(row,PocketUi.lp(this,-1,-2,0,5,0,12));
    }
}

package br.sayva.pocketlauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Own layout primitives: no inherited multi-emulator frontend UI. */
final class PocketUi {
    static final int BG=0xff0d1019, SURFACE=0xff1b202c, RAISED=0xff272c39;
    static final int TEXT=0xfff4f5fb, MUTED=0xff9ca6b7, ACCENT=0xffffbd68;
    static final int[] PLATFORM={0xffb69bff,0xff53d8b1,0xffffb277,0xff76c7f5,0xffff8baf};
    static int dp(Context c,float n){return (int)(c.getResources().getDisplayMetrics().density*n+0.5f);}
    static GradientDrawable shape(int color,float radius,Context c) {
        GradientDrawable result=new GradientDrawable();result.setColor(color);result.setCornerRadius(dp(c,radius));return result;
    }
    static GradientDrawable outlined(int color,int outline,float radius,Context c) {
        GradientDrawable result=shape(color,radius,c);result.setStroke(dp(c,1),outline);return result;
    }
    static GradientDrawable gradient(int first,int last,float radius,Context c) {
        GradientDrawable result=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{first,last});
        result.setCornerRadius(dp(c,radius));return result;
    }
    static TextView text(Context c,String content,int size,int color,boolean bold) {
        TextView text=new TextView(c);text.setText(content);text.setTextSize(size);text.setTextColor(color);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setFontFeatureSettings("kern");if(bold)text.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        return text;
    }
    static TextView action(Context c,String label,boolean selected) {
        TextView view=text(c,label,13,selected?BG:TEXT,true);
        view.setGravity(Gravity.CENTER);view.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));
        view.setBackground(shape(selected?ACCENT:RAISED,18,c));
        return view;
    }
    static LinearLayout vertical(Context c) {LinearLayout l=new LinearLayout(c);l.setOrientation(1);return l;}
    static LinearLayout horizontal(Context c) {LinearLayout l=new LinearLayout(c);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    static LinearLayout.LayoutParams lp(Context c,int w,int h,int left,int top,int right,int bottom) {
        LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(w<0?w:dp(c,w),h<0?h:dp(c,h));
        l.setMargins(dp(c,left),dp(c,top),dp(c,right),dp(c,bottom));return l;
    }
    static void space(LinearLayout parent,Context c,int height) {parent.addView(new View(c),lp(c,1,height,0,0,0,0));}
    static Drawable dots(Context c) {
        float density=c.getResources().getDisplayMetrics().density;
        return new Drawable(){Paint p=new Paint(3);public void draw(Canvas canvas){
            canvas.drawColor(BG);p.setColor(0x193d465c);
            for(float y=9*density;y<getBounds().height();y+=23*density)
                for(float x=9*density;x<getBounds().width();x+=23*density)canvas.drawCircle(x,y,1.3f*density,p);
        }
        public void setAlpha(int alpha){} public void setColorFilter(android.graphics.ColorFilter filter){}
        public int getOpacity(){return PixelFormat.OPAQUE;}};
    }
}

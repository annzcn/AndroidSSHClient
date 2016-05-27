package com.zou.sshclient;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by zou on 2016/3/8.
 */
public class CursorView extends View {
    private Paint cursorPaint;
    private int with,height;
    public CursorView(Context context) {
        super(context);
        init();
    }

    public CursorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CursorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        cursorPaint = new Paint();
        cursorPaint.setColor(Color.parseColor("#fc6c1f"));
        cursorPaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(with,height);
    }

    public void setSize(int with,int height){
        this.with = with;
        this.height = height;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0,0,with,height,cursorPaint);
    }
}

package com.github.wensimin.rikaisya.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import static android.content.ContentValues.TAG;

/**
 * 框选截图用view
 */
public class CaptureView extends View {
    private Paint linePaint;
    private static final float DEFAULT_LINE_LENGTH = 300f;

    public CaptureView(Context context) {
        super(context);
        init();
    }


    public CaptureView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CaptureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(Color.BLUE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawLines(getDefaultPts(), linePaint);
        super.onDraw(canvas);
    }

    private float[] getDefaultPts() {
        int width = getWidth();
        int height = getHeight();
        float left = width / 2f - DEFAULT_LINE_LENGTH;
        float right = width / 2f + DEFAULT_LINE_LENGTH;
        float top = height / 2f - DEFAULT_LINE_LENGTH / 2f;
        float bottom = height / 2f + DEFAULT_LINE_LENGTH / 2f;
        return getLinePts(left, right, top, bottom);
    }

    private float[] getLinePts(float left, float right, float top, float bottom) {
        return new float[]{
                left, top, right, top,
                left, top, left, bottom,
                left, bottom, right, bottom,
                right, top, right, bottom
        };
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent: action:" + event.getAction());
        this.performClick();
        return true;
    }



    @Override
    public boolean performClick() {
        return super.performClick();
    }
}

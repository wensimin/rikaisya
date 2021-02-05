package com.github.wensimin.rikaisya.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * 始终拥有焦点的textView
 */
@SuppressLint("AppCompatCustomView")
public class AlwaysFocusTextView extends TextView {
    public AlwaysFocusTextView(Context context) {
        super(context);
    }

    public AlwaysFocusTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AlwaysFocusTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean isFocused() {
        return true;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
//        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        //super.onWindowFocusChanged(hasWindowFocus);
    }
}

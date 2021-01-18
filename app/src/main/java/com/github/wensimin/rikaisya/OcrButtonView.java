package com.github.wensimin.rikaisya;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.Nullable;

/**
 * ocr 按钮view
 */
@SuppressLint("AppCompatCustomView")
public class OcrButtonView extends ImageView {
    public OcrButtonView(Context context) {
        super(context);
    }

    public OcrButtonView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OcrButtonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

}

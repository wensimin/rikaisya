package com.github.wensimin.rikaisya.adapter;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.activity.MainActivity;

import java.util.Objects;

/**
 * 剪贴板监听rikai listener
 */
public class RikaiClipChangeListener implements ClipboardManager.OnPrimaryClipChangedListener {


    private final Context context;
    private final WindowManager windowManager;
    private final Handler mDelayHandler;
    public static final String ACTION_NAME = "RIKAI";
    // 是否已经有浮窗状态
    private boolean isFloating = false;

    public RikaiClipChangeListener(Handler mDelayHandler, Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mDelayHandler = mDelayHandler;
    }

    @Override
    public void onPrimaryClipChanged() {
        if (isFloating)
            return;
        // 设置LayoutParam
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.x = 300;
        layoutParams.y = 500;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        View floatView = LayoutInflater.from(context).inflate(R.layout.rikai_btn, new FrameLayout(context), false);
        floatView.setOnClickListener(view -> {
            this.deleteFloatView(floatView, 0);
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ACTION_NAME, true);
            context.startActivity(intent);
        });
        windowManager.addView(floatView, layoutParams);
        isFloating = true;
        this.deleteFloatView(floatView, 5 * 1000);
    }


    /**
     * 删除floatView
     *
     * @param floatView floatView
     * @param time      time ms
     */
    private void deleteFloatView(View floatView, int time) {
        mDelayHandler.postDelayed(() -> {
            if (floatView.isShown()) {
                windowManager.removeView(floatView);
                isFloating = false;
            }
        }, time);
    }

}

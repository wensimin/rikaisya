package com.github.wensimin.rikaisya.service;

import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.activity.MainActivity;
import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.utils.SystemUtils;

/**
 * 浮动窗服务
 */
public class RikaiFloatingService extends Service {
    private final Handler mDelayHandler = new Handler(Looper.getMainLooper());
    public static final String ACTION_NAME = "RIKAI";
    private final ClipboardManager.OnPrimaryClipChangedListener listener = this::dialogButton;
    // 是否已经有浮窗状态
    private boolean isFloating = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        // 剪贴板变动时创建按钮
        ClipboardManager clipboardManager = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(listener);
    }


    /**
     * 弹出按钮
     */
    private void dialogButton() {
        if (isFloating)
            return;
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 设置LayoutParam
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.x = 300;
        layoutParams.y = 500;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        View floatView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.rikai_btn, new FrameLayout(getApplicationContext()), false);
        floatView.setOnClickListener(view -> {
            this.deleteFloatView(floatView, 0);
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ACTION_NAME, true);
            getApplication().startActivity(intent);
        });
        SystemUtils.addView(windowManager, floatView, layoutParams);
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
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDelayHandler.postDelayed(() -> {
            SystemUtils.removeView(windowManager, floatView);
            isFloating = false;
        }, time);
    }


    @Override
    public void onDestroy() {
        ClipboardManager clipboardManager = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.removePrimaryClipChangedListener(listener);
        mDelayHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

}

package com.github.wensimin.rikaisya;

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

/**
 * 浮动窗服务
 */
public class FloatingService extends Service {
    private final Handler mDelayHandler = new Handler(Looper.getMainLooper());
    public static final String ACTION_NAME = "RIKAI";
    private final ClipboardManager.OnPrimaryClipChangedListener listener = this::dialogButton;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 剪贴板变动时创建按钮
        ClipboardManager clipboardManager = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(listener);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 弹出按钮
     */
    private void dialogButton() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 设置LayoutParam
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;//不耽误其他事件
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.x = 300;
        layoutParams.y = 500;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        View floatView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.float_btn, new FrameLayout(getApplicationContext()), false);
        floatView.setOnClickListener(view -> {
            this.deleteFloatView(view, 100);
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ACTION_NAME, true);
            getApplication().startActivity(intent);
        });
        windowManager.addView(floatView, layoutParams);
        this.deleteFloatView(floatView, 5 * 1000);
    }

    /**
     * 删除floatView
     *
     * @param floatView floatView
     * @param time      time ms
     */
    private void deleteFloatView(View floatView, int time) {
        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Service.WINDOW_SERVICE);
        mDelayHandler.postDelayed(() -> {
            if (floatView.getParent() != null) {
                windowManager.removeView(floatView);
            }
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

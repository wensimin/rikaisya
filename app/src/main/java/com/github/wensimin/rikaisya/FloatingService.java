package com.github.wensimin.rikaisya;

import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.annotation.Nullable;

/**
 * 浮动窗服务
 */
public class FloatingService extends Service {

    public static final String ACTION_NAME = "RIKAI";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ClipboardManager clipboardManager = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        // 剪贴板变动时创建按钮
        clipboardManager.addPrimaryClipChangedListener(this::dialogButton);
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
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;//不耽误其他事件
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // fixme hardcode
        layoutParams.x = 300;
        layoutParams.y = 500;
        layoutParams.width = 150;
        layoutParams.height = 150;
        ImageButton button = new ImageButton(this);
        button.setImageResource(android.R.drawable.ic_menu_search);
        button.setOnClickListener(b -> {
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ACTION_NAME, true);
            getApplication().startActivity(intent);
        });
        windowManager.addView(button, layoutParams);
        new Thread(() -> {
            try {
                // fixme hardcode
                Thread.sleep(5000);
                windowManager.removeView(button);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}

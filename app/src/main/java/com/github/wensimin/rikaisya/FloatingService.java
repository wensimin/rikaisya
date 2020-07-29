package com.github.wensimin.rikaisya;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * 浮动窗服务
 */
public class FloatingService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 设置LayoutParam
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.width = 500;
        layoutParams.height = 100;
        layoutParams.x = 300;
        layoutParams.y = 300;
        // TODO 临时 新建悬浮窗控件
        Button button = new Button(getApplicationContext());
        button.setText("Floating Window");
        button.setBackgroundColor(Color.BLUE);
        //TODO 使用现有部件
//        LayoutInflater inflater=LayoutInflater.from(this);
//        View layout= inflater.inflate(R.layout.activity_main,null);
//        FloatingActionButton fab = layout.findViewById(R.id.fab);
        windowManager.addView(button, layoutParams);
        return super.onStartCommand(intent, flags, startId);
    }
}

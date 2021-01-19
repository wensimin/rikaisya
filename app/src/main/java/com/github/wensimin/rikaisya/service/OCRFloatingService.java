package com.github.wensimin.rikaisya.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.R;

import static android.content.ContentValues.TAG;

/**
 * ocr service
 */
//TODO 整理代码
public class OCRFloatingService extends Service {

    private View floatView;
    private WindowManager.LayoutParams layoutParams;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 设置LayoutParam
        layoutParams = new WindowManager.LayoutParams();
        floatView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.ocr_btn, new FrameLayout(getApplicationContext()), false);
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // 初始显示居中 TODO 持久化用户移动后位置
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        layoutParams.x = point.x / 2 - floatView.getWidth() / 2;
        layoutParams.y = point.y / 2 - floatView.getHeight() / 4;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        final Handler handler = new Handler();
        // 长按事件
        Runnable longPressEvent = () -> Log.d(TAG, "Long press!");
        // 拖动事件
        floatView.setOnTouchListener(new View.OnTouchListener() {
            private int oldX;
            private int oldY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        oldX = (int) event.getRawX();
                        oldY = (int) event.getRawY();
                        handler.postDelayed(longPressEvent, 1000);
                        break;
                    case MotionEvent.ACTION_UP:
                        boolean isClick = Math.abs(oldX - event.getRawX()) <= 10 && Math.abs(oldY - event.getRawY()) <= 10;
                        handler.removeCallbacks(longPressEvent);
                        if (isClick) {
                            view.performClick();
                            break;
                        }
                    default:
                        Log.d(TAG, "on touch");
                        handler.removeCallbacks(longPressEvent);
                        layoutParams.x = (int) event.getRawX() - floatView.getWidth() / 2;
                        layoutParams.y = (int) event.getRawY() - floatView.getHeight() / 2 - 25;
                        windowManager.updateViewLayout(floatView, layoutParams);
                }
                return true;
            }
        });
        // 单击事件
        floatView.setOnClickListener(view -> {
            Log.d(TAG, "on click");
        });
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (!floatView.isShown()) {
            windowManager.addView(floatView, layoutParams);
        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (floatView.isShown()) {
            windowManager.removeView(floatView);
        }
        super.onDestroy();
    }
}

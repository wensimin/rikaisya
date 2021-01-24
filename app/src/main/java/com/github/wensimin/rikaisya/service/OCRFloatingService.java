package com.github.wensimin.rikaisya.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.view.CaptureView;

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
        // 初始显示居中 持久化用户移动后位置
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        int defaultX = point.x / 2 - floatView.getWidth() / 2;
        int defaultY = point.y / 2 - floatView.getHeight() / 4;
        int x = preferences.getInt(OCRTouchListener.CURRENT_X_KEY, defaultX);
        int y = preferences.getInt(OCRTouchListener.CURRENT_Y_KEY, defaultY);
        layoutParams.x = x;
        layoutParams.y = y;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        // 拖动事件
        floatView.setOnTouchListener(new OCRTouchListener(windowManager,
                // 长按事件
                () -> Log.d(TAG, "Long press!")));
        // 单击事件
        floatView.setOnClickListener(this::startCapture);
        super.onCreate();
    }

    /**
     * 开始进行屏幕截图
     *
     * @param view ocr按钮view
     */
    private void startCapture(View view) {
        View captureView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.capture, new FrameLayout(getApplicationContext()), false);
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        windowManager.addView(captureView, layoutParams);
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

    /**
     * OCR touch监听
     * 可移动按钮+长按事件
     */
    private class OCRTouchListener implements View.OnTouchListener {
        private int oldX;
        private int oldY;
        public static final String CURRENT_X_KEY = "current_x";
        public static final String CURRENT_Y_KEY = "current_y";
        private final WindowManager windowManager;
        private final Handler handler = new Handler();
        // 长按事件
        private final Runnable longPressEvent;
        // 已经长按
        private boolean longClicked = false;

        OCRTouchListener(WindowManager windowManager, Runnable longPressEvent) {
            this.windowManager = windowManager;
            this.longPressEvent = () -> {
                longPressEvent.run();
                longClicked = true;
            };
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    longClicked = false;
                    oldX = (int) event.getRawX();
                    oldY = (int) event.getRawY();
                    handler.postDelayed(longPressEvent, 1000);
                    break;
                case MotionEvent.ACTION_UP:
                    boolean isClick = Math.abs(oldX - event.getRawX()) <= 10 && Math.abs(oldY - event.getRawY()) <= 10;
                    handler.removeCallbacks(longPressEvent);
                    if (isClick && !longClicked) {
                        view.performClick();
                    }
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(CURRENT_X_KEY, layoutParams.x);
                    editor.putInt(CURRENT_Y_KEY, layoutParams.y);
                    editor.apply();
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d(TAG, "on touch");
                    handler.removeCallbacks(longPressEvent);
                    layoutParams.x = (int) event.getRawX() - floatView.getWidth() / 2;
                    layoutParams.y = (int) event.getRawY() - floatView.getHeight() / 2 - 25;
                    this.windowManager.updateViewLayout(floatView, layoutParams);
            }
            return true;
        }
    }

}

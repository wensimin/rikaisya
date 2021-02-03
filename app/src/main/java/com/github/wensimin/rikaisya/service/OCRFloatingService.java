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
import com.github.wensimin.rikaisya.activity.ScreenActivity;
import com.github.wensimin.rikaisya.utils.SystemUtils;
import com.github.wensimin.rikaisya.view.CaptureView;

import static android.content.ContentValues.TAG;

/**
 * ocr service
 */
//TODO 整理代码
public class OCRFloatingService extends Service {

    private View OCRButton;
    private WindowManager.LayoutParams layoutParams;
    private WindowManager windowManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 设置LayoutParam
        layoutParams = new WindowManager.LayoutParams();
        OCRButton = LayoutInflater.from(getApplicationContext()).inflate(R.layout.ocr_btn, new FrameLayout(getApplicationContext()), false);
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // 初始显示居中 持久化用户移动后位置
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        int defaultX = point.x / 2 - OCRButton.getWidth() / 2;
        int defaultY = point.y / 2 - OCRButton.getHeight() / 4;
        int x = preferences.getInt(OCRTouchListener.OCR_X_KEY, defaultX);
        int y = preferences.getInt(OCRTouchListener.OCR_Y_KEY, defaultY);
        layoutParams.x = x;
        layoutParams.y = y;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        // 拖动事件
        OCRButton.setOnTouchListener(new OCRTouchListener(windowManager,
                // 长按事件
                this::startCapture));
        // 单击事件
        OCRButton.setOnClickListener(this::startCapture);
        super.onCreate();
    }


    /**
     * 开始进行屏幕截图
     *
     * @param view ocr按钮view
     * TODO lock
     */
    private void startCapture(View view) {
        FrameLayout layout = (FrameLayout) LayoutInflater.from(getApplicationContext()).inflate(R.layout.capture, new FrameLayout(getApplicationContext()), false);
        CaptureView captureView = (CaptureView) layout.getChildAt(0);
        WindowManager.LayoutParams capLayoutParams = new WindowManager.LayoutParams();
        capLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        capLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        capLayoutParams.format = PixelFormat.TRANSLUCENT;
        captureView.setListener(new CaptureView.ResListener() {
            @Override
            public void confirm(float left, float right, float top, float bottom) {
                SystemUtils.removeView(windowManager, layout);
                startCapture();
            }

            @Override
            public void cancel() {
                SystemUtils.removeView(windowManager, layout);
            }
        });
        SystemUtils.addView(windowManager, layout, capLayoutParams);
    }

    /**
     * 开始截图activity
     */
    //TODO LOCK 避免重复操作
    private void startCapture() {
        Intent intent = new Intent(getBaseContext(), ScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SystemUtils.addView(windowManager, OCRButton, layoutParams);
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        SystemUtils.removeView(windowManager, OCRButton);
        stopService(new Intent(this, ScreenCapService.class));
        super.onDestroy();
    }

    /**
     * OCR touch监听
     * 可移动按钮+长按事件
     */
    private class OCRTouchListener implements View.OnTouchListener {
        private int oldX;
        private int oldY;
        public static final String OCR_X_KEY = "OCR_X_KEY";
        public static final String OCR_Y_KEY = "OCR_Y_KEY";
        private final WindowManager windowManager;
        private final Handler handler = new Handler();
        // 长按事件
        private final Runnable longPressEvent;
        // 已经长按
        private boolean longClicked = false;

        OCRTouchListener(WindowManager windowManager, Runnable longPressEvent) {
            this.windowManager = windowManager;
            this.longPressEvent = () -> {
                //TODO 长按事件优化，手机上较难触发
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
                    editor.putInt(OCR_X_KEY, layoutParams.x);
                    editor.putInt(OCR_Y_KEY, layoutParams.y);
                    editor.apply();
                    break;
                case MotionEvent.ACTION_MOVE:
                    handler.removeCallbacks(longPressEvent);
                    layoutParams.x = (int) event.getRawX() - OCRButton.getWidth() / 2;
                    layoutParams.y = (int) event.getRawY() - OCRButton.getHeight() / 2 - 25;
                    Log.d(TAG, String.format("x %s->%s y%s->%s", layoutParams.x, event.getRawX(), layoutParams.y, event.getRawY()));
                    this.windowManager.updateViewLayout(OCRButton, layoutParams);
            }
            return true;
        }
    }

}

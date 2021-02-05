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
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.activity.ScreenActivity;
import com.github.wensimin.rikaisya.utils.PerformanceTimer;
import com.github.wensimin.rikaisya.utils.SystemUtils;
import com.github.wensimin.rikaisya.view.CaptureView;

import static android.content.ContentValues.TAG;
import static com.github.wensimin.rikaisya.activity.ScreenActivity.ACTION_CLOSE;

/**
 * ocr service
 */
//TODO 整理代码
public class OCRFloatingService extends Service {

    private View OCRButton;
    private WindowManager.LayoutParams layoutParams;
    private WindowManager windowManager;
    private FrameLayout CapLayout;
    // 长按判定毫秒
    private static final int LONG_CLICK_INTERVAL_MS = 500;
    // 下次允许截图的事件
    private long nextTime;
    // 截图间隔秒数
    private static final int CAP_INTERVAL = 3;

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
        CapLayout = (FrameLayout) LayoutInflater.from(getApplicationContext()).inflate(R.layout.capture, new FrameLayout(getApplicationContext()), false);
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
        OCRButton.setOnTouchListener(new OCRTouchListener(OCRButton, windowManager,
                // 长按事件
                this::startCapture));
        // 单击事件
        OCRButton.setOnClickListener(this::openCaptureView);
        super.onCreate();
    }


    /**
     * 开始进行屏幕截图
     *
     * @param view ocr按钮view
     */
    private void openCaptureView(View view) {
        CaptureView captureView = (CaptureView) CapLayout.getChildAt(0);
        WindowManager.LayoutParams capLayoutParams = new WindowManager.LayoutParams();
        capLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        capLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        capLayoutParams.format = PixelFormat.TRANSLUCENT;
        captureView.setListener(new CaptureView.ResListener() {
            @Override
            public void confirm() {
                SystemUtils.removeView(windowManager, CapLayout);
                startCapture();
            }

            @Override
            public void cancel() {
                SystemUtils.removeView(windowManager, CapLayout);
            }
        });
        SystemUtils.addView(windowManager, CapLayout, capLayoutParams);
    }

    /**
     * 开始截图activity
     */
    private void startCapture() {
        if (nextTime > System.currentTimeMillis()) {
            Toast.makeText(getApplicationContext(), "OCR操作过于频繁", Toast.LENGTH_LONG).show();
            return;
        }
        nextTime = System.currentTimeMillis() + CAP_INTERVAL * 1000;
        Log.d(TAG, "开始截图");
        PerformanceTimer.start();
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
        stopService(new Intent(this, OCRResultService.class));
        Intent intent = new Intent(getBaseContext(), ScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(ACTION_CLOSE,true);
        startActivity(intent);
        super.onDestroy();
    }

    /**
     * OCR touch监听
     * 可移动按钮+长按事件
     */
    private class OCRTouchListener implements View.OnTouchListener {
        private int startX;
        private int startY;
        public static final String OCR_X_KEY = "OCR_X_KEY";
        public static final String OCR_Y_KEY = "OCR_Y_KEY";
        private final WindowManager windowManager;
        private final Handler handler = new Handler();
        // 长按事件
        private final Runnable longPressEvent;
        // 已经长按
        private boolean longClicked = false;

        OCRTouchListener(View view, WindowManager windowManager, Runnable longPressEvent) {
            this.windowManager = windowManager;
            this.longPressEvent = () -> {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                longPressEvent.run();
                longClicked = true;
            };
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            int x = (int) event.getRawX();
            int y = (int) event.getRawY();
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    longClicked = false;
                    startX = x;
                    startY = y;
                    handler.postDelayed(longPressEvent, LONG_CLICK_INTERVAL_MS);
                    break;
                case MotionEvent.ACTION_UP:
                    boolean isClick = Math.abs(startX - x) <= 10 && Math.abs(startY - y) <= 10;
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
                    if (moveRangeMini(x, y)) {
                        break;
                    }
                    handler.removeCallbacks(longPressEvent);
                    layoutParams.x = x - OCRButton.getWidth() / 2;
                    layoutParams.y = y - OCRButton.getHeight() / 2 - 25;
                    Log.d(TAG, String.format("x %s->%s y%s->%s", layoutParams.x, x, layoutParams.y, y));
                    this.windowManager.updateViewLayout(OCRButton, layoutParams);
            }
            return true;
        }

        private boolean moveRangeMini(int x, int y) {
            int miniRange = 10;
            return Math.abs(startX - x) < miniRange || Math.abs(startY - y) < miniRange;
        }
    }

}

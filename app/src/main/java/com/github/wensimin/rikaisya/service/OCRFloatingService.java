package com.github.wensimin.rikaisya.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.utils.SystemUtils;

/**
 * ocr service
 */
public class OCRFloatingService extends Service {
    private static final int FOREGROUND_ID = 1;
    private OCRFloatViewManager ocrFloatViewManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        SystemUtils.switchToForeground(this, FOREGROUND_ID);
        ocrFloatViewManager = new OCRFloatViewManager(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ocrFloatViewManager.showFloatButton();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        ocrFloatViewManager.destroy();
        super.onDestroy();
    }

}

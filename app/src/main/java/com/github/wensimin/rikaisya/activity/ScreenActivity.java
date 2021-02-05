package com.github.wensimin.rikaisya.activity;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.contract.ScreenCaptureContract;
import com.github.wensimin.rikaisya.service.OCRResultService;
import com.github.wensimin.rikaisya.utils.PerformanceTimer;

import java.lang.reflect.Field;
import java.util.Optional;

import static android.content.ContentValues.TAG;

public class ScreenActivity extends ComponentActivity {
    public static final String ACTION_CLOSE = "close";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 只在给定action为close的时候才finish activity
        // FIXME 待观察是否会有内存泄露
        boolean close = Optional.ofNullable(getIntent()).
                map(Intent::getExtras).
                map(e -> e.getBoolean(ACTION_CLOSE)).
                orElse(false);
        if (close) {
            finish();
        } else {
            this.startCapture();
        }
    }


    private void startCapture() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        ActivityResultLauncher<Integer> integerActivityResultLauncher = registerForActivityResult(new ScreenCaptureContract(mediaProjectionManager),
                result -> {
                    if (result != null) {
                        Log.d(TAG, "用户允许截图 消耗时间:" + PerformanceTimer.cut());
                        Intent i = new Intent(this, OCRResultService.class)
                                .putExtra(OCRResultService.EXTRA_RESULT_INTENT, result)
                                .putExtra(OCRResultService.EXTRA_RESULT_CODE, RESULT_OK);
                        startService(i);
                    }
                    moveTaskToBack(true);
//                    finish();
                });
        integerActivityResultLauncher.launch(null);
    }

    @Override
    protected void onResume() {
        // FIXME 暴力hack
        try {
            @SuppressWarnings("JavaReflectionMemberAccess") Field mFinished = Activity.class.getDeclaredField("mFinished");
            mFinished.setAccessible(true);
            Log.d(TAG, "onResume: mFinished" + mFinished.get(this));
            mFinished.setBoolean(this, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onResume();
    }


    @Override
    public void finish() {
        Log.d(TAG, "screenActivity finish!");
        super.finish();
    }
}

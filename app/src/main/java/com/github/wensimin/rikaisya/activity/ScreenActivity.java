package com.github.wensimin.rikaisya.activity;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.contract.ScreenCaptureContract;
import com.github.wensimin.rikaisya.utils.ScreenshotPermissionUtils;

import static android.content.ContentValues.TAG;

/**
 * 一次性activity 用于申请截图权限
 */
public class ScreenActivity extends ComponentActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getCapturePermission();
    }

    private void getCapturePermission() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        ActivityResultLauncher<Integer> integerActivityResultLauncher = registerForActivityResult(new ScreenCaptureContract(mediaProjectionManager),
                result -> {
                    if (result != null) {
                        Log.d(TAG, "用户允许截图");
                        ScreenshotPermissionUtils.setScreenshotPermission((Intent) result.clone());
                    } else {
                        Log.d(TAG, "用户拒绝截图");
                    }
                    finish();
                });
        integerActivityResultLauncher.launch(null);
    }

    @Override
    public void finish() {
        Log.d(TAG, "screenActivity finish!");
        super.finish();
    }
}

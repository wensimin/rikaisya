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
import com.github.wensimin.rikaisya.service.ScreenCapService;

import java.lang.reflect.Field;

import static android.content.ContentValues.TAG;

public class ScreenActivity extends ComponentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
//        Window window = this.getWindow();
//        WindowManager.LayoutParams p = window.getAttributes(); // 获取对话框当前的参数值
//        p.height = 0; // 高度设置为0
//        p.width = 0;//宽0
//        p.gravity = Gravity.CENTER;
//        window.setAttributes(p);
        this.startCapture();
    }



    private void startCapture() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        ActivityResultLauncher<Integer> integerActivityResultLauncher = registerForActivityResult(new ScreenCaptureContract(mediaProjectionManager),
                result -> {
                    if (result != null) {
                        Intent i = new Intent(this, ScreenCapService.class)
                                .putExtra(ScreenCapService.EXTRA_RESULT_INTENT, result)
                                .putExtra(ScreenCapService.EXTRA_RESULT_CODE, RESULT_OK);
                        startService(i);
                    }
                    finish();
                });
        integerActivityResultLauncher.launch(null);
    }

    @Override
    protected void onResume() {
        // TODO 暴力hack
        try {
            Field mFinished = Activity.class.getDeclaredField("mFinished");
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

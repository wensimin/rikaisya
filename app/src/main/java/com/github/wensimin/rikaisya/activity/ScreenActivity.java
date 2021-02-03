package com.github.wensimin.rikaisya.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.github.wensimin.rikaisya.contract.ScreenCaptureContract;
import com.github.wensimin.rikaisya.service.ScreenCapService;
import com.github.wensimin.rikaisya.utils.SystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

public class ScreenActivity extends ComponentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verifyStoragePermissions(this);
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


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//
//        super.onActivityResult(requestCode, resultCode, data);
//        Log.d(TAG, "onActivityResult: " + (requestCode == RESULT_OK));
//        if (resultCode == RESULT_OK) {
//            Intent i = new Intent(this, ScreenCapService.class)
//                    .putExtra(ScreenCapService.EXTRA_RESULT_INTENT, data)
//                    .putExtra(ScreenCapService.EXTRA_RESULT_CODE, resultCode);
//            startService(i);
//        }
//        // FIXME 执行activity之后应该没有任何残留
//        isFinish = true;
////        finishAndRemoveTask();
//    }


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
//        while (!isFinish) ;
        //FIXME 等待onActivityResult执行 应该不可靠
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        Log.d(TAG, "onResume: ");
//        finish();
//        finishAndRemoveTask();
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

    //fixme DELETE ! 临时申请权限,实质无需
    public static void verifyStoragePermissions(Activity activity) {
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE, 1);
        }
    }

    @Override
    public void finish() {
        Log.d(TAG, "screenActivity finish!");
        super.finish();
    }
}

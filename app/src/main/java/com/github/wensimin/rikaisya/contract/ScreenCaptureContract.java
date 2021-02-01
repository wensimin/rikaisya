package com.github.wensimin.rikaisya.contract;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ScreenCaptureContract extends ActivityResultContract<Integer, Intent> {
    private final MediaProjectionManager mediaProjectionManager;

    public ScreenCaptureContract(MediaProjectionManager mediaProjectionManager) {
        this.mediaProjectionManager = mediaProjectionManager;
    }

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, Integer input) {
        return mediaProjectionManager.createScreenCaptureIntent();
    }

    @Override
    public Intent parseResult(int resultCode, @Nullable Intent intent) {
        if (resultCode != Activity.RESULT_OK) {
            return null;
        }
        return intent;
    }
}

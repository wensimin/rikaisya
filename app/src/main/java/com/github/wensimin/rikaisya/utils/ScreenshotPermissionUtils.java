package com.github.wensimin.rikaisya.utils;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;

/**
 * hack
 * 保存intent的clone进行使用,不消耗掉原版
 * 理论上应该会与同时使用该api的其他应用冲突
 */
public class ScreenshotPermissionUtils {

    private static Intent screenshotPermission = null;


    public static MediaProjection getMediaProjection(MediaProjectionManager mediaProjectionManager) {
        if (screenshotPermission == null) {
            return null;
        }
        try {
            return mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) screenshotPermission.clone());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void setScreenshotPermission(final Intent permissionIntent) {
        screenshotPermission = permissionIntent;
    }
}

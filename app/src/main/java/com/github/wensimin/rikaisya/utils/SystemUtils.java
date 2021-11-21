package com.github.wensimin.rikaisya.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

public class SystemUtils {

    public static DisplayMetrics getScreenMetrics(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getDisplay().getRealMetrics(displayMetrics);
        if (SystemUtils.showNavigationBar(context.getResources())) {
            int navigationBarHeight = SystemUtils.getNavigationBarHeight(context);
            displayMetrics.heightPixels -= navigationBarHeight;
        }
        return displayMetrics;
    }

    public static int getStatusBarHeight(Resources resources) {
        int statusBarHeight = 0;
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    public static boolean showNavigationBar(Resources resources) {
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        return id > 0 && resources.getBoolean(id);
    }

    public static int getNavigationBarHeight(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        context.getDisplay().getRealMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        // 使用高度差求导航栏高度
        context.getDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight)
            return realHeight - usableHeight;
        else
            return 0;
    }

    /**
     * 删除view
     *
     * @param windowManager windowManager
     * @param layout        layout
     */
    public static void removeView(WindowManager windowManager, View layout) {
        if (layout.isShown()) {
            windowManager.removeView(layout);
        }
    }

    /**
     * add view
     *
     * @param windowManager windowManager
     * @param layout        layout
     * @param params        layoutParams
     */
    public static void addView(WindowManager windowManager, View layout, WindowManager.LayoutParams params) {
        if (!layout.isShown()) {
            windowManager.addView(layout, params);
        }
    }


}

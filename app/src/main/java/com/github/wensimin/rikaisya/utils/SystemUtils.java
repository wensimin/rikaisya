package com.github.wensimin.rikaisya.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import static android.content.Context.WINDOW_SERVICE;

public class SystemUtils {

    public static DisplayMetrics getScreenMetrics(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        if (SystemUtils.showNavigationBar(context.getResources())) {
            int navigationBarHeight = SystemUtils.getNavigationBarHeight(windowManager);
            displayMetrics.heightPixels -= navigationBarHeight;
        }
        return displayMetrics;
    }

    public static boolean showNavigationBar(Resources resources) {
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        return id > 0 && resources.getBoolean(id);
    }

    public static int getNavigationBarHeight(WindowManager windowManager) {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
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

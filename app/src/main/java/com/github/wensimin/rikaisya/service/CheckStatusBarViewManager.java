package com.github.wensimin.rikaisya.service;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.github.wensimin.rikaisya.utils.SystemUtils;

import static android.content.ContentValues.TAG;

public class CheckStatusBarViewManager {
    /**
     * 用于检查状态栏是否存在的view
     */
    private View checkStatusBarView;
    private final WindowManager windowManager;
    private final Context context;
    private final DisplayMetrics screenMetrics;

    public CheckStatusBarViewManager(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        screenMetrics = new DisplayMetrics();
        initCheckStatusBarView();
    }

    /**
     * 初始化检查状态栏的view
     */
    private void initCheckStatusBarView() {
        WindowManager.LayoutParams checkStatusBarViewLayout = new WindowManager.LayoutParams();
        checkStatusBarViewLayout.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        checkStatusBarViewLayout.gravity = Gravity.END | Gravity.TOP;
        checkStatusBarViewLayout.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        checkStatusBarViewLayout.width = 1;
        checkStatusBarViewLayout.height = WindowManager.LayoutParams.MATCH_PARENT;
        checkStatusBarViewLayout.format = PixelFormat.TRANSPARENT;
        checkStatusBarView = new View(context); //View helperWnd;
        SystemUtils.addView(windowManager, checkStatusBarView, checkStatusBarViewLayout);
    }


    /**
     * 销毁检查状态栏的view
     */
    public void destroy() {
        SystemUtils.removeView(windowManager, checkStatusBarView);
        checkStatusBarView = null;
    }

    /**
     * 检查状态栏是否存在
     *
     * @return 是否存在状态栏
     */
    public boolean checkStatusBarExist() {
        if (checkStatusBarView == null) {
            return false;
        }
        windowManager.getDefaultDisplay().getRealMetrics(screenMetrics);
        Log.d(TAG, "checkStatusBarExist: view height:" + checkStatusBarView.getHeight());
        return !(checkStatusBarView.getHeight() == screenMetrics.heightPixels);
    }
}

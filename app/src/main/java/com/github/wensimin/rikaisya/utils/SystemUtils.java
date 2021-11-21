package com.github.wensimin.rikaisya.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.service.SwitchTile;

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


    /**
     * 将服务切换成前台服务
     * 同时显示通知与点击关闭功能
     *
     * @param service         需要切换的服务
     * @param id              服务id
     * @param notification    需要显示的通知
     * @param switchTileClass 对应的switchTile class
     */
    public static void switchToForeground(Service service, int id, Notification notification, Class<? extends SwitchTile> switchTileClass) {
        NotificationManager mNotificationManager =
                (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent stopTile = new Intent(service, switchTileClass).setFlags(SwitchTile.STOP_FLAG);
        PendingIntent pendingIntent =
                PendingIntent.getService(service, 0, stopTile, 0);
        NotificationChannel channel = new NotificationChannel(service.getString(R.string.foreNotificationChannelId),
                service.getString(R.string.foreNotificationChannelName),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(service.getString(R.string.foreNotificationChannelDesc));
        mNotificationManager.createNotificationChannel(channel);
        notification.contentIntent = pendingIntent;
        service.startForeground(id, notification);
    }


}

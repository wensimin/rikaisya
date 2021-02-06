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
import com.github.wensimin.rikaisya.activity.MainActivity;

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

    public static int getNavigationBarHeight(WindowManager windowManager) {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        // 使用高度差求导航栏高度
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

    /**
     * 将服务切换成前台服务
     * @param service service
     * @param id 前台服务id
     */
    public static void switchToForeground(Service service, int id) {
        NotificationManager mNotificationManager =
                (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(service, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(service, 0, notificationIntent, 0);
        NotificationChannel channel = new NotificationChannel(service.getString(R.string.foreNotificationChannelId),
                service.getString(R.string.foreNotificationChannelName),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(service.getString(R.string.foreNotificationChannelDesc));
        mNotificationManager.createNotificationChannel(channel);
        Notification notification = new Notification.Builder(service, service.getString(R.string.foreNotificationChannelId))
                .setContentIntent(pendingIntent)
                .build();
        service.startForeground(id, notification);
    }
}

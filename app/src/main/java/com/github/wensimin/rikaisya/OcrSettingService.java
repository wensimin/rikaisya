package com.github.wensimin.rikaisya;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * ocr 快速设置服务
 */
public class OcrSettingService extends TileService {
    private View floatView;
    private WindowManager.LayoutParams layoutParams;

    @Override
    public IBinder onBind(Intent intent) {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 设置LayoutParam
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // TODO 初始显示居中，持久化用户拖动过的位置
        layoutParams.x = 300;
        layoutParams.y = 500;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        floatView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.ocr_btn, new FrameLayout(getApplicationContext()), false);
        return super.onBind(intent);
    }

    @Override
    public void onTileAdded() {
        // TODO 进行是否账号设置完成，未完成则使用未启用状态
        super.onTileAdded();
        Tile tile = getQsTile();
        tile.setState(Tile.STATE_INACTIVE);
        refresh();
    }

    @Override
    public void onClick() {
        super.onClick();
        // 关闭状态栏
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        getApplicationContext().sendBroadcast(it);
        Tile tile = getQsTile();
        // 反转状态
        int state = tile.getState() == Tile.STATE_ACTIVE ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE;
        tile.setState(state);
        refresh();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        this.refresh();
    }


    @Override
    public void onStopListening() {
        super.onStopListening();
        this.refresh();
    }

    private void refresh() {
        Tile tile = getQsTile();
        tile.updateTile();
        int state = tile.getState();
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (state == Tile.STATE_ACTIVE) {
            if (floatView.getWindowToken() == null) {
                windowManager.addView(floatView, layoutParams);
            }
        } else if (state == Tile.STATE_INACTIVE) {
            if (floatView.getWindowToken() != null) {
                windowManager.removeView(floatView);
            }
        }
    }
}
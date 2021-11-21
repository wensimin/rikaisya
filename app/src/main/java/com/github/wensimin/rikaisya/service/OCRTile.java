package com.github.wensimin.rikaisya.service;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * ocr 快速设置服务
 */
public class OCRTile extends TileService {

    public static int STOP_FLAG = -1;

    @Override
    public void onTileAdded() {
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 需要停止且当前tile状态为active时调用click tile使其停止
        if (intent.getFlags() == STOP_FLAG && getQsTile().getState() == Tile.STATE_ACTIVE) {
            onClick();
        }
        return super.onStartCommand(intent, flags, startId);
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
        if (state == Tile.STATE_ACTIVE) {
            startService(new Intent(this, OCRService.class));
        } else if (state == Tile.STATE_INACTIVE) {
            stopService(new Intent(this, OCRService.class));
        }
    }

}

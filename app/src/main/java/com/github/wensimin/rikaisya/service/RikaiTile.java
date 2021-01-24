package com.github.wensimin.rikaisya.service;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * quick setting service
 */
public class RikaiTile extends TileService {

    @Override
    public void onTileAdded() {
        refresh();
    }

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        // 反转状态
        int state = tile.getState() == Tile.STATE_ACTIVE ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE;
        tile.setState(state);
        this.refresh();
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

    /**
     * 刷新按钮状态
     */
    private void refresh() {
        Tile tile = getQsTile();
        // 不使用禁用状态而使用关闭状态
        if (tile.getState() == Tile.STATE_UNAVAILABLE) {
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
        if (tile.getState() == Tile.STATE_ACTIVE) {
            startService(new Intent(this, RikaiFloatingService.class));
        } else {
            stopService(new Intent(this, RikaiFloatingService.class));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, RikaiFloatingService.class));
    }
}

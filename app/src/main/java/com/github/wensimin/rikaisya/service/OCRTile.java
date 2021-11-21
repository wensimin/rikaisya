package com.github.wensimin.rikaisya.service;

import android.content.Intent;
import android.service.quicksettings.Tile;

/**
 * ocr 快速设置服务
 */
public class OCRTile extends SwitchTile {


    @Override
    public void refresh(int state) {
        if (state == Tile.STATE_ACTIVE) {
            startService(new Intent(this, OCRService.class));
        } else if (state == Tile.STATE_INACTIVE) {
            stopService(new Intent(this, OCRService.class));
        }
    }
}

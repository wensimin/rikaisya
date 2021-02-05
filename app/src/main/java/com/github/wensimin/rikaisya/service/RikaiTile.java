package com.github.wensimin.rikaisya.service;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.github.wensimin.rikaisya.adapter.RikaiClipChangeListener;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

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
//        openRikaiListener(tile.getState() == Tile.STATE_ACTIVE);
        if (tile.getState() == Tile.STATE_ACTIVE) {
            startService(new Intent(this, RikaiFloatingService.class));
        } else {
            stopService(new Intent(this, RikaiFloatingService.class));
        }
    }

//    private void openRikaiListener(boolean enable) {
//        ClipboardManager clipboardManager = (ClipboardManager)
//                getSystemService(Context.CLIPBOARD_SERVICE);
//        RikaiClipChangeListener rikaiClipChangeListener = new RikaiClipChangeListener(new Handler(Looper.getMainLooper()), getApplicationContext());
//        if (enable) {
//            clipboardManager.addPrimaryClipChangedListener(rikaiClipChangeListener);
//        } else {
//            this.deleteListener(clipboardManager, rikaiClipChangeListener.getClass());
//            clipboardManager.removePrimaryClipChangedListener(rikaiClipChangeListener);
//        }
//    }
//
//    private void deleteListener(ClipboardManager clipboardManager, Class<? extends RikaiClipChangeListener> listenerClass) {
//        try {
//            Field mPrimaryClipChangedListeners = clipboardManager.getClass().getField("mPrimaryClipChangedListeners");
//            Collection listeners = (Collection) mPrimaryClipChangedListeners.get(clipboardManager);
//            if (listeners != null) {
//                listeners.removeIf(e -> e.getClass().equals(listenerClass));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, RikaiFloatingService.class));
    }
}

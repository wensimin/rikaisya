package com.github.wensimin.rikaisya;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class RikaiService extends TileService {
    @Override
    public void onTileAdded() {
        super.onTileAdded();
        getQsTile().setState(Tile.STATE_INACTIVE);
        getQsTile().updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        int state = tile.getState() == Tile.STATE_ACTIVE ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE;
        tile.setState(state);
        tile.updateTile();
        if (state == Tile.STATE_ACTIVE) {
            startService(new Intent(this, FloatingService.class));
        } else {
            stopService(new Intent(this, FloatingService.class));
        }
    }
}

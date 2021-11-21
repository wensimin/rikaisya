package com.github.wensimin.rikaisya.service

import android.content.Intent
import android.service.quicksettings.Tile

/**
 * rikai tile
 */
class RikaiTile : SwitchTile() {

    override fun refresh(state: Int) {
        if (state == Tile.STATE_ACTIVE) {
            startService(Intent(this, RikaiFloatingService::class.java))
        } else if (state == Tile.STATE_INACTIVE) {
            stopService(Intent(this, RikaiFloatingService::class.java))
        }
    }

}
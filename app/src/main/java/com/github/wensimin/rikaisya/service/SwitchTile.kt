package com.github.wensimin.rikaisya.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * switch型tile抽象类,实现了基础的打开关闭图标效果
 */
abstract class SwitchTile : TileService() {
    companion object {
        const val STOP_FLAG = -1
    }

    override fun onTileAdded() {
        super.onTileAdded()
        qsTile.state = Tile.STATE_INACTIVE
        refresh()
    }

    override fun onClick() {
        super.onClick()
        // 关闭状态栏
        applicationContext.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        // 反转状态
        val state = if (qsTile.state == Tile.STATE_ACTIVE) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        qsTile.state = state
        refresh()
    }

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // 需要停止且当前tile状态为active时调用click tile使其停止
        if (intent.flags == STOP_FLAG && qsTile.state == Tile.STATE_ACTIVE) {
            onClick()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onStopListening() {
        super.onStopListening()
        refresh()
    }

    /**
     * 刷新当前图贴状态
     */
    private fun refresh() {
        qsTile.updateTile()
        refresh(qsTile.state)
    }

    /**
     * 图标状态更新时会调用该方法,目前应该只会在开启和关闭两个状态下调用
     */
    abstract fun refresh(state: Int)

}
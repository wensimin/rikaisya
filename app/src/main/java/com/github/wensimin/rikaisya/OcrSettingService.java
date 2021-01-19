package com.github.wensimin.rikaisya;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import static android.content.ContentValues.TAG;

/**
 * ocr 快速设置服务
 */
//TODO 整理代码
public class OcrSettingService extends TileService {
    private View floatView;
    private WindowManager.LayoutParams layoutParams;

    @Override
    public IBinder onBind(Intent intent) {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 设置LayoutParam
        layoutParams = new WindowManager.LayoutParams();
        floatView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.ocr_btn, new FrameLayout(getApplicationContext()), false);
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // 初始显示居中
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        layoutParams.x = point.x / 2 - floatView.getWidth() / 2;
        layoutParams.y = point.y / 2 - floatView.getHeight() / 2 - 25;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        final Handler handler = new Handler();
        // 长按事件
        Runnable longPressEvent = () -> Log.d(TAG, "Long press!");
        // 拖动事件
        floatView.setOnTouchListener(new View.OnTouchListener() {
            private int oldX;
            private int oldY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        oldX = (int) event.getRawX();
                        oldY = (int) event.getRawY();
                        handler.postDelayed(longPressEvent, 1000);
                        break;
                    case MotionEvent.ACTION_UP:
                        boolean isClick = Math.abs(oldX - event.getRawX()) <= 10 && Math.abs(oldY - event.getRawY()) <= 10;
                        handler.removeCallbacks(longPressEvent);
                        if (isClick) {
                            view.performClick();
                            break;
                        }
                    default:
                        Log.d(TAG, "on touch");
                        layoutParams.x = (int) event.getRawX() - floatView.getWidth() / 2;
                        layoutParams.y = (int) event.getRawY() - floatView.getHeight() / 2 - 25;
                        windowManager.updateViewLayout(floatView, layoutParams);
                }
                return true;
            }
        });
        // 单击事件
        floatView.setOnClickListener(view -> {
            Log.d(TAG, "on click");
        });
        floatView.setLongClickable(true);
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
            if (!floatView.isShown()) {
                windowManager.addView(floatView, layoutParams);
            }
        } else if (state == Tile.STATE_INACTIVE) {
            if (floatView.isShown()) {
                windowManager.removeView(floatView);
            }
        }
    }

}

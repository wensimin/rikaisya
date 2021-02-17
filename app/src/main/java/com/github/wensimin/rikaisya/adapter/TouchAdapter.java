package com.github.wensimin.rikaisya.adapter;

import android.view.MotionEvent;

import java.util.ArrayList;

public class TouchAdapter {
    private ArrayList<Point> points = new ArrayList<>(2);
    private TapListener startListener;
    private TapListener clickListener;
    private TapListener moveListener;
    private Orientation orientation;

    public boolean onTouch(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int actionIndex = event.getActionIndex();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // 初始化 点击集合
                points = new ArrayList<>(2);
                points.set(actionIndex, new Point(x, y));
                if (startListener != null) {
                    startListener.call(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
                Point p = points.get(actionIndex);
                boolean isClick = Math.abs(p.startX - x) <= 10 && Math.abs(p.startY - y) <= 10;
                if (isClick && clickListener != null) {
                    clickListener.call(x, y);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (moveListener != null) {
                    moveListener.call(x, y);
                }
                break;
            // 多指状态
            case MotionEvent.ACTION_POINTER_DOWN:
                // 只处理2指以内动作
                if (actionIndex >= 2) {
                    break;
                }
                points.set(actionIndex, new Point(x, y));
                setMultipleTapOrientation();
                break;
        }
        return true;
    }

    /**
     * 设置双指的方向
     */
    private void setMultipleTapOrientation() {
        Point point = points.get(0);
        Point point1 = points.get(1);
        float xAbs = Math.abs(point.startX - point1.startX);
        float yAbs = Math.abs(point.startY - point1.startY);
        orientation = xAbs > yAbs ? Orientation.horizontal : Orientation.vertical;
    }

    public enum Orientation {
        /**
         * 垂直
         */
        vertical,
        /**
         * 平行
         */
        horizontal
    }

    private static class Point {
        float startX;
        float startY;
        float prevX;
        float prevY;

        public Point(float startX, float startY) {
            this.startX = startX;
            this.startY = startY;
            this.prevX = startX;
            this.prevY = startY;
        }
    }

    public interface TapListener {
        void call(float x, float y);
    }


}

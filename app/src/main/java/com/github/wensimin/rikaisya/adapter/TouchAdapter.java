package com.github.wensimin.rikaisya.adapter;

import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;

import static android.content.ContentValues.TAG;

/**
 * touch 事件分发器
 */
public class TouchAdapter {
    private Point pointA;
    private Point pointB;
    private TapListener startListener;
    private TapListener clickListener;
    private TapListener doubleClickListener;
    private MoveListener moveListener;
    private MultipleTapListener multipleTapListener;
    private MultipleMoveListener multipleMoveListener;
    private Orientation orientation;
    private int clickCount = 0;

    public boolean onTouch(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        Log.d(TAG, "onTouch: id " + pointerId);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 初始化 点击集合
                pointA = new Point(pointerId, x, y);
                Log.d(TAG, "onTouch: start");
                if (startListener != null) {
                    startListener.call(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
                this.end(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                if (isMultipleTap()) {
                    multipleMoveAction(pointerId, x, y);
                } else {
                    moveAction(x, y);
                }
                break;
            // 多指状态
            case MotionEvent.ACTION_POINTER_DOWN:
                // 只处理2指以内动作
                if (pointB != null) {
                    break;
                }
                pointB = new Point(pointerId, x, y);
                setMultipleTapOrientation();
                break;
        }
        return true;
    }

    /**
     * 触摸事件结束
     *
     * @param x up事件的x
     * @param y up事件的y
     */
    private void end(float x, float y) {
        if (isMultipleTap()) {
            boolean isClickA = isClick(pointA, pointA.prevX, pointA.prevY);
            boolean isClickB = isClick(pointA, pointA.prevX, pointA.prevY);
            if (isClickA && isClickB) {
                Log.d(TAG, "end: is double click");
                if (multipleTapListener != null) {
                    multipleTapListener.call(pointA.prevX, pointA.prevY, pointB.prevX, pointB.prevY);
                }
            }
            // 释放b point
            pointB = null;
        } else {
            boolean isClick = this.isClick(pointA, x, y);
            if (isClick) {
                Log.d(TAG, "onTouch: click ");
                addClickCount();
                // 在有listener的情况下优先触发双击click 没有将始终只触发单击
                if (doubleClickListener != null && isDoubleClick()) {
                    doubleClickListener.call(x, y);
                } else if (clickListener != null) {
                    clickListener.call(x, y);
                }
            }
        }

    }

    /**
     * 确定某个point 是否为click
     *
     * @param point point
     * @param x     需要判断位置x
     * @param y     需要判断位置y
     * @return boolean
     */
    private boolean isClick(Point point, float x, float y) {
        return Math.abs(point.startX - x) <= 10 && Math.abs(point.startY - y) <= 10;
    }

    private void addClickCount() {
        clickCount++;
        // 归0
        new Handler().postDelayed(() -> clickCount = 0, 1000 / 2);
    }

    private boolean isDoubleClick() {
        return clickCount >= 2;
    }

    private void multipleMoveAction(int id, float x, float y) {
        // 判断当前移动和非移动的point
        Point movePoint = pointA.id == id ? pointA : pointB;
        Point holePoint = pointA.id == id ? pointB : pointA;
        PointPosition position;
        float move;
        switch (orientation) {
            case vertical:
                position = movePoint.prevY > holePoint.prevY ? PointPosition.bottom : PointPosition.top;
                move = y - movePoint.prevY;
                break;
            case horizontal:
                position = movePoint.prevX > holePoint.prevX ? PointPosition.right : PointPosition.left;
                move = x - movePoint.prevY;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + orientation);
        }
        movePoint.prevX = x;
        movePoint.prevY = y;
        if (multipleMoveListener != null) {
            multipleMoveListener.call(position, move);
        }
    }

    /**
     * 单指移动操作
     *
     * @param x 当前x
     * @param y 当前y
     */
    private void moveAction(float x, float y) {
        Point point = pointA;
        float moveX = x - point.prevX;
        float moveY = y - point.prevY;
        point.prevX = x;
        point.prevY = y;
        if (moveListener != null) {
            moveListener.call(moveX, moveY, x, y);
        }
    }

    private boolean isMultipleTap() {
        return pointB != null;
    }

    /**
     * 设置双指的方向
     */
    private void setMultipleTapOrientation() {
        float xAbs = Math.abs(pointA.startX - pointB.startX);
        float yAbs = Math.abs(pointA.startY - pointB.startY);
        orientation = xAbs > yAbs ? Orientation.horizontal : Orientation.vertical;
    }

    /**
     * 方向枚举
     */
    private enum Orientation {
        /**
         * 垂直
         */
        vertical,
        /**
         * 平行
         */
        horizontal
    }

    /**
     * 移动事件中point的位置
     */
    public enum PointPosition {
        top, bottom, left, right
    }


    /**
     * Point对象
     * 每个点击视为一个point
     */
    private static class Point {
        int id;
        float startX;
        float startY;
        float prevX;
        float prevY;

        public Point(int id, float startX, float startY) {
            this.id = id;
            this.startX = startX;
            this.startY = startY;
            this.prevX = startX;
            this.prevY = startY;
        }
    }

    /**
     * 单击listener
     */
    public interface TapListener {
        void call(float x, float y);
    }

    /**
     * move listener
     */
    public interface MoveListener {
        void call(float moveX, float moveY, float x, float y);
    }


    /**
     * 多点触摸listener
     */
    public interface MultipleTapListener {
        void call(float aX, float aY, float bx, float bY);
    }

    /**
     * 多点move listener
     * 一次只会给定单边的移动方向和move
     */
    public interface MultipleMoveListener {
        void call(PointPosition position, float move);
    }

    public void setStartListener(TapListener startListener) {
        this.startListener = startListener;
    }

    public void setClickListener(TapListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setDoubleClickListener(TapListener doubleClickListener) {
        this.doubleClickListener = doubleClickListener;
    }

    public void setMoveListener(MoveListener moveListener) {
        this.moveListener = moveListener;
    }

    public void setMultipleTapListener(MultipleTapListener multipleTapListener) {
        this.multipleTapListener = multipleTapListener;
    }

    public void setMultipleMoveListener(MultipleMoveListener multipleMoveListener) {
        this.multipleMoveListener = multipleMoveListener;
    }

}

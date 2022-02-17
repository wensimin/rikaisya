package com.github.wensimin.rikaisya.adapter;

import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;

import static android.content.ContentValues.TAG;

/**
 * touch 事件分发器
 */
public class TouchAdapter {
    private Point mainPoint;
    private Point subPoint;
    private TapListener startListener;
    private TapListener clickListener;
    private TapListener doubleClickListener;
    private MoveListener moveListener;
    private MultipleTapListener multipleTapListener;
    private MultipleMoveListener multipleMoveListener;
    private Orientation orientation;
    private int clickCount = 0;
    private boolean multipleMove;

    public boolean onTouch(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.start(pointerId, x, y);
                break;
            case MotionEvent.ACTION_UP:
                this.end(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                int aIndex = event.findPointerIndex(mainPoint.id);
                if (isMultipleTap() && event.getPointerCount() > 1) {
                    multipleMove = true;
                    int bIndex = event.findPointerIndex(subPoint.id);
                    float ax = event.getX(aIndex);
                    float ay = event.getY(aIndex);
                    float bx = event.getX(bIndex);
                    float by = event.getY(bIndex);
                    multipleMoveAction(ax, ay, bx, by);
                } else if (!multipleMove) {
                    moveAction(x, y);
                }
                break;
            // 多指状态
            case MotionEvent.ACTION_POINTER_DOWN:
                // 只处理2指以内动作
                if (subPoint != null) {
                    break;
                }
                subPoint = new Point(pointerId, event.getX(actionIndex), event.getY(actionIndex));
                setMultipleTapOrientation();
                break;
            // 从多指状态恢复单指
            case MotionEvent.ACTION_POINTER_UP:
                // 离开时2指,剩下1指
                if (event.getPointerCount() != 2)
                    break;
                // 判断剩下的手指是哪个
                mainPoint = event.getActionIndex() == 0 ? subPoint : mainPoint;
                // 将现在的手指上次定位作为start定位重新开始
                this.start(mainPoint.id, mainPoint.prevX, mainPoint.prevY);
                subPoint = null;
                break;

        }
        return true;
    }

    /**
     * 事件开始
     *
     * @param pointerId aId
     * @param x         x
     * @param y         y
     */
    private void start(int pointerId, float x, float y) {
        // 初始化 点击集合
        mainPoint = new Point(pointerId, x, y);
        multipleMove = false;
        Log.d(TAG, "onTouch: start");
        if (startListener != null) {
            startListener.call(x, y);
        }
    }

    /**
     * 触摸事件结束
     *
     * @param x up事件的x
     * @param y up事件的y
     */
    private void end(float x, float y) {
        // DOUBLE CLICK 现在已经由于处理双指恢复单指无效了
        if (isMultipleTap()) {
            boolean isClickA = isClick(mainPoint, mainPoint.prevX, mainPoint.prevY);
            boolean isClickB = isClick(mainPoint, mainPoint.prevX, mainPoint.prevY);
            if (!multipleMove && isClickA && isClickB) {
                Log.d(TAG, "end: is double click");
                if (multipleTapListener != null) {
                    multipleTapListener.call(mainPoint.prevX, mainPoint.prevY, subPoint.prevX, subPoint.prevY);
                }
            }
            // 释放b point
            subPoint = null;
        } else {
            boolean isClick = this.isClick(mainPoint, x, y);
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

    private void multipleMoveAction(float ax, float ay, float bx, float by) {
        // 分两次移动
        this.multipleMoveAction(mainPoint, ax, ay);
        this.multipleMoveAction(subPoint, bx, by);
        mainPoint.prevX = ax;
        mainPoint.prevY = ay;
        subPoint.prevX = bx;
        subPoint.prevY = by;

    }

    private void multipleMoveAction(Point movePoint, float x, float y) {
        float move;
        switch (orientation) {
            case vertical:
                move = y - movePoint.prevY;
                break;
            case horizontal:
                move = x - movePoint.prevX;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + orientation);
        }
        if (multipleMoveListener != null) {
            multipleMoveListener.call(movePoint.position, move);
            Log.d(TAG, String.format("multiple move position %s move %s", movePoint.position, move));
        }
    }

    /**
     * 单指移动操作
     *
     * @param x 当前x
     * @param y 当前y
     */
    private void moveAction(float x, float y) {
        Point point = mainPoint;
        float moveX = x - point.prevX;
        float moveY = y - point.prevY;
        point.prevX = x;
        point.prevY = y;
        if (moveListener != null) {
            moveListener.call(moveX, moveY, x, y);
        }
    }

    private boolean isMultipleTap() {
        return subPoint != null;
    }

    /**
     * 设置双指的方向
     */
    private void setMultipleTapOrientation() {
        // 给予x 2倍的数值让体验舒服一点
        float xAbs = Math.abs(mainPoint.startX - subPoint.startX) * 2;
        float yAbs = Math.abs(mainPoint.startY - subPoint.startY);
        orientation = xAbs > yAbs ? Orientation.horizontal : Orientation.vertical;
        switch (orientation) {
            case horizontal:
                mainPoint.position = mainPoint.startX > subPoint.startX ? PointPosition.right : PointPosition.left;
                subPoint.position = mainPoint.position == PointPosition.left ? PointPosition.right : PointPosition.left;
                break;
            case vertical:
                mainPoint.position = mainPoint.startY > subPoint.startY ? PointPosition.bottom : PointPosition.top;
                subPoint.position = mainPoint.position == PointPosition.bottom ? PointPosition.top : PointPosition.bottom;
                break;
        }
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
        PointPosition position;

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
    @Deprecated
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
    @Deprecated
    public void setMultipleTapListener(MultipleTapListener multipleTapListener) {
        this.multipleTapListener = multipleTapListener;
    }

    public void setMultipleMoveListener(MultipleMoveListener multipleMoveListener) {
        this.multipleMoveListener = multipleMoveListener;
    }

}

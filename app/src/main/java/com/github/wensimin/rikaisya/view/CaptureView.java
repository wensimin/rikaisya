package com.github.wensimin.rikaisya.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.utils.SystemUtils;

import static android.content.ContentValues.TAG;

/**
 * 框选截图用view
 */
public class CaptureView extends View {
    private Paint linePaint;
    private static final float DEFAULT_LINE_LENGTH = 300f;
    private static final float MIN_LINE_LENGTH = 100f;
    private static final int DEFAULT_BUTTON_SIZE = 80;

    public static final String CAP_LEFT_KEY = "CAP_LEFT_KEY";
    public static final String CAP_RIGHT_KEY = "CAP_RIGHT_KEY";
    public static final String CAP_TOP_KEY = "CAP_TOP_KEY";
    public static final String CAP_BOTTOM_KEY = "CAP_BOTTOM_KEY";
    private float left = 0f;
    private float right = 0f;
    private float top = 0f;
    private float bottom = 0f;
    private float oldX = 0f;
    private float oldY = 0f;
    private SharedPreferences preferences;
    private boolean isReSize = false;
    private boolean changeLeft = false;
    private boolean changeRight = false;
    private boolean changeTop = false;
    private boolean changeBottom = false;
    private Drawable confirm;
    private Rect confirmRect;
    private Drawable cancel;
    private Rect cancelRect;


    public CaptureView(Context context) {
        super(context);
        init();
    }


    public CaptureView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CaptureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        linePaint = new Paint();
        linePaint.setColor(Color.RED);
        DisplayMetrics point = SystemUtils.getScreenMetrics(getContext());
        int width = point.widthPixels;
        int height = point.heightPixels;
        left = preferences.getFloat(CAP_LEFT_KEY, width / 2f - DEFAULT_LINE_LENGTH);
        right = preferences.getFloat(CAP_RIGHT_KEY, width / 2f + DEFAULT_LINE_LENGTH);
        top = preferences.getFloat(CAP_TOP_KEY, height / 2f - DEFAULT_LINE_LENGTH / 2f);
        bottom = preferences.getFloat(CAP_BOTTOM_KEY, height / 2f + DEFAULT_LINE_LENGTH / 2f);
        confirm = AppCompatResources.getDrawable(getContext(), R.drawable.confirm);
        cancel = AppCompatResources.getDrawable(getContext(), R.drawable.cancel);
        confirmRect = new Rect();
        cancelRect = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawLines(getLinePts(left, right, top, bottom), linePaint);
        drawButton(canvas);
        super.onDraw(canvas);
    }

    /**
     * 绘制按钮
     *
     * @param canvas 画布
     */
    private void drawButton(Canvas canvas) {
        int padding = 10;
        // 默认按钮在上的情况
        int confirmRight = (int) (right - padding);
        int confirmLeft = confirmRight - DEFAULT_BUTTON_SIZE;
        int cancelRight = confirmLeft - padding;
        int cancelLeft = cancelRight - DEFAULT_BUTTON_SIZE;
        int buttonBottom = (int) (top - padding);
        int buttonTop = buttonBottom - DEFAULT_BUTTON_SIZE;
        // 按钮应该下的情况
        if (buttonTop <= 0) {
            buttonTop = (int) (bottom + padding);
            buttonBottom = buttonTop + DEFAULT_BUTTON_SIZE;
        }
        // 按钮已经没有外部位置的情况
        if (buttonBottom >= getHeight()) {
            buttonBottom = (int) (bottom - padding);
            buttonTop = buttonBottom - DEFAULT_BUTTON_SIZE;
        }
        ;
        confirmRect.set(confirmLeft, buttonTop, confirmRight, buttonBottom);
        cancelRect.set(cancelLeft, buttonTop, cancelRight, buttonBottom);
        confirm.setBounds(confirmRect);
        cancel.setBounds(cancelRect);
        confirm.draw(canvas);
        cancel.draw(canvas);
    }

    private float[] getLinePts(float left, float right, float top, float bottom) {
        return new float[]{
                left, top, right, top,
                left, top, left, bottom,
                left, bottom, right, bottom,
                right, top, right, bottom
        };
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                oldX = event.getX();
                oldY = event.getY();
                this.isReSize = isTouchAngle(oldX, oldY);
                Log.d(TAG, "onTouchEvent: reSize:" + isReSize);
                break;
            case MotionEvent.ACTION_UP:
                boolean isClick = Math.abs(oldX - event.getX()) <= 10 && Math.abs(oldY - event.getY()) <= 10;
                if (isClick) {
                    performClick();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "on touch");
                if (isReSize) {
                    touchReSize(x, y);
                } else {
                    touchMove(x, y);
                }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        Log.d(TAG, "performClick: button:" + isClickButton(getX(), getY(), confirmRect));
        return super.performClick();
    }

    /**
     * 拖动矩形大小
     *
     * @param x 当前x
     * @param y 当前y
     */
    private void touchReSize(float x, float y) {
        float moveX = x - oldX;
        float moveY = y - oldY;
        Log.d(TAG, "touchReSize: moveX:" + moveX);
        Log.d(TAG, "touchReSize: moveY:" + moveY);
        float newLeft = left;
        float newRight = right;
        float newTop = top;
        float newBottom = bottom;
        if (changeLeft) {
            newLeft += moveX;
            left = newLeft;
        }
        if (changeRight) {
            newRight += moveX;
            right = newRight;
        }
        oldX = x;
        if (changeTop) {
            newTop += moveY;
            top = newTop;
        }
        if (changeBottom) {
            newBottom += moveY;
            bottom = newBottom;
        }
        oldY = y;
        if (Math.abs(newLeft - newRight) <= MIN_LINE_LENGTH || Math.abs(newTop - newBottom) <= MIN_LINE_LENGTH) {
            return;
        }
        this.invalidate();
        this.savePos();
    }

    /**
     * 拖拽移动
     *
     * @param x 当前x
     * @param y 当前y
     */
    private void touchMove(float x, float y) {
        float moveX = x - oldX;
        float moveY = y - oldY;
        if (!checkXOver(moveX)) {
            left += moveX;
            right += moveX;
            oldX = x;
        }
        if (!checkYOver(moveY)) {
            top += moveY;
            bottom += moveY;
            oldY = y;
        }
        this.invalidate();
        this.savePos();
    }

    /**
     * 持久化位置
     */
    private void savePos() {
        SharedPreferences.Editor edit = preferences.edit();
        edit.putFloat(CAP_LEFT_KEY, left);
        Log.d(TAG, "savePos: left:" + left);
        edit.putFloat(CAP_RIGHT_KEY, right);
        Log.d(TAG, "savePos: right:" + right);
        edit.putFloat(CAP_TOP_KEY, top);
        Log.d(TAG, "savePos: top:" + top);
        edit.putFloat(CAP_BOTTOM_KEY, bottom);
        Log.d(TAG, "savePos: bottom:" + bottom);
        // TODO button pos
        edit.apply();
    }

    /**
     * 检查是否拖动直角
     *
     * @return boolean
     */
    private boolean isTouchAngle(float x, float y) {
        boolean inLeft = inLinePoint(left, x);
        boolean inRight = inLinePoint(right, x);
        boolean inTop = inLinePoint(top, y);
        boolean inBottom = inLinePoint(bottom, y);
        boolean isTouchAngle = (inLeft && (inTop || inBottom)) || (inRight && (inTop || inBottom));
        if (isTouchAngle) {
            this.changeLeft = inLeft;
            this.changeBottom = inBottom;
            this.changeRight = inRight;
            this.changeTop = inTop;
        }
        return isTouchAngle;
    }

    /**
     * 检查是否在一个线上坐标上
     *
     * @param point       需要检查的点
     * @param targetPoint 被检查的位置
     * @return boolean
     */
    private boolean inLinePoint(float point, float targetPoint) {
        float fixValue = 20f;
        return point <= targetPoint + fixValue && point >= targetPoint - fixValue;
    }

    /**
     * 检查是否在同个坐标
     *
     * @param x       x
     * @param y       y
     * @param targetX 目标x
     * @param targetY 目标y
     * @return boolean
     */
    private boolean inPoint(float x, float y, float targetX, float targetY) {
        return inLinePoint(x, targetX) && inLinePoint(y, targetY);
    }

    /**
     * 检查是否点击了按钮的坐标
     *
     * @param x      x
     * @param y      y
     * @param button 按钮
     * @return boolean
     */
    private boolean isClickButton(float x, float y, Rect button) {
        return inPoint(x, y, button.left, button.top) &&
                inPoint(x, y, button.left, button.bottom) &&
                inPoint(x, y, button.right, button.top) &&
                inPoint(x, y, button.right, button.bottom);
    }

    private boolean checkXOver(float moveX) {
        return left + moveX <= 0 || right + moveX >= getWidth();
    }

    private boolean checkYOver(float moveY) {
        return top + moveY <= 0 || bottom + moveY >= getHeight();
    }

}

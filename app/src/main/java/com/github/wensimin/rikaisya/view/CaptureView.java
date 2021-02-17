package com.github.wensimin.rikaisya.view;

import android.annotation.SuppressLint;
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
import com.github.wensimin.rikaisya.adapter.TouchAdapter;
import com.github.wensimin.rikaisya.utils.SystemUtils;

import static android.content.ContentValues.TAG;

/**
 * 框选截图用view
 */
public class CaptureView extends View {
    private Paint linePaint;
    private static final float DEFAULT_LINE_LENGTH = 300f;
    private static final float MIN_LINE_LENGTH = 100f;
    private static final int DEFAULT_BUTTON_SIZE = 120;
    private static final int PADDING = 1;
    public static final String CAP_LEFT_KEY = "CAP_LEFT_KEY";
    public static final String CAP_RIGHT_KEY = "CAP_RIGHT_KEY";
    public static final String CAP_TOP_KEY = "CAP_TOP_KEY";
    public static final String CAP_BOTTOM_KEY = "CAP_BOTTOM_KEY";
    private float left = 0f;
    private float right = 0f;
    private float top = 0f;
    private float bottom = 0f;
    private boolean isReSize = false;
    private boolean changeLeft = false;
    private boolean changeRight = false;
    private boolean changeTop = false;
    private boolean changeBottom = false;
    private SharedPreferences preferences;
    private Drawable confirm;
    private Rect confirmRect;
    private Drawable cancel;
    private Rect cancelRect;
    private ResListener listener;
    /**
     * 触摸事件适配器
     */
    private TouchAdapter touchAdapter;

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
        initTouch();
    }

    /**
     * touch事件初始化
     */
    private void initTouch() {
        touchAdapter = new TouchAdapter();
        // 点击事件开始 记录是否click了边角
        touchAdapter.setStartListener((x, y) -> this.isReSize = isTouchAngle(x, y));
        // 单击确认与取消按钮事件
        touchAdapter.setClickListener((x, y) -> {
            if (isClickButton(x, y, confirmRect)) {
                this.confirm();
            }
            if (isClickButton(x, y, cancelRect)) {
                this.cancel();
            }
        });
        // 双击事件 确认操作
        touchAdapter.setDoubleClickListener((x, y) -> confirm());
        // 多指单击事件 取消操作
        touchAdapter.setMultipleTapListener((aX, aY, bx, bY) -> cancel());
        // move事件 根据初始不同的click位置进行move or resize
        touchAdapter.setMoveListener(((moveX, moveY, x, y) -> {
            if (isReSize) {
                touchReSize(x, y);
            } else {
                touchMove(moveX, moveY);
            }
        }));
        // 双指move resize
        touchAdapter.setMultipleMoveListener(this::multipleTapResize);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        checkLine();
        canvas.drawLines(getLinePts(left, right, top, bottom), linePaint);
        drawButton(canvas);
        super.onDraw(canvas);
    }

    /**
     * 绘制线条时检查溢出
     */
    private void checkLine() {
        int width = getWidth();
        int height = getHeight();
        boolean reset = left < 0 || right > width || top < 0 || bottom > height;
        //设置初始值
        if (reset) {
            left = width / 2f - DEFAULT_LINE_LENGTH;
            right = width / 2f + DEFAULT_LINE_LENGTH;
            top = height / 2f - DEFAULT_LINE_LENGTH / 2f;
            bottom = height / 2f + DEFAULT_LINE_LENGTH / 2f;
        }
    }

    /**
     * 绘制按钮
     *
     * @param canvas 画布
     */
    private void drawButton(Canvas canvas) {
        int padding = 10;
        // 默认按钮在下的情况
        int confirmRight = (int) (right - padding);
        int confirmLeft = confirmRight - DEFAULT_BUTTON_SIZE;
        int cancelRight = confirmLeft - padding;
        int cancelLeft = cancelRight - DEFAULT_BUTTON_SIZE;
        int buttonTop = (int) (bottom + padding);
        int buttonBottom = buttonTop + DEFAULT_BUTTON_SIZE;
        // 按钮应该上的情况
        if (buttonBottom >= getHeight()) {
            buttonBottom = (int) (top - padding);
            buttonTop = buttonBottom - DEFAULT_BUTTON_SIZE;
        }
        // 按钮已经没有外部位置的情况
        if (buttonTop <= 0) {
            buttonBottom = (int) (bottom - padding);
            buttonTop = buttonBottom - DEFAULT_BUTTON_SIZE;
        }
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return touchAdapter.onTouch(event);
    }


    /**
     * 确定操作
     */
    private void confirm() {
        this.savePos();
        this.listener.confirm();
    }

    /**
     * 取消操作
     */
    private void cancel() {
        this.listener.cancel();
    }

    /**
     * 拖动矩形大小
     *
     * @param x 当前x
     * @param y 当前y
     */
    private void touchReSize(float x, float y) {
        x = x < PADDING ? PADDING : x;
        x = x > getWidth() - PADDING ? getWidth() - PADDING : x;
        y = y < PADDING ? PADDING : y;
        y = y > getHeight() - PADDING ? getHeight() - PADDING : y;
        Log.d(TAG, "touchReSize: new x:" + x);
        Log.d(TAG, "touchReSize: new y:" + y);
        if (changeLeft)
            left = x;
        if (changeRight)
            right = x;
        if (changeTop)
            top = y;
        if (changeBottom)
            bottom = y;
        float overWidth = right - left - MIN_LINE_LENGTH;
        if (overWidth <= 0) {
            if (changeLeft)
                left += overWidth;
            if (changeRight)
                right -= overWidth;
        }
        float overHeight = bottom - top - MIN_LINE_LENGTH;
        if (overHeight <= 0) {
            if (changeTop)
                top += overHeight;
            if (changeBottom)
                bottom -= overHeight;
        }
        this.invalidate();
    }

    /**
     * 双指移动 resize
     *
     * @param position 需要移动的边
     * @param move     需要移动的量
     */
    private void multipleTapResize(TouchAdapter.PointPosition position, float move) {
        switch (position) {
            case top:
                top += move;
                top = fixValue(top, PADDING, bottom - MIN_LINE_LENGTH);
                break;
            case left:
                left += move;
                left = fixValue(left, PADDING, right - MIN_LINE_LENGTH);
                break;
            case right:
                right += move;
                right = fixValue(right, left + MIN_LINE_LENGTH, getWidth() - PADDING);
                break;
            case bottom:
                bottom += move;
                bottom = fixValue(bottom, top + MIN_LINE_LENGTH, getHeight() - PADDING);
                break;
        }
        this.invalidate();
    }

    private float fixValue(float value, float min, float max) {
        return Math.max(Math.min(value, max), min);
    }

    /**
     * 拖拽移动
     *
     * @param moveX 移动x
     * @param moveY 移动y
     */
    private void touchMove(float moveX, float moveY) {
        left += moveX;
        right += moveX;
        top += moveY;
        bottom += moveY;
        fixOver();
        this.invalidate();
    }

    /**
     * 持久化位置
     */
    private void savePos() {
        SharedPreferences.Editor edit = preferences.edit();
        edit.putFloat(CAP_LEFT_KEY, left);
        edit.putFloat(CAP_RIGHT_KEY, right);
        edit.putFloat(CAP_TOP_KEY, top);
        edit.putFloat(CAP_BOTTOM_KEY, bottom);
        Log.d(TAG, String.format("savePos: left:%s,right:%s,top:%s,bottom:%s", left, right, top, bottom));
        edit.apply();
    }

    /**
     * 获取当前捕获的rect
     *
     * @param preferences 首选项
     * @return rect
     */
    public static Rect getCaptureRect(SharedPreferences preferences) {
        Rect rect = new Rect();
        rect.left = (int) preferences.getFloat(CAP_LEFT_KEY, 0);
        rect.right = (int) preferences.getFloat(CAP_RIGHT_KEY, 0);
        rect.top = (int) preferences.getFloat(CAP_TOP_KEY, 0);
        rect.bottom = (int) preferences.getFloat(CAP_BOTTOM_KEY, 0);
        return rect;
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
        float fixValue = 30f;
        return point <= targetPoint + fixValue && point >= targetPoint - fixValue;
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
        return x >= button.left && x <= button.right && y >= button.top && y <= button.bottom;
    }


    /**
     * 修正溢出部分
     */
    private void fixOver() {
        float leftOver = PADDING - left;
        if (leftOver >= 0) {
            moveX(leftOver);
        }
        float rightOver = (getWidth() - PADDING) - right;
        if (rightOver <= 0) {
            moveX(rightOver);
        }
        float topOver = PADDING - top;
        if (topOver >= 0) {
            moveY(topOver);
        }
        float bottomOver = (getHeight() - PADDING) - bottom;
        if (bottomOver <= 0) {
            moveY(bottomOver);
        }
    }

    private void moveY(float y) {
        top += y;
        bottom += y;
    }

    private void moveX(float x) {
        left += x;
        right += x;
    }

    public void setListener(ResListener listener) {
        this.listener = listener;
    }

    public interface ResListener {
        void confirm();

        void cancel();
    }

}

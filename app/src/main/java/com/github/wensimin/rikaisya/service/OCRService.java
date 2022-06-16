package com.github.wensimin.rikaisya.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.activity.ScreenActivity;
import com.github.wensimin.rikaisya.utils.OCRUtils;
import com.github.wensimin.rikaisya.utils.PerformanceTimer;
import com.github.wensimin.rikaisya.utils.ScreenshotPermissionUtils;
import com.github.wensimin.rikaisya.utils.SystemUtils;
import com.github.wensimin.rikaisya.view.CaptureView;

import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;
import static com.github.wensimin.rikaisya.service.OCRResultViewManager.ACCURATE_SWITCH_STATUS;

/**
 * ocr service
 */
public class OCRService extends Service {
    private static final int FOREGROUND_ID = 1;
    private OCRFloatViewManager ocrFloatViewManager;
    private Rect screenRect;
    private MediaProjectionManager mediaProjectionManager;
    private Rect rect;
    private ImageReader imageReader;
    private Surface surface;
    private boolean isCaptured = false;
    private SharedPreferences preferences;
    private OCRResultViewManager ocrResultViewManager;
    private CheckStatusBarViewManager checkStatusBarViewManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        // 切换到前台服务
        switchToForeground();
        ocrFloatViewManager = new OCRFloatViewManager(getApplicationContext());
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        ocrResultViewManager = new OCRResultViewManager(getApplicationContext());
        checkStatusBarViewManager = new CheckStatusBarViewManager(getApplicationContext());
        requestScreenshotPermission();
        ocrFloatViewManager.showFloatButton(this::startCapture);
    }

    /**
     * 将服务切换成前台服务
     */
    private void switchToForeground() {
        Notification notification = new Notification.Builder(this, this.getString(R.string.foreNotificationChannelId))
                .setContentTitle(getString(R.string.OCRActive))
                .setContentText(getString(R.string.clickStopOCR))
                .setSmallIcon(R.drawable.ic_launcher_round)
                .build();
        SystemUtils.switchToForeground(this, FOREGROUND_ID, notification, OCRTile.class);
    }

    /**
     * 请求截图权限
     */
    private void requestScreenshotPermission() {
        Intent intent = new Intent(this, ScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * 开始截图
     */
    @SuppressLint("WrongConstant")
    private void startCapture() {
        screenRect = SystemUtils.getScreenRect(this);
        imageReader = ImageReader.newInstance(screenRect.width(), screenRect.height(), PixelFormat.RGBA_8888, 1);
        surface = imageReader.getSurface();
        rect = CaptureView.getCaptureRect(PreferenceManager.getDefaultSharedPreferences(getBaseContext()));
        if (checkIsOver(screenRect, rect)) {
            Toast.makeText(this, "区域无效,请重新截取", Toast.LENGTH_SHORT).show();
            return;
        }
        MediaProjection mediaProjection = ScreenshotPermissionUtils.getMediaProjection(mediaProjectionManager);
        if (mediaProjection == null) {
            Toast.makeText(this, "获取截图权限失败,请重启开关并授权", Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(() -> {
            VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay("screen-mirror",
                    screenRect.width(), screenRect.height(), getResources().getDisplayMetrics().densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    surface, null, null);
            isCaptured = false;
            imageReader.setOnImageAvailableListener(reader -> {
                if (isCaptured) {
                    return;
                }
                isCaptured = true;
                Bitmap bitmap = this.getBitmap(imageReader);
                if (bitmap != null) {
                    Log.d(TAG, "get bitmap size:" + bitmap.getByteCount());
                    Log.d(TAG, "bitmap生成 消耗时间:" + PerformanceTimer.cut());
                    OCRUtils.getInstance(
                            preferences.getString(getResources().getString(R.string.baidu_OCR_config_title_API), null),
                            preferences.getString(getResources().getString(R.string.baidu_OCR_config_title_Secret), null)
                    ).readBitmap(bitmap, result -> {
                        Log.d(TAG, "OCR完成 消耗时间:" + PerformanceTimer.cut());
                        if (result.getErrorCode() != OCRUtils.OCRResult.SUCCESS_CODE) {
                            Toast.makeText(getApplicationContext(), "OCR识别失败，请重试或检查配置", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "ocr error" + result.getErrorMsg());
                        } else {
                            String allWords = result.getAllWords();
                            allWords = allWords.trim();
                            // 即使没有扫描到文字,也打开结果窗口
                            if (allWords.isEmpty()) {
                                Toast.makeText(getApplicationContext(), "未识别到文字", Toast.LENGTH_LONG).show();
                            }
                            ocrResultViewManager.setSourceText(allWords);
                            ocrResultViewManager.showOCRResultView();
                            Log.d(TAG, "显示OCR窗口 全部花费时间:" + PerformanceTimer.end());
                        }
                    }, preferences.getBoolean(ACCURATE_SWITCH_STATUS, false));
                }
                virtualDisplay.release();
                mediaProjection.stop();
            }, new Handler(Looper.getMainLooper()));
        }).start();
    }


    private boolean checkIsOver(Rect screenMetrics, Rect rect) {
        return rect.left < 0 || rect.right > screenMetrics.width() || rect.top < 0 || rect.bottom > screenMetrics.height();
    }


    /**
     * 从imageReader 获取bitmap
     *
     * @param imageReader reader
     * @return bitmap
     */
    private Bitmap getBitmap(ImageReader imageReader) {
        Image image = imageReader.acquireNextImage();
        if (image == null) {
            Log.w(TAG, "getBitmap: image is null, retry");
            return getBitmap(imageReader);
        }
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        // 切割
        bitmap = splitBitmap(bitmap);
        buffer.clear();
        image.close();
        return bitmap;
    }


    /**
     * 切指定位置bitMap
     *
     * @param bitmap oldBitmap
     * @return newBitmap
     */
    private Bitmap splitBitmap(Bitmap bitmap) {
        int statusBarHeight = SystemUtils.getStatusBarHeight(getResources());
        // check statusBar exist
        if (!checkStatusBarViewManager.checkStatusBarExist()) {
            statusBarHeight = 0;
        }
        return Bitmap.createBitmap(bitmap, rect.left, rect.top + statusBarHeight, rect.right - rect.left, rect.bottom - rect.top);
    }


    @Override
    public void onDestroy() {
        if (surface != null) {
            surface.release();
        }
        if (imageReader != null) {
            imageReader.close();
        }
        ocrResultViewManager.destroy();
        checkStatusBarViewManager.destroy();
        ocrFloatViewManager.destroy();
        super.onDestroy();
    }

}

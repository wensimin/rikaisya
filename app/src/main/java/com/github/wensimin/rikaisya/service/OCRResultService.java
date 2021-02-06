package com.github.wensimin.rikaisya.service;

import android.annotation.SuppressLint;
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
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.utils.OCRUtils;
import com.github.wensimin.rikaisya.utils.PerformanceTimer;
import com.github.wensimin.rikaisya.utils.SystemUtils;
import com.github.wensimin.rikaisya.view.CaptureView;

import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;
import static com.github.wensimin.rikaisya.service.OCRResultViewManager.ACCURATE_SWITCH_STATUS;

/**
 * 截图service
 * 进行截图&后续ocr
 * TODO 性能优化
 */
public class OCRResultService extends Service {
    public static final String EXTRA_RESULT_INTENT = "EXTRA_RESULT_INTENT";
    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    private static final int FOREGROUND_ID = 2;

    private DisplayMetrics screenMetrics;
    private MediaProjectionManager mediaProjectionManager;
    private WindowManager windowManager;
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


    @SuppressLint("WrongConstant")
    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        screenMetrics = new DisplayMetrics();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // 切换到前台服务
//        SystemUtils.switchToForeground(this, FOREGROUND_ID);
        ocrResultViewManager = new OCRResultViewManager(getApplicationContext());
        checkStatusBarViewManager = new CheckStatusBarViewManager(getApplicationContext());
    }


    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        windowManager.getDefaultDisplay().getRealMetrics(screenMetrics);
        imageReader = ImageReader.newInstance(screenMetrics.widthPixels, screenMetrics.heightPixels, PixelFormat.RGBA_8888, 1);
        surface = imageReader.getSurface();
        rect = CaptureView.getCaptureRect(PreferenceManager.getDefaultSharedPreferences(getBaseContext()));
        if (checkIsOver(screenMetrics, rect)) {
            Toast.makeText(this, "区域无效,请重新截取", Toast.LENGTH_SHORT).show();
            return super.onStartCommand(intent, flags, startId);
        }
        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent parcelableExtra = intent.getParcelableExtra(EXTRA_RESULT_INTENT);
        new Thread(() -> {
            MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, parcelableExtra);
            VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay("screen-mirror",
                    screenMetrics.widthPixels, screenMetrics.heightPixels, screenMetrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
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
                            //FIXME 测试待删除
                            ocrResultViewManager.setSourceText("错误");
                            ocrResultViewManager.showOCRResultView();
                        } else {
                            ocrResultViewManager.setSourceText(result.getAllWords());
                            ocrResultViewManager.showOCRResultView();
                            Log.d(TAG, "显示OCR窗口 全部花费时间:" + PerformanceTimer.end());
                        }
                    }, preferences.getBoolean(ACCURATE_SWITCH_STATUS, false));
                }
                virtualDisplay.release();
                mediaProjection.stop();
            }, new Handler(Looper.getMainLooper()));
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }


    private boolean checkIsOver(DisplayMetrics screenMetrics, Rect rect) {
        return rect.left < 0 || rect.right > screenMetrics.widthPixels || rect.top < 0 || rect.bottom > screenMetrics.heightPixels;
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
        surface.release();
        imageReader.close();
        ocrResultViewManager.destroy();
        checkStatusBarViewManager.destroy();
        super.onDestroy();
    }
}

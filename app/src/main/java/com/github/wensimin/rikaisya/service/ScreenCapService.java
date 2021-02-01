package com.github.wensimin.rikaisya.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.utils.SystemUtils;
import com.github.wensimin.rikaisya.view.CaptureView;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

public class ScreenCapService extends Service {
    public static final String EXTRA_RESULT_INTENT = "EXTRA_RESULT_INTENT";
    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";

    private ImageReader imageReader;
    private DisplayMetrics screenMetrics;
    private MediaProjectionManager mediaProjectionManager;
    private WindowManager windowManager;
    private Rect rect;
    /**
     * 用于检查状态栏是否存在的view
     */
    private View checkStatusBarView;

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
        initCheckStatusBarView();
        screenMetrics = new DisplayMetrics();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        DisplayMetrics screenMetrics = SystemUtils.getScreenMetrics(getApplicationContext());
        imageReader = ImageReader.newInstance(screenMetrics.widthPixels, screenMetrics.heightPixels, PixelFormat.RGBA_8888, 1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        windowManager.getDefaultDisplay().getRealMetrics(screenMetrics);
        rect = CaptureView.getCaptureRect(PreferenceManager.getDefaultSharedPreferences(getBaseContext()));
        if (checkIsOver(screenMetrics, rect)) {
            Toast.makeText(this, "区域无效,请重新截取", Toast.LENGTH_SHORT).show();
            return super.onStartCommand(intent, flags, startId);
        }
        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent parcelableExtra = intent.getParcelableExtra(EXTRA_RESULT_INTENT);
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, parcelableExtra);
        mediaProjection.createVirtualDisplay("screen-mirror",
                screenMetrics.widthPixels, screenMetrics.heightPixels, screenMetrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Bitmap bitmap = this.getBitmap(imageReader);
            if (bitmap != null) {
                this.writeFile(bitmap);
            }
            mediaProjection.stop();
        }, 0);
        return super.onStartCommand(intent, flags, startId);
    }

    private boolean checkIsOver(DisplayMetrics screenMetrics, Rect rect) {
        return rect.left < 0 || rect.right > screenMetrics.widthPixels || rect.top < 0 || rect.bottom > screenMetrics.heightPixels;
    }

    private void writeFile(Bitmap bitmap) {
        try {
//                    File fileImage = new File(getCacheDir().getPath() + "/test.png");
            // TODO delete and switch private storage
            File fileImage = new File(Environment.getExternalStorageDirectory().getPath() + "/Pictures/test.png");
            Log.d(TAG, "filePath:" + fileImage.getPath());
            if (!fileImage.exists()) {
                boolean newFile = fileImage.createNewFile();
                if (!newFile) {
                    Log.e(TAG, "cap image: create file err");
                }
            }
            FileOutputStream out = new FileOutputStream(fileImage, false);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            Log.d(TAG, "writeFile: end");
            Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(fileImage);
            media.setData(contentUri);
            this.sendBroadcast(media);
        } catch (Exception e) {
            Log.e(TAG, "onActivityResult: " + e.getLocalizedMessage());
        }
    }

    /**
     * 从imageReader 获取bitmap
     * @param imageReader reader
     * @return bitmap
     * fixme image nullPoint & bitmap dataEmpty
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
        if (!checkStatusBarExist()) {
            statusBarHeight = 0;
        }
        return Bitmap.createBitmap(bitmap, rect.left, rect.top + statusBarHeight, rect.right - rect.left, rect.bottom - rect.top);
    }

    /**
     * 初始化检查状态栏的view
     */
    private void initCheckStatusBarView() {
        WindowManager.LayoutParams checkStatusBarViewLayout = new WindowManager.LayoutParams();
        checkStatusBarViewLayout.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        checkStatusBarViewLayout.gravity = Gravity.END | Gravity.TOP;
        checkStatusBarViewLayout.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        checkStatusBarViewLayout.width = 1;
        checkStatusBarViewLayout.height = WindowManager.LayoutParams.MATCH_PARENT;
        checkStatusBarViewLayout.format = PixelFormat.TRANSPARENT;
        checkStatusBarView = new View(this); //View helperWnd;
        SystemUtils.addView(windowManager, checkStatusBarView, checkStatusBarViewLayout);
    }

    /**
     * 销毁检查状态栏的view
     */
    private void destroyCheckStatusBarView() {
        SystemUtils.removeView(windowManager, checkStatusBarView);
        checkStatusBarView = null;
    }

    /**
     * 检查状态栏是否存在
     *
     * @return 是否存在状态栏
     */
    public boolean checkStatusBarExist() {
        if (checkStatusBarView == null) {
            return false;
        }
        Log.d(TAG, "checkStatusBarExist: view height:" + checkStatusBarView.getHeight());
        return !(checkStatusBarView.getHeight() == screenMetrics.heightPixels);
    }


    @Override
    public void onDestroy() {
        imageReader.getSurface().release();
        destroyCheckStatusBarView();
        super.onDestroy();
    }
}

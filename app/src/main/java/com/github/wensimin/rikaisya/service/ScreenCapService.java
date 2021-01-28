package com.github.wensimin.rikaisya.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.utils.SystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

public class ScreenCapService extends Service {
    public static final String EXTRA_RESULT_INTENT = "EXTRA_RESULT_INTENT";
    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent parcelableExtra = intent.getParcelableExtra(EXTRA_RESULT_INTENT);
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, parcelableExtra);
        DisplayMetrics screenMetrics = SystemUtils.getScreenMetrics(getApplicationContext());
        @SuppressLint("WrongConstant") ImageReader imageReader = ImageReader.newInstance(screenMetrics.widthPixels, screenMetrics.heightPixels, 0x1, 2);
        mediaProjection.createVirtualDisplay("screen-mirror",
                screenMetrics.widthPixels, screenMetrics.heightPixels, screenMetrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);
        // FIXME 同步操作
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            image.close();
            if (bitmap != null) {
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
                    FileOutputStream out = new FileOutputStream(fileImage);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();
                    Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri contentUri = Uri.fromFile(fileImage);
                    media.setData(contentUri);
                    this.sendBroadcast(media);
                } catch (Exception e) {
                    Log.e(TAG, "onActivityResult: " + e.getLocalizedMessage());
                }
            }
            mediaProjection.stop();
        }, null);
        return super.onStartCommand(intent, flags, startId);
    }
}

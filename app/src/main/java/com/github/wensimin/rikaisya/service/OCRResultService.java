package com.github.wensimin.rikaisya.service;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.utils.OCRUtils;
import com.github.wensimin.rikaisya.utils.PerformanceTimer;
import com.github.wensimin.rikaisya.utils.SystemUtils;
import com.github.wensimin.rikaisya.utils.TransitionUtils;
import com.github.wensimin.rikaisya.view.CaptureView;

import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

/**
 * 截图service
 * 进行截图&后续ocr
 * TODO 性能优化
 */
public class OCRResultService extends Service {
    public static final String EXTRA_RESULT_INTENT = "EXTRA_RESULT_INTENT";
    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    private static final String TRANSITION_SWITCH_STATUS = "TRANSITION_SWITCH_STATUS";


    private DisplayMetrics screenMetrics;
    private MediaProjectionManager mediaProjectionManager;
    private WindowManager windowManager;
    private Rect rect;
    private ImageReader imageReader;
    private Surface surface;
    /**
     * 用于检查状态栏是否存在的view
     */
    private View checkStatusBarView;
    private boolean isCaptured = false;
    private ClipboardManager clipboardManager;
    private SharedPreferences preferences;

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
        clipboardManager = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

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
                    } else {
                        this.openOCRResultView(result.getAllWords());
                    }
                });
            }
            virtualDisplay.release();
            mediaProjection.stop();
        }, new Handler(Looper.getMainLooper()));
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 打开OCR结果窗口
     */
    @SuppressLint("ClickableViewAccessibility")
    private void openOCRResultView(String OCRResult) {
        FrameLayout layout = (FrameLayout) LayoutInflater.from(getApplicationContext()).inflate(R.layout.ocr_result_view, new FrameLayout(getApplicationContext()), true);
        initOCRResultView(layout, OCRResult);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        SystemUtils.addView(windowManager, layout, layoutParams);
        Log.d(TAG, "显示view 消耗时间:" + PerformanceTimer.cut());
        Log.d(TAG, "全部花费时间:" + PerformanceTimer.end());

    }

    /**
     * 初始化 OCR结果view
     *
     * @param layout    layout
     * @param OCRResult ocr结果
     */
    private void initOCRResultView(FrameLayout layout, String OCRResult) {
        // OCR结果text
        TextView sourceText = layout.findViewById(R.id.sourceText);
        sourceText.setText(OCRResult);
        sourceText.setMovementMethod(ScrollingMovementMethod.getInstance());
        TextView resultText = layout.findViewById(R.id.resultText);
        resultText.setMovementMethod(ScrollingMovementMethod.getInstance());
        // 自动翻译按钮
        this.initTransitionSwitch(layout, resultText, sourceText);
        // 取消按钮
        Button cancelButton = layout.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> SystemUtils.removeView(windowManager, layout));
        View.OnLongClickListener copyListener = v -> {
            TextView view = (TextView) v;
            Log.d(TAG, "copy:" + view.getText());
            Toast.makeText(getApplication(), "copy:" + view.getText(), Toast.LENGTH_LONG).show();
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, view.getText()));
            return true;
        };
        resultText.setOnLongClickListener(copyListener);
        sourceText.setOnLongClickListener(copyListener);
        Button editButton = layout.findViewById(R.id.editButton);
        editButton.setOnClickListener(v -> createEditDialog(sourceText, resultText));
    }

    /**
     * 初始化 自动翻译开关
     *
     * @param layout     布局
     * @param resultText 结果text
     * @param sourceText 原文text
     */
    private void initTransitionSwitch(FrameLayout layout, TextView resultText, TextView sourceText) {
        // 自动翻译开关
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch transitionSwitch = layout.findViewById(R.id.transitionSwitch);
        boolean switchStatus = preferences.getBoolean(TRANSITION_SWITCH_STATUS, false);
        transitionSwitch.setChecked(switchStatus);
        transitionSwitch.setOnCheckedChangeListener((v, checked) -> {
            preferences.edit().putBoolean(TRANSITION_SWITCH_STATUS, checked).apply();
            this.setTransitionStatus(checked, resultText, sourceText);
        });
        this.setTransitionStatus(switchStatus, resultText, sourceText);
    }

    /**
     * 设置自动翻译状态
     *
     * @param enabled    是否启用
     * @param resultText 翻译text
     * @param sourceText 原文text
     */
    private void setTransitionStatus(boolean enabled, TextView resultText, TextView sourceText) {
        if (enabled) {
            this.transition(sourceText.getText(), result -> {
                if (result.isError()) {
                    Log.w(TAG, String.format("error code:%s,error msg: %s", result.getCode(), result.getMessage()));
                    Toast.makeText(getApplicationContext(), "翻译出现问题,请重试或检查配置", Toast.LENGTH_LONG).show();
                } else {
                    resultText.setVisibility(View.VISIBLE);
                    resultText.setText(result.getText());
                }
            });
        } else {
            resultText.setVisibility(View.GONE);
        }
    }

    /**
     * 翻译
     *
     * @param sourceText 输入text
     */
    private void transition(CharSequence sourceText, TransitionUtils.TransitionListener listener) {
        TransitionUtils transitionUtils = TransitionUtils.getInstance(
                preferences.getString(getResources().getString(R.string.tencent_translate_id), null),
                preferences.getString(getResources().getString(R.string.tencent_translate_key), null),
                true);
        transitionUtils.transition(sourceText.toString(), listener);
    }

    /**
     * 创建编辑ocr结果dialog
     *
     * @param sourceText ocr结果text
     * @param resultText 翻译结果text
     */
    private void createEditDialog(TextView sourceText, TextView resultText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle(R.string.editDialogTitle);
        FrameLayout layout = (FrameLayout) LayoutInflater.from(getApplicationContext()).inflate(R.layout.ocr_edit_dialog, new FrameLayout(getApplicationContext()), true);
        TextView ocrEdit = layout.findViewById(R.id.OCREdit);
        ocrEdit.setText(sourceText.getText());
        builder.setView(layout);
        builder.setPositiveButton(R.string.ok, (dialog, id) -> {
            // User clicked OK button
            Log.d(TAG, "ocr dialog ok ");
            sourceText.setText(ocrEdit.getText());
            setTransitionStatus(true, resultText, sourceText);
        });
        builder.setNegativeButton(R.string.cancel, (dialog, id) -> Log.d(TAG, "ocr dialog cancel "));
        AlertDialog alertDialog = builder.create();
        Window window = alertDialog.getWindow();
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        // 进入编辑状态会弹出状态栏等
        // 使用以下flag可以阻止弹出，但是窗口不会自适应
//        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        alertDialog.show();
//        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
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
        surface.release();
        imageReader.close();
        destroyCheckStatusBarView();
        super.onDestroy();
    }
}

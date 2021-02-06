package com.github.wensimin.rikaisya.service;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.utils.SystemUtils;
import com.github.wensimin.rikaisya.utils.TransitionUtils;

import static android.content.ContentValues.TAG;

public class OCRResultViewManager {
    private final Context context;
    private final WindowManager windowManager;
    private final ClipboardManager clipboardManager;
    private final SharedPreferences preferences;
    public static final String TRANSITION_SWITCH_STATUS = "TRANSITION_SWITCH_STATUS";
    public static final String ACCURATE_SWITCH_STATUS = "ACCURATE_SWITCH_STATUS";
    private TextView sourceText;
    private TextView resultText;
    private FrameLayout layout;
    private WindowManager.LayoutParams layoutParams;


    public OCRResultViewManager(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        initOCRResultViewLayout();
    }

    public void showOCRResultView() {
        SystemUtils.addView(windowManager, layout, layoutParams);
    }

    public void destroy() {
        SystemUtils.removeView(windowManager, layout);
    }

    public void setSourceText(String OCRResult) {
        sourceText.setText(OCRResult);
        boolean transitionStatus = preferences.getBoolean(TRANSITION_SWITCH_STATUS, false);
        if (transitionStatus) {
            transition();
        }
    }

    /**
     * 初始化布局
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initOCRResultViewLayout() {
        layout = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.ocr_result_view, new FrameLayout(context), true);
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        initOCRResultView();
    }


    /**
     * 初始化 OCR结果view
     */
    private void initOCRResultView() {
        // OCR结果text
        sourceText = layout.findViewById(R.id.sourceText);
        sourceText.setMovementMethod(ScrollingMovementMethod.getInstance());
        resultText = layout.findViewById(R.id.resultText);
        resultText.setMovementMethod(ScrollingMovementMethod.getInstance());
        // 高精度OCR切换按钮
        this.initAccurateSwitch();
        // 自动翻译按钮
        this.initTransitionSwitch();
        // 取消按钮
        Button cancelButton = layout.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> SystemUtils.removeView(windowManager, layout));
        View.OnLongClickListener copyListener = v -> {
            TextView view = (TextView) v;
            Log.d(TAG, "copy:" + view.getText());
            Toast.makeText(context, "copy:" + view.getText(), Toast.LENGTH_LONG).show();
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, view.getText()));
            return true;
        };
        resultText.setOnLongClickListener(copyListener);
        sourceText.setOnLongClickListener(copyListener);
        Button editButton = layout.findViewById(R.id.editButton);
        editButton.setOnClickListener(v -> createEditDialog());
    }

    /**
     * 初始化高精度ocr按钮
     */
    private void initAccurateSwitch() {
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch accurateSwitch = layout.findViewById(R.id.accurateSwitch);
        boolean accurateStatus = preferences.getBoolean(ACCURATE_SWITCH_STATUS, false);
        accurateSwitch.setChecked(accurateStatus);
        accurateSwitch.setOnCheckedChangeListener((v, checked) -> {
            Toast.makeText(context, "已经修改OCR设置，下次开始生效", Toast.LENGTH_LONG).show();
            preferences.edit().putBoolean(ACCURATE_SWITCH_STATUS, checked).apply();
        });
    }

    /**
     * 初始化 自动翻译开关
     *
     */
    private void initTransitionSwitch() {
        // 自动翻译开关
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch transitionSwitch = layout.findViewById(R.id.transitionSwitch);
        boolean switchStatus = preferences.getBoolean(TRANSITION_SWITCH_STATUS, false);
        transitionSwitch.setChecked(switchStatus);
        transitionSwitch.setOnCheckedChangeListener((v, checked) -> {
            preferences.edit().putBoolean(TRANSITION_SWITCH_STATUS, checked).apply();
            this.setTransitionStatus(checked);
        });
    }

    /**
     * 设置自动翻译状态
     *
     * @param enabled 是否启用
     */
    private void setTransitionStatus(boolean enabled) {
        if (enabled) {
            this.transition();
        } else {
            resultText.setVisibility(View.GONE);
        }
    }

    /**
     * 翻译
     */
    private void transition() {
        TransitionUtils transitionUtils = TransitionUtils.getInstance(
                preferences.getString(context.getResources().getString(R.string.tencent_translate_id), null),
                preferences.getString(context.getResources().getString(R.string.tencent_translate_key), null),
                true);
        transitionUtils.transition(sourceText.toString(), result -> {
            if (result.isError()) {
                Log.w(TAG, String.format("error code:%s,error msg: %s", result.getCode(), result.getMessage()));
                Toast.makeText(context, "翻译出现问题,请重试或检查配置", Toast.LENGTH_LONG).show();
            } else {
                resultText.setVisibility(View.VISIBLE);
                resultText.setText(result.getText());
            }
        });
    }

    /**
     * 创建编辑ocr结果dialog
     */
    private void createEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.editDialogTitle);
        FrameLayout layout = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.ocr_edit_dialog, new FrameLayout(context), true);
        TextView ocrEdit = layout.findViewById(R.id.OCREdit);
        ocrEdit.setText(sourceText.getText());
        builder.setView(layout);
        builder.setPositiveButton(R.string.ok, (dialog, id) -> {
            // User clicked OK button
            Log.d(TAG, "ocr dialog ok ");
            setSourceText(ocrEdit.getText().toString());
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


}

package com.github.wensimin.rikaisya.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.utils.SystemUtils;

public class OCRResultView extends FrameLayout {

    private WindowManager windowManager;

    public OCRResultView(@NonNull Context context) {
        super(context);
        init();
    }

    public OCRResultView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OCRResultView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }



    private void init() {
        windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        EditText sourceText = this.findViewById(R.id.sourceText);
        sourceText.setText("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaanishibushishb愛lz地涌hb魏kwbぁjkpジオ所きみはしゃ");
        sourceText.setEnabled(false);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch transitionSwitch = this.findViewById(R.id.transitionSwitch);
        transitionSwitch.setEnabled(false);
        TextView resultText = this.findViewById(R.id.resultText);
        resultText.setText("111111111111111111111111111111111111111111111111111111111111111111aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        Button cancelButton = this.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            SystemUtils.removeView(windowManager, this);
        });
    }

}

package com.github.wensimin.rikaisya.utils;

import android.os.Handler;
import android.os.Looper;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.tmt.v20180321.TmtClient;
import com.tencentcloudapi.tmt.v20180321.models.TextTranslateRequest;
import com.tencentcloudapi.tmt.v20180321.models.TextTranslateResponse;

public class TranslateUtils {
    private static TranslateUtils instance;
    private final TmtClient tmtClient;

    public static TranslateUtils getInstance(String id, String key, boolean isNew) {
        if (isNew || instance == null) {
            instance = new TranslateUtils(id, key);
        }
        return instance;
    }

    private TranslateUtils(String id, String key) {
        Credential cred = new Credential(id, key);
        tmtClient = new TmtClient(cred, "ap-guangzhou");
    }

    public void transition(String sourceText, TransitionListener listener) {
        TextTranslateRequest textTranslateRequest = new TextTranslateRequest();
        // TODO 分行模式 off 可能考虑改为配置
        sourceText = sourceText.replaceAll("\n", "");
        textTranslateRequest.setSourceText(sourceText);
        textTranslateRequest.setSource("auto");
        //TODO 目标语言大概会修改成配置项
        textTranslateRequest.setTarget("zh");
        textTranslateRequest.setProjectId(0L);
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                TextTranslateResponse textTranslateResponse = tmtClient.TextTranslate(textTranslateRequest);
                handler.postDelayed(() -> listener.callback(new TransitionResult(textTranslateResponse.getTargetText())), 0);
            } catch (TencentCloudSDKException e) {
                e.printStackTrace();
                handler.postDelayed(() -> listener.callback(new TransitionResult(e.getErrorCode(), e.getMessage())), 0);
            }
        }).start();

    }


    public interface TransitionListener {
        void callback(TransitionResult result);
    }

    public static class TransitionResult {
        private final boolean error;
        private String text;
        private String code;
        private String message;

        private TransitionResult(String code, String message) {
            this.code = code;
            this.message = message;
            this.error = true;
        }

        private TransitionResult(String text) {
            this.text = text;
            this.error = false;
        }

        public boolean isError() {
            return error;
        }

        public String getText() {
            return text;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}

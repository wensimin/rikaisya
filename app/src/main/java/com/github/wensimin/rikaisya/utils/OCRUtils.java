package com.github.wensimin.rikaisya.utils;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.baidu.aip.ocr.AipOcr;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import static android.content.ContentValues.TAG;

/**
 * ocr 工具类 单例
 */
public class OCRUtils {
    private final AipOcr aipOcr;
    private static OCRUtils OCRUtils;

    public static OCRUtils getInstance(String baiduApi, String baiduSecret) {
        return getInstance(baiduApi, baiduSecret, false);
    }

    public static OCRUtils getInstance(String baiduApi, String baiduSecret, boolean isNew) {
        if (isNew || OCRUtils == null) {
            OCRUtils = new OCRUtils(baiduApi, baiduSecret);
        }
        return OCRUtils;
    }

    private OCRUtils(String baiduApi, String baiduSecret) {
        aipOcr = new AipOcr(null, baiduApi, baiduSecret);
        aipOcr.setConnectionTimeoutInMillis(2000);
        aipOcr.setSocketTimeoutInMillis(5000);
    }

    public void readBitmap(Bitmap bitmap, OCRListener listener, boolean isAccurate) {
        byte[] bytes = bitmap2Bytes(bitmap, isAccurate);
        Log.d(TAG, "byte size:" + bytes.length);

        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            HashMap<String, String> options = new HashMap<>();
            options.put("detect_direction", "true");
            JSONObject jsonObject;
            Log.d(TAG, "ocr开始 消耗时间:" + PerformanceTimer.cut());
            if (isAccurate) {
                options.put("language_type", "auto_detect");
                jsonObject = aipOcr.basicAccurateGeneral(bytes, options);
            } else {
                options.put("detect_language", "true");
                jsonObject = aipOcr.basicGeneral(bytes, options);
            }
            handler.postDelayed(() -> listener.callback(new OCRResult(jsonObject)), 0);
        }).start();
    }

    public interface OCRListener {
        void callback(OCRResult result);
    }


    public static class OCRResult {
        public static final int SUCCESS_CODE = -1;

        private int errorCode = SUCCESS_CODE;
        private String errorMsg;
        private String[] wordsList;


        public OCRResult(JSONObject jsonObject) {
            try {
                if (!jsonObject.isNull("error_code")) {
                    Object errorCode = jsonObject.get("error_code");
                    this.errorCode = errorCode instanceof Integer ? (int) errorCode : 0;
                    errorMsg = jsonObject.getString("error_msg");
                } else {
                    int wordsNum = jsonObject.getInt("words_result_num");
                    wordsList = new String[wordsNum];
                    JSONArray jsonArray = jsonObject.getJSONArray("words_result");
                    for (int i = 0; i < wordsList.length; i++) {
                        wordsList[i] = jsonArray.getJSONObject(i).getString("words");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public String getAllWords() {
            StringBuilder sb = new StringBuilder();
            for (String s : wordsList) {
                if (!sb.toString().isEmpty()) {
                    sb.append("\n");
                }
                sb.append(s);
            }
            return sb.toString();
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

    }

    private byte[] bitmap2Bytes(Bitmap bitmap, boolean isAccurate) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int quality = isAccurate ? 100 : 50;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }


}

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
    }

    public void readBitmap(Bitmap bitmap, OCRListener listener) {
        byte[] base64 = bitmap2Bytes(bitmap);
        Log.d(TAG, "byte size:" + base64.length);
        HashMap<String, String> options = new HashMap<>();
        options.put("detect_language", "true");
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            JSONObject jsonObject = aipOcr.basicGeneral(base64, options);
            handler.postDelayed(() -> listener.callback(new OCRResult(jsonObject)), 0);
        }).start();
    }

    public interface OCRListener {
        void callback(OCRResult result);
    }


    public static class OCRResult {
        private int errorCode;
        private String errorMsg;
        private String[] wordsList;

        public static final int SUCCESS_CODE = 0;

        public OCRResult(JSONObject jsonObject) {
            try {
                if (!jsonObject.isNull("error_code")) {
                    errorCode = jsonObject.getInt("error_code");
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

    private byte[] bitmap2Bytes(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }


}

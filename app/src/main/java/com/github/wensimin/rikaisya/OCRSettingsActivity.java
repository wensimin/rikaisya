package com.github.wensimin.rikaisya;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class OCRSettingsActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.OCRSettings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        this.finish();
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    @Override
    public void finish() {
        this.checkAndSaveConfig();
        super.finish();
    }

    /**
     * 保存和验证设置
     */
    private void checkAndSaveConfig() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String baiduApi = preferences.getString(getResources().getString(R.string.baidu_OCR_config_title_API), null);
        String baiduSecret = preferences.getString(getResources().getString(R.string.baidu_OCR_config_title_Secret), null);
        String tencentId = preferences.getString(getResources().getString(R.string.tencent_translate_id), null);
        String tencentKey = preferences.getString(getResources().getString(R.string.tencent_translate_key), null);
        //TODO 验证数据有效性
        Log.d("", "checkAndSaveConfig: 1");
    }
}
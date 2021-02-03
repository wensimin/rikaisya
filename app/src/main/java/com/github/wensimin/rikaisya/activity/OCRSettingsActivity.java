package com.github.wensimin.rikaisya.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.utils.OCRUtils;

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

    /**
     * 验证设置
     */
    public void checkConfig(View view) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String baiduApi = preferences.getString(getResources().getString(R.string.baidu_OCR_config_title_API), null);
        String baiduSecret = preferences.getString(getResources().getString(R.string.baidu_OCR_config_title_Secret), null);
        String tencentId = preferences.getString(getResources().getString(R.string.tencent_translate_id), null);
        String tencentKey = preferences.getString(getResources().getString(R.string.tencent_translate_key), null);
        // TODO checkConfig
        OCRUtils.getInstance(baiduApi, baiduSecret, true);
        Toast.makeText(this, "检查config结果", Toast.LENGTH_LONG).show();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

}
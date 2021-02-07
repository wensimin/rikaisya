package com.github.wensimin.rikaisya.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.adapter.RikaiAdapter;
import com.github.wensimin.rikaisya.api.Rikai;
import com.github.wensimin.rikaisya.api.RikaiUtils;
import com.github.wensimin.rikaisya.service.RikaiFloatingService;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 顶部bar
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // 获取权限
        requestDrawOverLays();
        // 开启监听剪贴板服务
        startService(new Intent(this, RikaiFloatingService.class));
    }

    /**
     * 获取悬浮权限
     */
    private void requestDrawOverLays() {
        if (!Settings.canDrawOverlays(this)) {

            Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT).show();
            ActivityResultLauncher<String> launcher = registerForActivityResult(new ActivityResultContract<String, Boolean>() {
                @NonNull
                @Override
                public Intent createIntent(@NonNull Context context, String input) {
                    return new Intent(input, Uri.parse("package:" + getPackageName()));
                }

                @Override
                public Boolean parseResult(int resultCode, @Nullable Intent intent) {
                    return resultCode == 0;
                }
            }, result -> {
                if (result) {
                    if (!Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                        startService(new Intent(MainActivity.this, RikaiFloatingService.class));
                    }
                }
            });

            launcher.launch(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        }
    }

    /**
     * 进行解析
     */
    public void rikai(View view) {
        Toast.makeText(MainActivity.this, "rikai!", Toast.LENGTH_SHORT).show();
        // listView
        ListView listView = findViewById(R.id.list_view);
        ClipboardManager clipboardManager = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager.hasPrimaryClip()) {
            ClipData clipData = clipboardManager.getPrimaryClip();
            String text = Optional.ofNullable(clipData)
                    .map(c -> c.getItemAt(0))
                    .map(ClipData.Item::getText)
                    .map(CharSequence::toString).orElse("");
            Set<Rikai> rikais = RikaiUtils.rikai(text);
            RikaiAdapter adapter = new RikaiAdapter(MainActivity.this, android.R.layout.simple_list_item_1, new ArrayList<>(rikais));
            listView.setAdapter(adapter);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        acceptAction(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        acceptAction(getIntent());
    }

    /**
     * 接受服务请求
     */
    private void acceptAction(Intent intent) {
        // 判断是否是服务按钮发起的理解
        if (intent.getBooleanExtra(RikaiFloatingService.ACTION_NAME, false)) {
            // 消耗掉action
            intent.putExtra(RikaiFloatingService.ACTION_NAME, false);
            // 解析操作
            this.rikai(null);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting_btn, menu);
        return true;
    }


    public void openOCRSetting(MenuItem item) {
        Intent intent = new Intent(this, OCRSettingsActivity.class);
        startActivity(intent);
    }


}
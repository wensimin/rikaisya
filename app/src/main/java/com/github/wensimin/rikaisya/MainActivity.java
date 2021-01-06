package com.github.wensimin.rikaisya;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.wensimin.rikaisya.api.Rikai;
import com.github.wensimin.rikaisya.api.RikaiUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
        FloatingActionButton button = findViewById(R.id.fab);
        // 按钮发起理解
        button.setOnClickListener(b -> this.rikai());
        // 服务权限
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT).show();
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        }
    }

    /**
     * 进行解析
     */
    private void rikai() {
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
        acceptAction(this.getIntent());
    }

    /**
     * 接受服务请求
     */
    private void acceptAction(Intent intent) {
        // 判断是否是服务按钮发起的理解
        if (intent.getBooleanExtra(RikaiFloatingService.ACTION_NAME, false)) {
            // 解析操作
            this.rikai();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                startService(new Intent(MainActivity.this, RikaiFloatingService.class));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
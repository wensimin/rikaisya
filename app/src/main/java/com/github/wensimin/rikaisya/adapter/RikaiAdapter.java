package com.github.wensimin.rikaisya.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.wensimin.rikaisya.R;
import com.github.wensimin.rikaisya.api.Rikai;
import com.github.wensimin.rikaisya.api.RikaiType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 理解适配器
 */
public class RikaiAdapter extends ArrayAdapter<Rikai> {
    private final ClipboardManager clipboardManager;
    private final Context context;
    private Map<RikaiType, RikaiFunction> functions;
    private RikaiFunction defaultFunction;

    public RikaiAdapter(@NonNull Context context, int resource, @NonNull List<Rikai> objects) {
        super(context, resource, objects);
        this.clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        this.context = context;
        initFunction();
    }

    private void initFunction() {
        defaultFunction = (rikai, text, button) -> {
            button.setText("复制");
            button.setOnClickListener(getCopyFunction(text.getText()));
        };
        functions = new HashMap<>();
        functions.put(RikaiType.code, defaultFunction);
        functions.put(RikaiType.base64, ((rikai, text, button) -> {
            button.setText("解析");
            button.setOnClickListener(getCopyFunction(rikai.getRikaiText()));
        }));
        functions.put(RikaiType.tag, (rikai, text, button) -> {
            button.setText("提取");
            button.setOnClickListener(getCopyFunction(rikai.getRikaiText()));
        });
        functions.put(RikaiType.bilibili, ((rikai, text, button) -> {
            String url = "https://www.bilibili.com/video/" + rikai.getText();
            button.setText("打开");
            button.setOnClickListener(v -> this.openURL(url));
        }));
        functions.put(RikaiType.ip, ((rikai, text, button) -> {
            String url = "https://" + rikai.getText();
            button.setText("打开");
            button.setOnClickListener(v -> this.openURL(url));
        }));
        functions.put(RikaiType.url, ((rikai, text, button) -> {
            button.setText("打开");
            button.setOnClickListener(v -> this.openURL(rikai.getText()));
        }));
    }

    private View.OnClickListener getCopyFunction(CharSequence text) {
        return v -> {
            ClipData clipData = ClipData.newPlainText(text, text);
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(context, "已复制:" + text, Toast.LENGTH_SHORT).show();
        };
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // 获取当前行数据
        Rikai rikai = getItem(position);
        if (rikai == null) {
            return super.getView(position, convertView, parent);
        }
        if (convertView == null) {
            // inflate出子项布局
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.rikai_item, parent, false);
            TextView textView = rowView.findViewById(R.id.rikai_text);
            Button button = rowView.findViewById(R.id.rikai_button);
            textView.setText(rikai.getText());
            View.OnClickListener textCopyFunction = getCopyFunction(textView.getText());
            textView.setOnClickListener(textCopyFunction);
            RikaiFunction rikaiFunction = functions.get(rikai.getType());
            if (rikaiFunction == null) {
                defaultFunction.action(rikai, textView, button);
            } else {
                rikaiFunction.action(rikai, textView, button);
            }
            return rowView;
        }
        return convertView;
    }

    /**
     * 打开url
     *
     * @param urlString url
     */
    private void openURL(String urlString) {
        Uri uri = Uri.parse(urlString);
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setData(uri);
        context.startActivity(intent);
    }

    @FunctionalInterface
    private interface RikaiFunction {
        void action(Rikai rikai, TextView text, Button button);
    }
}

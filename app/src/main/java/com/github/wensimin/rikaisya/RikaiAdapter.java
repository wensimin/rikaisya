package com.github.wensimin.rikaisya;

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

import com.github.wensimin.rikaisya.api.Rikai;
import com.github.wensimin.rikaisya.api.RikaiType;

import java.util.List;

/**
 * 理解适配器
 */
public class RikaiAdapter extends ArrayAdapter<Rikai> {
    private ClipboardManager clipboardManager;
    private final Context context;

    public RikaiAdapter(@NonNull Context context, int resource, @NonNull List<Rikai> objects) {
        super(context, resource, objects);
        this.clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        this.context = context;
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
            View.OnClickListener copyListener = view -> {
                ClipData clipData = ClipData.newPlainText(rikai.getText(), rikai.getText());
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(context, "已复制:" + rikai.getText(), Toast.LENGTH_SHORT).show();
            };
            textView.setOnClickListener(copyListener);
            Button button = rowView.findViewById(R.id.rikai_button);
            textView.setText(rikai.getText());
            switch (rikai.getType()) {
                case code:
                    button.setText("复制");
                    button.setOnClickListener(copyListener);
                    break;
                case base64:
                    button.setText("解析");
                    button.setOnClickListener(view -> {
                        ClipData clipData = ClipData.newPlainText(rikai.getRikaiText(), rikai.getRikaiText());
                        clipboardManager.setPrimaryClip(clipData);
                        Toast.makeText(context, "已复制:" + rikai.getRikaiText(), Toast.LENGTH_SHORT).show();
                    });
                    break;
                case tag:
                    button.setText("提取");
                    button.setOnClickListener(view -> {
                        ClipData clipData = ClipData.newPlainText(rikai.getRikaiText(), rikai.getRikaiText());
                        clipboardManager.setPrimaryClip(clipData);
                        Toast.makeText(context, "已复制:" + rikai.getRikaiText(), Toast.LENGTH_SHORT).show();
                    });
                    break;
                default:
                    button.setText("打开");
                    button.setOnClickListener(b -> {
                        String url = rikai.getText();
                        if (rikai.getType() == RikaiType.bilibili) {
                            url = "https://www.bilibili.com/video/" + url;
                        } else if (rikai.getType() == RikaiType.ip) {
                            url = "https://" + url;
                        }
                        this.openUrl(url);
                    });
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
    private void openUrl(String urlString) {
        Uri uri = Uri.parse(urlString);
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setData(uri);
        context.startActivity(intent);
    }
}

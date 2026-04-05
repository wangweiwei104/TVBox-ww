package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.R;

import org.jetbrains.annotations.NotNull;

public class AboutDialog extends BaseDialog {

    public AboutDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_about);

        // 初始化视图
        initViews();
    }

    private void initViews() {
        TextView versionTextView = findViewById(R.id.versionTextView);
        if (versionTextView == null) return;

        try {
            String versionName = BuildConfig.VERSION_NAME;
            String displayText = parseVersionInfo(versionName);
            versionTextView.setText(displayText);
        } catch (Exception e) {
            versionTextView.setText("版本: 未知");
            e.printStackTrace();
        }
    }

    private String parseVersionInfo(String versionName) {
        if (versionName == null || versionName.isEmpty()) {
            return "版本: 未知";
        }

        // 按 "_" 分割版本号
        String[] parts = versionName.split("_");

        StringBuilder result = new StringBuilder();

        if (parts.length >= 1) {
            result.append("版本: ").append(parts[0]);

            if (parts.length >= 2) {
                result.append("_").append(parts[1]);
            }

            if (parts.length >= 3) {
                result.append("\n二开版本: ").append(parts[2]);
            }

            // 如果有更多部分，也显示为二开版本的一部分
            for (int i = 3; i < parts.length; i++) {
                result.append("_").append(parts[i]);
            }
        }

        return result.toString();
    }
}
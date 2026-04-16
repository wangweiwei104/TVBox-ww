package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.ui.dialog.BaseDialog;
import com.github.tvbox.osc.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 更新对话框 - 只显示更新信息，不提供下载功能
 */
public class UpdateDialog extends BaseDialog {

    // 更新信息类
    public static class UpdateInfo {
        String latestVersion;
        String releaseName;
        String releaseDate;
        String body;
        String downloadUrl;

        public UpdateInfo(String latestVersion, String releaseName, String releaseDate, String body, String downloadUrl) {
            this.latestVersion = latestVersion;
            this.releaseName = releaseName;
            this.releaseDate = releaseDate;
            this.body = body;
            this.downloadUrl = downloadUrl;
        }

        public boolean isValid() {
            return latestVersion != null && !latestVersion.isEmpty();
        }
    }

    private final UpdateInfo updateInfo;

    public UpdateDialog(@NonNull Context context, UpdateInfo updateInfo) {
        super(context);
        this.updateInfo = updateInfo;
        setContentView(R.layout.dialog_update);
        initViews();
    }

    private void initViews() {
        // 获取视图引用
        TextView tvCurrentVersion = findViewById(R.id.tvCurrentVersion);
        TextView tvLatestVersion = findViewById(R.id.tvLatestVersion);
        TextView tvUpdateBody = findViewById(R.id.tvUpdateBody);
        TextView tvReleaseDate = findViewById(R.id.tvReleaseDate);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnUpdate = findViewById(R.id.btnUpdate);

        if (tvCurrentVersion == null || tvLatestVersion == null ||
                tvUpdateBody == null || tvReleaseDate == null ||
                btnCancel == null || btnUpdate == null) {
            dismiss();
            return;
        }

        // 设置更新信息
        String currentVersion = BuildConfig.VERSION_NAME;
        tvCurrentVersion.setText(String.format("当前版本: v%s", currentVersion));
        tvLatestVersion.setText(String.format("最新版本: v%s", updateInfo.latestVersion));

        // 处理更新内容，如果为空则显示默认文本
        String updateBody = updateInfo.body != null && !updateInfo.body.isEmpty() ?
                updateInfo.body : "无更新说明";
        tvUpdateBody.setText(updateBody);

        // 格式化并显示发布日期
        String formattedDate = formatDate(updateInfo.releaseDate);
        tvReleaseDate.setText(String.format("发布日期: %s", formattedDate));

        // 按钮事件
        btnCancel.setOnClickListener(v -> dismiss());

        // 修改：删除原来的下载功能，只显示提示
        btnUpdate.setOnClickListener(v -> {
            // 显示提示信息
            Toast.makeText(getContext(), "暂无稳定可用的下载服务器", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 格式化日期
     */
    private String formatDate(String isoDate) {
        try {
            if (isoDate == null || isoDate.isEmpty()) {
                return "未知";
            }

            // ISO 8601 格式: 2026-04-05T07:33:03Z
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(isoDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            return isoDate; // 如果解析失败，返回原始字符串
        }
    }

    /**
     * 版本比较工具方法
     */
    public static int compareVersions(String version1, String version2) {
        try {
            String[] parts1 = version1.split("_");
            String[] parts2 = version2.split("_");

            // 比较每部分
            for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
                int result = comparePart(parts1[i], parts2[i]);
                if (result != 0) {
                    return result;
                }
            }

            // 如果前几部分相同，长度长的版本更新
            return Integer.compare(parts1.length, parts2.length);
        } catch (Exception e) {
            // 如果解析失败，使用字符串比较
            return version1.compareTo(version2);
        }
    }

    /**
     * 比较版本号的一部分
     */
    private static int comparePart(String part1, String part2) {
        // 尝试按数字比较
        if (part1.matches("\\d+") && part2.matches("\\d+")) {
            int num1 = Integer.parseInt(part1);
            int num2 = Integer.parseInt(part2);
            return Integer.compare(num1, num2);
        }

        // 尝试按日期格式比较 (如 20260227-1116)
        if (part1.matches("\\d{8}-\\d{4}") && part2.matches("\\d{8}-\\d{4}")) {
            return part1.compareTo(part2);
        }

        // 其他情况使用字符串比较
        return part1.compareTo(part2);
    }
}
package com.github.tvbox.osc.ui.dialog;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.DefaultConfig;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.OnPermissionCallback;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 更新对话框 - 显示更新信息并提供应用内下载和自动安装功能
 * 只在需要更新时显示
 */
public class UpdateDialog extends BaseDialog {

    // 下载管理器相关
    private DownloadManager downloadManager;
    private long downloadId = -1;
    private String downloadFileName;
    private BroadcastReceiver downloadCompleteReceiver;

    // 新增：GitHub代理前缀常量
    private static final String GITHUB_PROXY_PREFIX = "https://gh-proxy.com/";

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
    private OnUpdateListener updateListener;
    private boolean isDownloading = false;

    public UpdateDialog(@NonNull Context context, UpdateInfo updateInfo) {
        super(context);
        this.updateInfo = updateInfo;
        setContentView(R.layout.dialog_update);
        initDownloadManager();
        initViews();
    }

    /**
     * 初始化下载管理器
     */
    private void initDownloadManager() {
        downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);

        // 注册下载完成广播接收器
        downloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long receivedDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

                if (downloadId == receivedDownloadId && downloadManager != null) {
                    // 下载完成，开始安装
                    installApk(context);
                }
            }
        };

        // 注册广播接收器
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        getContext().registerReceiver(downloadCompleteReceiver, filter);
    }

    /**
     * 设置更新监听器
     */
    public void setOnUpdateListener(OnUpdateListener listener) {
        this.updateListener = listener;
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

        btnUpdate.setOnClickListener(v -> {
            if (updateListener != null) {
                updateListener.onUpdateConfirmed(updateInfo);
            }
            // 检查权限并开始下载
            checkStoragePermissionAndDownload();
        });
    }

    /**
     * 检查存储权限并开始下载
     */
    private void checkStoragePermissionAndDownload() {
        Context context = getContext();

        // 检查是否已授予存储权限
        if (XXPermissions.isGranted(context, DefaultConfig.StoragePermissionGroup())) {
            // 已有权限，开始下载
            startDownload(updateInfo.downloadUrl);
        } else {
            // 请求存储权限
            XXPermissions.with(context)
                    .permission(DefaultConfig.StoragePermissionGroup())
                    .request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                            if (all) {
                                Toast.makeText(context, "已获得存储权限", Toast.LENGTH_SHORT).show();
                                // 权限已授予，开始下载
                                startDownload(updateInfo.downloadUrl);
                            }
                        }

                        @Override
                        public void onDenied(List<String> permissions, boolean never) {
                            if (never) {
                                Toast.makeText(context, "获取存储权限失败,请在系统设置中开启", Toast.LENGTH_SHORT).show();
                                // 用户选择了"不再询问"，引导用户到设置页面
                                if (context instanceof android.app.Activity) {
                                    XXPermissions.startPermissionActivity((android.app.Activity) context, permissions);
                                }
                            } else {
                                Toast.makeText(context, "获取存储权限失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
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
     * 启动应用内下载
     */
    private void startDownload(String originalDownloadUrl) {
        // 参数名改为 originalDownloadUrl 以更清晰
        if (originalDownloadUrl == null || originalDownloadUrl.isEmpty()) {
            Toast.makeText(getContext(), "下载链接无效", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isDownloading) {
            Toast.makeText(getContext(), "下载已在进行中", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- 新增：根据网络状况处理下载链接 ---
        String finalDownloadUrl = originalDownloadUrl;
        String networkStatus = "";

        // 判断是否能访问外部网络（以Google为例）
        boolean accessible = canAccessGoogle();
        if (!accessible) {
            // 若不能访问，则在原始链接前拼接代理前缀
            finalDownloadUrl = GITHUB_PROXY_PREFIX + originalDownloadUrl;
            networkStatus = "（通过代理）";
            Toast.makeText(getContext(), "网络受限，已启用代理加速下载", Toast.LENGTH_LONG).show();
        } else {
            networkStatus = "（直连）";
            Toast.makeText(getContext(), "网络通畅，使用原始链接下载", Toast.LENGTH_LONG).show();
        }
        // --- 链接处理结束 ---

        try {
            Log.d("UpdateDialog", "最终下载链接: " + finalDownloadUrl);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(finalDownloadUrl)); // 使用处理后的最终链接
            request.setTitle("TVBox 更新" + networkStatus); // 在通知标题中显示连接方式
            request.setDescription("正在下载新版本，下载完成后将自动安装");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);

            // 设置文件名
            downloadFileName = "TVBox_" + updateInfo.latestVersion + ".apk";

            // 设置保存位置
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadFileName);

            // 设置MIME类型
            request.setMimeType("application/vnd.android.package-archive");

            // 开始下载
            if (downloadManager != null) {
                downloadId = downloadManager.enqueue(request);
                isDownloading = true;

                // 更新按钮状态
                Button btnUpdate = findViewById(R.id.btnUpdate);
                if (btnUpdate != null) {
                    btnUpdate.setText(getContext().getResources().getString(R.string.update_downloading));
                    btnUpdate.setEnabled(false);
                }

                Toast.makeText(getContext(), getContext().getResources().getString(R.string.update_download_start), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "下载服务不可用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("UpdateDialog", "下载失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否能访问Google（用于判断网络环境）
     * 这个方法会对Google的204响应服务发起一个简单的网络请求
     * 注意：应在后台线程中调用此方法
     *
     * @return 如果能访问Google则返回true，否则返回false
     */
    private boolean canAccessGoogle() {
        HttpURLConnection connection = null;

        try {
            // 使用Google的204响应服务（轻量级，无实际内容）
            URL url = new URL("http://www.google.com/generate_204");

            // 设置超时值（单位：毫秒）
            int timeoutMs = 3000; // 3秒连接超时
            int responseTimeoutMs = 3000; // 3秒读取超时

            connection = (HttpURLConnection) url.openConnection();

            // 配置连接
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(responseTimeoutMs);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);

            // 尝试连接
            connection.connect();

            // 获取响应码
            int responseCode = connection.getResponseCode();

            // 204是Google的generate_204服务的标准"无内容"响应
            // 有时可能会返回200或其他2xx状态码
            boolean isAccessible = (responseCode == 204 || responseCode >= 200 && responseCode < 300);

//            Log.d("UpdateDialog", "Google可访问性检查: responseCode=" + responseCode + ", accessible=" + isAccessible);
            return isAccessible;

        } catch (Exception e) {
            // 任何异常（超时、IO错误等）都意味着无法访问Google
//            Log.d("UpdateDialog", "Google可访问性检查失败: " + e.getMessage());
            return false;

        } finally {
            // 清理资源
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 安装APK
     */
    private void installApk(Context context) {
        try {
            // 获取下载文件路径
            File downloadFile = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    downloadFileName
            );

            if (!downloadFile.exists()) {
                Toast.makeText(context, "安装文件不存在: " + downloadFile.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
                return;
            }

            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0及以上版本需要使用FileProvider
                // 注意：这里使用与AndroidManifest.xml中配置一致的authorities
                String authorities = context.getPackageName() + ".provider";

                Uri apkUri = FileProvider.getUriForFile(context, authorities, downloadFile);

                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                // Android 7.0以下版本
                Uri apkUri = Uri.fromFile(downloadFile);
                installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            }

            // 检查是否有应用可以处理安装Intent
            if (installIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(installIntent);
                Toast.makeText(context, "正在安装新版本...", Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                Toast.makeText(context, "未找到安装程序，请手动安装", Toast.LENGTH_LONG).show();
                enableManualInstall();
            }
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = "安装失败: " + e.getMessage();
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
            enableManualInstall();
        } finally {
            isDownloading = false;
        }
    }

    /**
     * 启用手动安装按钮
     */
    private void enableManualInstall() {
        Button btnUpdate = findViewById(R.id.btnUpdate);
        if (btnUpdate != null) {
            btnUpdate.setText("手动安装");
            btnUpdate.setEnabled(true);
            btnUpdate.setOnClickListener(v -> {
                File downloadFile = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        downloadFileName
                );
                openFileManager(downloadFile);
            });
        }
    }

    /**
     * 打开文件管理器
     */
    private void openFileManager(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "*/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                getContext().startActivity(intent);
            } else {
                Toast.makeText(getContext(), "未找到文件管理器", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "打开文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    /**
     * 对话框关闭时清理资源
     */
    @Override
    public void dismiss() {
        // 注销广播接收器
        if (downloadCompleteReceiver != null) {
            try {
                getContext().unregisterReceiver(downloadCompleteReceiver);
            } catch (IllegalArgumentException e) {
                // 接收器可能未注册，忽略异常
            }
        }

        super.dismiss();
    }

    /**
     * 更新监听器接口
     */
    public interface OnUpdateListener {
        void onUpdateConfirmed(UpdateInfo updateInfo);
    }
}
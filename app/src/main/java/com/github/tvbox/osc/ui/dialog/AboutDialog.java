// AboutDialog.java (改进版)
package com.github.tvbox.osc.ui.dialog;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;

import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;

import java.util.Locale;
import java.util.TimeZone;


public class AboutDialog extends BaseDialog {

    public AboutDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_about);

        // 初始化所有视图
        initViews();
    }

    private void initViews() {
        // 获取版本信息
        String versionName = BuildConfig.VERSION_NAME;
        String[] versionParts = versionName.split("_");

        // 设置完整版本号（居中显示）
        TextView fullVersionTextView = findViewById(R.id.fullVersionTextView);
        if (fullVersionTextView != null) {
            fullVersionTextView.setText("v" + versionName);
        }

        // 设置版本号第一部分
        TextView versionPart1TextView = findViewById(R.id.versionPart1TextView);
        if (versionPart1TextView != null && versionParts.length >= 1) {
            versionPart1TextView.setText(versionParts[0]);
        }

        // 设置Takagen99版本（第二部分）
        TextView takagenVersionTextView = findViewById(R.id.takagenVersionTextView);
        if (takagenVersionTextView != null && versionParts.length >= 2) {
            takagenVersionTextView.setText(versionParts[1]);
        }

        // 设置我的版本（第三部分）
        TextView myVersionTextView = findViewById(R.id.myVersionTextView);
        if (myVersionTextView != null && versionParts.length >= 3) {
            myVersionTextView.setText(versionParts[2]);
        }

        // 设置本机解码器点击事件 (绑定到新的LinearLayout)
        LinearLayout llDecoder = findViewById(R.id.llDecoder);
        if (llDecoder != null) {
            llDecoder.setOnClickListener(v -> showDecoderInfo());
        }

        // 设置免责声明点击事件
        LinearLayout llDisclaimer = findViewById(R.id.llDisclaimer);
        if (llDisclaimer != null) {
            llDisclaimer.setOnClickListener(v -> showDisclaimer());
        }

        // 设置检查更新点击事件
        LinearLayout llCheckUpdate = findViewById(R.id.llCheckUpdate);
        if (llCheckUpdate != null) {
            llCheckUpdate.setOnClickListener(v -> checkForUpdates());
        }

        // 设置仓库链接点击事件
        LinearLayout llRepoLinks = findViewById(R.id.llRepoLinks);
        if (llRepoLinks != null) {
            llRepoLinks.setOnClickListener(v -> {
                RepoLinksDialog repoLinksDialog = new RepoLinksDialog(getContext());
                repoLinksDialog.show();
            });
        }
    }

    /**
     * 免责声明二级页面 - 使用BaseDialog子类
     */
    private void showDisclaimer() {
        try {
            // 创建并显示自定义的DisclaimerDialog
            DisclaimerDialog disclaimerDialog = new DisclaimerDialog(getContext());

            // 显示对话框
            disclaimerDialog.show();

        } catch (Exception e) {
//            Log.e("AboutDialog", "显示免责声明失败: " + e.getMessage(), e);
            e.printStackTrace();

            // 如果加载自定义布局失败，则使用简单的文本对话框作为后备方案
            String fallbackText = "本软件只提供聚合展示功能，所有资源来自网上，软件不参与任何制作、上传、储存、下载等内容。\n\n" +
                    "软件仅供学习参考，请于安装后24小时内删除。\n\n" +
                    "本软件为开源项目，遵循相关开源协议。使用者应遵守当地法律法规，不得用于非法用途。\n\n" +
                    "如因使用本软件产生的任何问题，开发者不承担任何责任。";

            new AlertDialog.Builder(getContext())
                    .setTitle("免责声明")
                    .setMessage(fallbackText)
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    /**
     * 检查更新功能
     */
    private void checkForUpdates() {
        // 显示检查中的提示
        Toast.makeText(getContext(), "正在检查更新...", Toast.LENGTH_SHORT).show();

        // 在后台线程中检查更新
        new AsyncTask<Void, Void, UpdateDialog.UpdateInfo>() {
            @Override
            protected UpdateDialog.UpdateInfo doInBackground(Void... voids) {
                return fetchLatestReleaseInfo();
            }

            @Override
            protected void onPostExecute(UpdateDialog.UpdateInfo updateInfo) {
                if (updateInfo != null && updateInfo.isValid()) {
                    // 有更新信息，检查版本是否需要更新
                    showUpdateDialog(updateInfo);
                } else {
                    // 检查失败
                    Toast.makeText(getContext(), "检查更新失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    /**
     * 获取最新发布信息
     */
    private UpdateDialog.UpdateInfo fetchLatestReleaseInfo() {
        try {
            // GitHub API URL
            String apiUrl = "https://api.github.com/repos/wangweiwei104/TVBox-ww/releases/latest";

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                String latestVersion = jsonResponse.getString("tag_name");
                String releaseName = jsonResponse.optString("name", latestVersion);
                String releaseDate = convertUTCToBeijingTime(jsonResponse.optString("published_at", ""));
                String body = jsonResponse.optString("body", "");

                // 获取下载链接
                String downloadUrl = null;
                if (jsonResponse.has("assets")) {
                    JSONArray assets = jsonResponse.getJSONArray("assets");
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String assetName = asset.getString("name");
                        if (assetName.endsWith(".apk")) {
                            downloadUrl = asset.getString("browser_download_url");
                            break;
                        }
                    }
                }

                // 如果没找到apk，使用zip包
                if (downloadUrl == null && jsonResponse.has("zipball_url")) {
                    downloadUrl = jsonResponse.getString("zipball_url");
                }

                return new UpdateDialog.UpdateInfo(latestVersion, releaseName, releaseDate, body, downloadUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将 UTC 时间字符串转换为北京时间字符串
     *
     * @param utcTimeStr UTC 时间字符串，格式必须为 "yyyy-MM-dd'T'HH:mm:ss'Z'"
     * @return 北京时间字符串，格式为 "yyyy-MM-dd HH:mm:ss"
     */
    public static String convertUTCToBeijingTime(String utcTimeStr) {
        return convertUTCToBeijingTime(utcTimeStr, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 将 UTC 时间字符串转换为指定格式的北京时间字符串
     *
     * @param utcTimeStr UTC 时间字符串，格式必须为 "yyyy-MM-dd'T'HH:mm:ss'Z'"
     * @param pattern 输出时间的格式模式
     * @return 指定格式的北京时间字符串，如果转换失败返回空字符串
     */
    public static String convertUTCToBeijingTime(String utcTimeStr, String pattern) {
        try {
            // 创建 UTC 时区的日期格式化器
            SimpleDateFormat utcFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

            // 解析 UTC 时间
            Date utcDate = utcFormatter.parse(utcTimeStr);

            // 创建北京时区的日期格式化器
            SimpleDateFormat beijingFormatter = new SimpleDateFormat(pattern, Locale.getDefault());
            beijingFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

            // 格式化为北京时间
            return beijingFormatter.format(utcDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 显示更新对话框 - 只有需要更新时才显示
     */
    private void showUpdateDialog(UpdateDialog.UpdateInfo updateInfo) {
        String currentVersion = BuildConfig.VERSION_NAME;
        String latestVersion = updateInfo.latestVersion;

        // 对比版本号
        int comparison = UpdateDialog.compareVersions(currentVersion, latestVersion);

        if (comparison < 0) {
            // 有新版本可用 -> 显示更新对话框
            UpdateDialog updateDialog = new UpdateDialog(getContext(), updateInfo);

            updateDialog.show();
        } else if (comparison == 0) {
            // 当前已是最新版本 -> 只显示Toast，不弹对话框
            Toast.makeText(getContext(), "当前已是最新版本", Toast.LENGTH_SHORT).show();
        } else {
            // 当前版本比最新版本还新（可能是开发版）-> 只显示Toast，不弹对话框
            Toast.makeText(getContext(), "当前为开发版本", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 美化解码器信息显示（使用新设计的对话框）
     */
    private void showDecoderInfo() {
        try {
            // 创建并显示自定义的DecoderInfoDialog
            DecoderInfoDialog decoderDialog = new DecoderInfoDialog(getContext());

            // 显示对话框
            decoderDialog.show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "获取解码器信息失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 在浏览器中打开URL
     */
    private void openUrlInBrowser(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            getContext().startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "无法打开链接", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 复制文本到剪贴板
     */
    private void copyToClipboard(String label, String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
        } catch (Exception e) {
            Toast.makeText(getContext(), "复制失败", Toast.LENGTH_SHORT).show();
        }
    }
}






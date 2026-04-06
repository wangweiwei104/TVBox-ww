package com.github.tvbox.osc.ui.dialog;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AboutDialog extends BaseDialog {

    private static final int REQUEST_WRITE_STORAGE = 1001;
    private List<String> currentDecoders = new ArrayList<>();

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
            versionPart1TextView.setText("版本号：" + versionParts[0]);
        }

        // 设置Takagen99版本（第二部分）
        TextView takagenVersionTextView = findViewById(R.id.takagenVersionTextView);
        if (takagenVersionTextView != null && versionParts.length >= 2) {
            takagenVersionTextView.setText("Takagen99版本：" + versionParts[1]);
        }

        // 设置二开版本（第三部分）
        TextView forkVersionTextView = findViewById(R.id.forkVersionTextView);
        if (forkVersionTextView != null && versionParts.length >= 3) {
            forkVersionTextView.setText("二开版本：" + versionParts[2]);
        }

        // 设置本机解码器点击事件
        TextView decoderTextView = findViewById(R.id.decoderTextView);
        if (decoderTextView != null) {
            decoderTextView.setOnClickListener(v -> showDecoderInfo());
        }

//        // 设置免责声明点击事件
//        TextView disclaimerTextView = findViewById(R.id.disclaimerTextView);
//        if (disclaimerTextView != null) {
//            disclaimerTextView.setOnClickListener(v -> showDisclaimer());
//        }

        // 设置免责声明点击事件
        TextView disclaimerTextView = findViewById(R.id.disclaimerTextView);
        if (disclaimerTextView != null) {
            Log.d("AboutDialog", "找到disclaimerTextView");
            disclaimerTextView.setOnClickListener(v -> {
                Log.d("AboutDialog", "免责声明按钮被点击");
                showDisclaimer();
            });
        } else {
            Log.e("AboutDialog", "未找到disclaimerTextView");
        }

        // 设置检查更新点击事件
        TextView checkUpdateTextView = findViewById(R.id.checkUpdateTextView);
        if (checkUpdateTextView != null) {
            checkUpdateTextView.setOnClickListener(v -> checkForUpdates());
        }

        // 设置原仓库链接点击事件
        TextView originalRepoTextView = findViewById(R.id.originalRepoTextView);
        if (originalRepoTextView != null) {
            originalRepoTextView.setOnClickListener(v -> {
                openUrlInBrowser("https://github.com/CatVodTVOfficial/TVBoxOSC");
            });

            // 长按复制功能
            originalRepoTextView.setOnLongClickListener(v -> {
                copyToClipboard("原仓库地址", "https://github.com/CatVodTVOfficial/TVBoxOSC");
                Toast.makeText(getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // 设置Takagen99仓库链接点击事件
        TextView takagenRepoTextView = findViewById(R.id.takagenRepoTextView);
        if (takagenRepoTextView != null) {
            takagenRepoTextView.setOnClickListener(v -> {
                openUrlInBrowser("https://github.com/takagen99/Box");
            });

            // 长按复制功能
            takagenRepoTextView.setOnLongClickListener(v -> {
                copyToClipboard("Takagen99仓库地址", "https://github.com/takagen99/Box");
                Toast.makeText(getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // 设置二开仓库链接点击事件
        TextView forkRepoTextView = findViewById(R.id.forkRepoTextView);
        if (forkRepoTextView != null) {
            forkRepoTextView.setOnClickListener(v -> {
                openUrlInBrowser("https://github.com/wangweiwei104/TVBox-ww");
            });

            // 长按复制功能
            forkRepoTextView.setOnLongClickListener(v -> {
                copyToClipboard("二开仓库地址", "https://github.com/wangweiwei104/TVBox-ww");
                Toast.makeText(getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    /**
     * 免责声明二级页面 - 使用自定义布局
     */
    private void showDisclaimer() {
        Log.d("AboutDialog", "showDisclaimer() 被调用");
        try {
            Log.d("AboutDialog", "开始加载 dialog_disclaimer.xml");

            // 使用自定义布局文件 dialog_disclaimer.xml
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_disclaimer, null);
            Log.d("AboutDialog", "dialog_disclaimer.xml 加载成功");

            // 获取布局中的TextView
            TextView disclaimerContent = dialogView.findViewById(R.id.disclaimerContent);
            Log.d("AboutDialog", "找到 disclaimerContent: " + (disclaimerContent != null));

            if (disclaimerContent != null) {
                Log.d("AboutDialog", "disclaimerContent ID: " + disclaimerContent.getId());
            }

            // 设置详细的免责声明内容
            String disclaimerText = "本软件只提供聚合展示功能，所有资源来自网上，软件不参与任何制作、上传、储存、下载等内容。\n\n" +
                    "软件仅供学习参考，请于安装后24小时内删除。\n\n" +
                    "本软件为开源项目，遵循相关开源协议。使用者应遵守当地法律法规，不得用于非法用途。\n\n" +
                    "如因使用本软件产生的任何问题，开发者不承担任何责任。";

            if (disclaimerContent != null) {
                disclaimerContent.setText(disclaimerText);
                Log.d("AboutDialog", "免责声明文本已设置，长度: " + disclaimerText.length());
            }

            // 创建并显示对话框
            Log.d("AboutDialog", "创建 AlertDialog");
            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setTitle("免责声明")
                    .setView(dialogView)
                    .setPositiveButton("确定", null)
                    .create();

            Log.d("AboutDialog", "显示 AlertDialog");
            dialog.show();

            // 添加对话框显示后的日志
            if (dialog.getWindow() != null) {
                Log.d("AboutDialog", "对话框窗口宽度: " + dialog.getWindow().getAttributes().width);
                Log.d("AboutDialog", "对话框窗口高度: " + dialog.getWindow().getAttributes().height);
            }

        } catch (Exception e) {
            Log.e("AboutDialog", "加载免责声明失败: " + e.getMessage());
            // 如果加载自定义布局失败，则使用简单的文本对话框
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
        new AsyncTask<Void, Void, UpdateInfo>() {
            @Override
            protected UpdateInfo doInBackground(Void... voids) {
                return fetchLatestReleaseInfo();
            }

            @Override
            protected void onPostExecute(UpdateInfo updateInfo) {
                if (updateInfo != null && updateInfo.isValid()) {
                    showUpdateDialog(updateInfo);
                } else {
                    Toast.makeText(getContext(), "检查更新失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    /**
     * 获取最新发布信息
     */
    private UpdateInfo fetchLatestReleaseInfo() {
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
                String releaseDate = jsonResponse.optString("published_at", "");
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

                return new UpdateInfo(latestVersion, releaseName, releaseDate, body, downloadUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 显示更新对话框
     */
    private void showUpdateDialog(UpdateInfo updateInfo) {
        String currentVersion = BuildConfig.VERSION_NAME;
        String latestVersion = updateInfo.latestVersion;

        // 对比版本号
        int comparison = compareVersions(currentVersion, latestVersion);

        if (comparison < 0) {
            // 有新版本可用
            String message = String.format(
                    "当前版本: v%s\n" +
                            "最新版本: v%s\n\n" +
                            "更新内容:\n%s\n\n" +
                            "发布日期: %s",
                    currentVersion, latestVersion,
                    updateInfo.body.length() > 0 ? updateInfo.body : "无更新说明",
                    formatDate(updateInfo.releaseDate)
            );

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                    .setTitle("发现新版本")
                    .setMessage(message)
                    .setPositiveButton("立即下载", (dialog, which) -> {
                        if (updateInfo.downloadUrl != null) {
                            openUrlInBrowser(updateInfo.downloadUrl);
                        } else {
                            openUrlInBrowser("https://github.com/wangweiwei104/TVBox-ww/releases/latest");
                        }
                    })
                    .setNegativeButton("取消", null);

            // 如果不想更新，可以稍后提醒
            builder.setNeutralButton("稍后提醒", null);

            builder.show();
        } else if (comparison == 0) {
            Toast.makeText(getContext(), "当前已是最新版本", Toast.LENGTH_SHORT).show();
        } else {
            // 当前版本比最新版本还新（可能是开发版）
            Toast.makeText(getContext(), "当前为开发版本", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 比较版本号
     * 版本号格式: 1.0_20260227-1116_20260404-2323
     * 比较规则: 按"_"分割，比较每一部分
     */
    private int compareVersions(String version1, String version2) {
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
    private int comparePart(String part1, String part2) {
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
            return isoDate;
        }
    }

    /**
     * 更新信息类
     */
    private static class UpdateInfo {
        String latestVersion;
        String releaseName;
        String releaseDate;
        String body;
        String downloadUrl;

        UpdateInfo(String latestVersion, String releaseName, String releaseDate, String body, String downloadUrl) {
            this.latestVersion = latestVersion;
            this.releaseName = releaseName;
            this.releaseDate = releaseDate;
            this.body = body;
            this.downloadUrl = downloadUrl;
        }

        boolean isValid() {
            return latestVersion != null && !latestVersion.isEmpty();
        }
    }

    /**
     * 美化解码器信息显示
     */
    private void showDecoderInfo() {
        try {
            // 获取视频解码器列表
            currentDecoders = getVideoDecoders();

            // 创建自定义对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_decoder_info, null);
            builder.setView(dialogView);
            builder.setPositiveButton("确定", null);

            TextView decoderCount = dialogView.findViewById(R.id.decoderCount);
            ListView decoderList = dialogView.findViewById(R.id.decoderList);
            Button saveButton = dialogView.findViewById(R.id.btnSaveDecoders);

            // 设置解码器数量
            int decoderNum = currentDecoders.size();
            decoderCount.setText(String.format("共找到 %d 个视频解码器", decoderNum));

            // 设置列表适配器
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                    android.R.layout.simple_list_item_1, currentDecoders) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView textView = (TextView) view.findViewById(android.R.id.text1);
                    textView.setTextColor(Color.parseColor("#333333"));
                    textView.setTextSize(16);
                    textView.setPadding(15, 15, 15, 15);
                    return view;
                }
            };
            decoderList.setAdapter(adapter);

            // 设置保存按钮点击事件
            if (saveButton != null) {
                saveButton.setOnClickListener(v -> {
                    saveDecodersToFile(currentDecoders);
                });
            }

            // 显示对话框
            AlertDialog dialog = builder.create();
            dialog.show();

        } catch (Exception e) {
            Toast.makeText(getContext(), "获取解码器信息失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 保存解码器信息到文件
     */
    private void saveDecodersToFile(List<String> decoders) {
        try {
            // 检查存储权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // 请求权限 - 使用 getContext() 并转换为 Activity
                    if (getContext() instanceof Activity) {
                        ActivityCompat.requestPermissions((Activity) getContext(),
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_WRITE_STORAGE);
                    } else {
                        Toast.makeText(getContext(), "无法请求权限，上下文不是Activity", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
            }

            // 创建文件名
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timeStamp = sdf.format(new Date());
            String fileName = "解码器列表_" + timeStamp + ".txt";

            // 获取保存目录
            File saveDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10及以上使用应用私有目录
                saveDir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Decoders");
            } else {
                // Android 10以下使用外部存储公共目录
                saveDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TVBoxDecoders");
            }

            // 确保目录存在
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            // 创建文件
            File file = new File(saveDir, fileName);

            // 写入文件
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");

            // 写入文件头
            osw.write("========================================\n");
            osw.write("TVBox 解码器列表\n");
            osw.write("生成时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n");
            osw.write("设备型号: " + Build.MODEL + "\n");
            osw.write("Android版本: " + Build.VERSION.RELEASE + "\n");
            osw.write("SDK版本: " + Build.VERSION.SDK_INT + "\n");
            osw.write("解码器数量: " + decoders.size() + " 个\n");
            osw.write("========================================\n\n");

            // 写入解码器列表
            for (int i = 0; i < decoders.size(); i++) {
                osw.write((i + 1) + ". " + decoders.get(i) + "\n");
            }

            osw.close();
            fos.close();

            // 显示保存成功消息
            String message = "解码器列表已保存到:\n" + file.getAbsolutePath();
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

            // 可选：显示一个对话框，让用户可以选择打开文件
            new AlertDialog.Builder(getContext())
                    .setTitle("保存成功")
                    .setMessage(message)
                    .setPositiveButton("确定", null)
                    .setNeutralButton("打开文件夹", (dialog, which) -> {
                        openFileInExplorer(file.getParentFile());
                    })
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开文件管理器
     */
    private void openFileInExplorer(File directory) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(directory.getAbsolutePath());
            intent.setDataAndType(uri, "resource/folder");

            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                getContext().startActivity(intent);
            } else {
                Toast.makeText(getContext(), "未找到文件管理器应用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "无法打开文件夹", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取视频解码器列表
     */
    private List<String> getVideoDecoders() {
        List<String> decoders = new ArrayList<>();
        try {
            MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            MediaCodecInfo[] codecInfos = codecList.getCodecInfos();

            Set<String> addedDecoders = new HashSet<>();

            for (MediaCodecInfo codecInfo : codecInfos) {
                if (!codecInfo.isEncoder()) { // 只获取解码器
                    String[] types = codecInfo.getSupportedTypes();
                    for (String type : types) {
                        if (type.startsWith("video/")) {
                            String decoderName = codecInfo.getName();
                            if (!addedDecoders.contains(decoderName)) {
                                decoders.add(decoderName);
                                addedDecoders.add(decoderName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decoders;
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
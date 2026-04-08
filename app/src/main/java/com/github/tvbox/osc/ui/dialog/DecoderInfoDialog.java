package com.github.tvbox.osc.ui.dialog;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
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

import com.github.tvbox.osc.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DecoderInfoDialog extends BaseDialog {


    private static final int REQUEST_WRITE_STORAGE = 1001;
    private List<String> mDecoderList = new ArrayList<>();

    public DecoderInfoDialog(@NonNull Context context) {
        super(context);
//        this.mDecoderList = decoderList;
        setContentView(R.layout.dialog_decoder_info); // 使用新设计的布局
        initView();
    }

    private void initView() {

        // 0.获取视频解码器列表
        this.mDecoderList = getVideoDecoders();

        // 1. 设置解码器数量
        TextView countTextView = findViewById(R.id.tv_decoder_count);
        if (countTextView != null && mDecoderList != null) {
            countTextView.setText(String.format("共找到 %d 个视频解码器", mDecoderList.size()));
        }

        // 2. 配置ListView与适配器（关键：在此处调整每行高度）
        // 使用 getContext() 获取 Context
        Context context = getContext();

        // 获取颜色值
        int color = ContextCompat.getColor(context, R.color.color_FFFFFF);

        ListView listView = findViewById(R.id.lv_decoder_list);
        if (listView != null && mDecoderList != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                    android.R.layout.simple_list_item_1, mDecoderList){
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView textView = (TextView) view.findViewById(android.R.id.text1);
                    textView.setTextColor(color);
                    textView.setTextSize(14);
                    textView.setPadding(15, 5, 15, 5);
                    return view;
                }
            };
            listView.setAdapter(adapter);

            // 可选：直接设置ListView的项高度（另一个有效方法）
            // listView.setDividerHeight(dpToPx(1)); // 分隔线高度
            // 但通过适配器getView控制更灵活。
        }

        // 3. 设置保存按钮的点击事件
        Button saveButton = findViewById(R.id.btn_save);
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> {
                saveDecodersToFile(mDecoderList);
            });
        }
    }

    // 工具方法：将dp值转换为像素值
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
}
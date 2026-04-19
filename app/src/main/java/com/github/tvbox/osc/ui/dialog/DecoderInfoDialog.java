package com.github.tvbox.osc.ui.dialog;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
//import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
//import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AlertDialog;
//import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.tvbox.osc.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import tv.danmaku.ijk.media.player.IjkMediaCodecInfo;

public class DecoderInfoDialog extends BaseDialog {

    private static final int REQUEST_WRITE_STORAGE = 1001;
    private List<DecoderInfo> mDecoderInfoList = new ArrayList<>();

    public DecoderInfoDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_decoder_info);
        initView();
    }

    private void initView() {
        // 0. 只获取解码器列表
        this.mDecoderInfoList = getDecodersOnly();

        // 新增：对解码器列表进行排序
        if (mDecoderInfoList != null) {
            Collections.sort(mDecoderInfoList, new Comparator<DecoderInfo>() {
                @Override
                public int compare(DecoderInfo info1, DecoderInfo info2) {
                    // 1. 视频解码器优先于音频解码器
                    boolean isVideo1 = "视频".equals(info1.getMediaType());
                    boolean isVideo2 = "视频".equals(info2.getMediaType());
                    if (isVideo1 && !isVideo2) {
                        return -1; // info1(视频) 排在 info2(音频) 前面
                    } else if (!isVideo1 && isVideo2) {
                        return 1;  // info2(视频) 排在 info1(音频) 前面
                    } else if (isVideo1 && isVideo2) {
                        // 2. 同为视频解码器，按 IJK Rank 降序排列（Rank值大的在前）
                        return Integer.compare(info2.getRank(), info1.getRank());
                    } else {
                        // 3. 同为音频解码器，保持原始顺序或按其他规则排序，这里按名称排序
                        return info1.getName().compareTo(info2.getName());
                    }
                }
            });
        }

        // 1. 设置统计信息
        TextView countTextView = findViewById(R.id.tv_decoder_count);
        if (countTextView != null && mDecoderInfoList != null) {
            int videoCount = 0, audioCount = 0, hardwareCount = 0, softwareCount = 0, unknownCount = 0;
            for (DecoderInfo info : mDecoderInfoList) {
                if (info.getMediaType().equals("视频")) {
                    videoCount++;
                } else {
                    audioCount++;
                }

                int decodeType = info.getDecodeType();
                if (decodeType == 0) {
                    hardwareCount++;
                } else if (decodeType == 1) {
                    softwareCount++;
                } else {
                    unknownCount++;
                }
            }
            String summary = String.format("共发现 %d 个解码器 (视频: %d, 音频: %d, 硬解: %d, 软解: %d, 未知: %d)",
                    mDecoderInfoList.size(), videoCount, audioCount, hardwareCount, softwareCount, unknownCount);
            countTextView.setText(summary);
        }

        // 2. 配置 ListView 与自定义适配器
        ListView listView = findViewById(R.id.lv_decoder_list);
        if (listView != null && mDecoderInfoList != null) {
            DecoderAdapter adapter = new DecoderAdapter(getContext(), R.layout.item_decoder, mDecoderInfoList);
            listView.setAdapter(adapter);

            // 3. 添加点击事件显示详情
            listView.setOnItemClickListener((parent, view, position, id) -> {
                DecoderInfo decoderInfo = mDecoderInfoList.get(position);
                showDecoderDetail(decoderInfo);
            });
        }

        // 4. 设置保存按钮的点击事件
        Button saveButton = findViewById(R.id.btn_save);
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> {
                saveDecodersToFile(mDecoderInfoList);
            });
        }
    }

    /**
     * 显示解码器详情对话框
     */
    private void showDecoderDetail(DecoderInfo decoderInfo) {
        DecoderDetailDialog detailDialog = new DecoderDetailDialog(getContext(), decoderInfo);
        detailDialog.show();
    }

    /**
     * 只获取解码器（排除编码器）
     */
    private List<DecoderInfo> getDecodersOnly() {
        List<DecoderInfo> decoderInfoList = new ArrayList<>();
        try {
            MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            MediaCodecInfo[] codecInfos = codecList.getCodecInfos();

            for (MediaCodecInfo codecInfo : codecInfos) {
                // 只获取解码器
                if (!codecInfo.isEncoder()) {
                    decoderInfoList.add(new DecoderInfo(codecInfo));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decoderInfoList;
    }

    /**
     * 保存解码器信息到文件
     */
    private void saveDecodersToFile(List<DecoderInfo> decoderInfoList) {
        try {
            // 检查存储权限
            if (!checkStoragePermission()) {
                return;
            }

            // 创建文件名
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timeStamp = sdf.format(new Date());
            String fileName = "解码器列表_" + timeStamp + ".txt";

            // 获取保存目录
            File saveDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveDir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Decoders");
            } else {
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
            osw.write("解码器数量: " + decoderInfoList.size() + " 个\n");

            int videoCount = 0, audioCount = 0, hardwareCount = 0, softwareCount = 0, unknownCount = 0;
            for (DecoderInfo info : decoderInfoList) {
                if (info.getMediaType().equals("视频")) videoCount++; else audioCount++;
                int decodeType = info.getDecodeType();
                if (decodeType == 0) hardwareCount++;
                else if (decodeType == 1) softwareCount++;
                else unknownCount++;
            }
            osw.write(String.format("其中：视频 %d 个, 音频 %d 个, 硬解 %d 个, 软解 %d 个, 未知 %d 个\n",
                    videoCount, audioCount, hardwareCount, softwareCount, unknownCount));
            osw.write("========================================\n\n");

            // 写入详细的解码器列表
            for (int i = 0; i < decoderInfoList.size(); i++) {
                DecoderInfo info = decoderInfoList.get(i);
                osw.write((i + 1) + ". " + info.getName() + "\n");
                osw.write("   媒体类型: " + info.getMediaType() + "\n");
                osw.write("   解码方式: " + info.getDecodeTypeString() + "\n");
                osw.write("   最大码率: " + info.getMaxBitrateString() + "\n");
                // 新增：写入 IJK Rank
                if ("视频".equals(info.getMediaType()) && info.getRank() > 0) {
                    osw.write("   IJK优先级: " + info.getRankString() + "\n");
                }
                osw.write("   支持格式: ");
                String[] types = info.getSupportedTypes();
                for (int j = 0; j < types.length; j++) {
                    if (j > 0) osw.write(", ");
                    osw.write(types[j]);
                }
                osw.write("\n");
                osw.write("   详细信息: " + info.getDetailedInfo() + "\n");
                osw.write("----------------------------------------\n");
            }

            osw.close();
            fos.close();

            // 显示保存成功消息
            String message = "解码器列表已保存到:\n" + file.getAbsolutePath();
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 检查存储权限（兼容 Android 5.0+ 全版本）
    private boolean checkStoragePermission() {
        // 1. Android 6.0 以下不需要动态申请权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        // 2. Android 10+ 不需要申请 WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }

        // 3. Android 6.0 ~ Android 9.0 必须申请存储权限
        int permissionCheck = ContextCompat.checkSelfPermission(
                getContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        );

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Dialog 里正确申请权限的方式
            Activity activity = getDialogActivity();
            if (activity != null) {
                activity.requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_STORAGE // 你自己定义的常量，比如 1001
                );
            } else {
                Toast.makeText(getContext(), "无法获取Activity上下文", Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        return true;
    }

    // 辅助方法：从 Dialog 获取所属的 Activity
    private Activity getDialogActivity() {
        if (getContext() == null) {
            return null;
        }
        if (getContext() instanceof Activity) {
            return (Activity) getContext();
        } else if (getContext() instanceof ContextWrapper) {
            Context baseContext = ((ContextWrapper) getContext()).getBaseContext();
            if (baseContext instanceof Activity) {
                return (Activity) baseContext;
            }
        }
        return null;
    }

    /**
     * 通过解码器名称判断解码方式
     * @param decoderName 解码器名称
     * @return -1:未知, 0:硬解码, 1:软解码
     */
    public static int getDecodeTypeByName(String decoderName) {
        if (decoderName == null || decoderName.trim().isEmpty()) {
            return -1;
        }

        String lowerName = decoderName.toLowerCase().trim();

        // 按点分割字符串
        String[] parts = lowerName.split("\\.");

        // 检查是否是硬解码
        if (parts.length >= 2) {
            String firstPart = parts[0];
            String secondPart = parts[1];

            // 检查第一部分是否是omx或c2
            boolean isOmxOrC2 = "omx".equals(firstPart) || "c2".equals(firstPart);

            // 检查第二部分是否是芯片厂商
            if (isOmxOrC2 && CHIP_VENDORS.contains(secondPart)) {
                return 0;
            }
        }

        // 软解码规则
        for (String keyword : SOFTWARE_KEYWORDS) {
            if (lowerName.contains(keyword)) {
                return 1;
            }
        }

        // 未知
        return -1;
    }

    // 芯片厂商集合
    private static final Set<String> CHIP_VENDORS = new HashSet<>();
    static {
        // MStar 晨星
        CHIP_VENDORS.add("ms");
        CHIP_VENDORS.add("mstar");

        // 晶晨
        CHIP_VENDORS.add("amlogic");
        CHIP_VENDORS.add("aml");

        // 联发科
        CHIP_VENDORS.add("mtk");
        CHIP_VENDORS.add("mediatek");

        // 海思
        CHIP_VENDORS.add("hi");
        CHIP_VENDORS.add("hisi");
        CHIP_VENDORS.add("hisilicon");

        // 瑞芯微
        CHIP_VENDORS.add("rk");
        CHIP_VENDORS.add("rockchip");

        // 三星
        CHIP_VENDORS.add("exynos");
        CHIP_VENDORS.add("samsung");

        // 高通
        CHIP_VENDORS.add("qcom");
        CHIP_VENDORS.add("qualcomm");

        // 全志
        CHIP_VENDORS.add("allwinner");
        CHIP_VENDORS.add("sunxi");
        CHIP_VENDORS.add("aw");

        // 星宸
        CHIP_VENDORS.add("sigmastar");
        CHIP_VENDORS.add("ss");

        // 其他芯片厂商
        CHIP_VENDORS.add("intel");
        CHIP_VENDORS.add("nvidia");
        CHIP_VENDORS.add("realtek");
        CHIP_VENDORS.add("verisilicon");
    }

    // 软解关键词数组
    private static final String[] SOFTWARE_KEYWORDS = {
            "ffmpeg", "libav", "software", "soft", "sw",
            "openh264", "x264", "google", "android"
    };

    /**
     * 解码器信息类
     */
    static class DecoderInfo {
        private String name;
        private String canonicalName;
        private boolean isHardwareAccelerated;
        private boolean isSoftwareOnly;
        private boolean isVendor;
        private String[] supportedTypes;
        private int maxBitrate; // 最大码率(bps)
        private String mediaType; // 视频/音频
        private int decodeType; // -1:未知, 0:硬解码, 1:软解码
        private String detailedInfo; // 详细信息
        private int rank; // 新增：IJK 优先级评分

        public DecoderInfo(MediaCodecInfo codecInfo) {
            this.name = codecInfo.getName();
            this.supportedTypes = codecInfo.getSupportedTypes();

            // 判断媒体类型（视频/音频）
            this.mediaType = getMediaTypeFromSupportedTypes(supportedTypes);

            // 通过名称判断解码方式
            this.decodeType = getDecodeTypeByName(this.name);

            // 新增：计算 IJK Rank
            this.rank = calculateIjkRank(codecInfo);

            // API 29+ 的方法需要版本检查
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                this.canonicalName = codecInfo.getCanonicalName();
                this.isHardwareAccelerated = codecInfo.isHardwareAccelerated();
                this.isSoftwareOnly = codecInfo.isSoftwareOnly();
                this.isVendor = codecInfo.isVendor();

                // 如果API 29+，用系统API覆盖名称判断的结果
                if (this.isHardwareAccelerated) {
                    this.decodeType = 0; // 硬解码
                } else if (this.isSoftwareOnly) {
                    this.decodeType = 1; // 软解码
                }
            } else {
                this.canonicalName = this.name;
                this.isHardwareAccelerated = (this.decodeType == 0);
                this.isSoftwareOnly = (this.decodeType == 1);
                this.isVendor = false;
            }

            // 获取最大码率
            this.maxBitrate = getMaxBitrateFromCapabilities(codecInfo, supportedTypes);

            // 生成详细信息
            this.detailedInfo = generateDetailedInfo(codecInfo);
        }

        /**
         * 从支持的媒体类型判断是视频还是音频解码器
         */
        private String getMediaTypeFromSupportedTypes(String[] types) {
            for (String type : types) {
                if (type.startsWith("video/")) {
                    return "视频";
                } else if (type.startsWith("audio/")) {
                    return "音频";
                }
            }
            return "未知";
        }

        /**
         * 获取最大码率
         */
        private int getMaxBitrateFromCapabilities(MediaCodecInfo codecInfo, String[] supportedTypes) {
            int maxBitrate = 0;

            try {
                for (String mimeType : supportedTypes) {
                    MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);

                    if (mimeType.startsWith("video/")) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            MediaCodecInfo.VideoCapabilities videoCaps = capabilities.getVideoCapabilities();
                            if (videoCaps != null) {
                                int bitrate = videoCaps.getBitrateRange().getUpper();
                                if (bitrate > maxBitrate) {
                                    maxBitrate = bitrate;
                                }
                            }
                        }
                    } else if (mimeType.startsWith("audio/")) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            MediaCodecInfo.AudioCapabilities audioCaps = capabilities.getAudioCapabilities();
                            if (audioCaps != null) {
                                int bitrate = audioCaps.getBitrateRange().getUpper();
                                if (bitrate > maxBitrate) {
                                    maxBitrate = bitrate;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return maxBitrate;
        }

        /**
         * 计算当前解码器对于 video/avc 格式的 IJK 优先级评分
         * 仅对视频解码器且支持 avc 格式时计算，否则返回 -1
         */
        private int calculateIjkRank(MediaCodecInfo codecInfo) {
            // 仅在当前解码器是视频解码器时计算
            if (!"视频".equals(this.mediaType)) {
                return -1;
            }

            // 检查当前解码器是否支持 video/avc
            boolean supportsAvc = false;
            for (String type : this.supportedTypes) {
                if ("video/avc".equalsIgnoreCase(type)) {
                    supportsAvc = true;
                    break;
                }
            }
            if (!supportsAvc) {
                return -1;
            }

            // 调用 IjkMediaCodecInfo 的静态方法获取 Rank
            try {
                IjkMediaCodecInfo ijkInfo =
                        IjkMediaCodecInfo.setupCandidate(codecInfo, "video/avc");
                if (ijkInfo != null) {
                    return ijkInfo.mRank;
                } else {
                    return -1; // 获取失败返回-1
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1; // 发生异常时返回-1
            }
        }

        /**
         * 生成详细信息
         */
        private String generateDetailedInfo(MediaCodecInfo codecInfo) {
            StringBuilder details = new StringBuilder();

            details.append("名称: ").append(name).append("\n");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                details.append("规范名称: ").append(canonicalName).append("\n");
                details.append("硬件加速: ").append(isHardwareAccelerated).append("\n");
                details.append("纯软件: ").append(isSoftwareOnly).append("\n");
                details.append("供应商提供: ").append(isVendor).append("\n");
            }

            details.append("媒体类型: ").append(mediaType).append("\n");
            details.append("解码方式: ").append(getDecodeTypeString()).append("\n");
            details.append("最大码率: ").append(getMaxBitrateString()).append("\n");
            // 新增：添加 IJK Rank 信息只有在rank不为-1时才显示IJK优先级
            if ("视频".equals(mediaType) && getRank() > 0) {
                details.append("IJK优先级(Rank): ").append(getRankString()).append("\n");
            }

            // 获取并添加详细能力信息
            try {
                for (String mimeType : supportedTypes) {
                    details.append("\n格式: ").append(mimeType).append("\n");

                    MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);

                    // 颜色格式（视频）
                    if (capabilities.colorFormats != null && capabilities.colorFormats.length > 0) {
                        details.append("颜色格式: ");
                        for (int format : capabilities.colorFormats) {
                            details.append(formatToString(format)).append(", ");
                        }
                        details.append("\n");
                    }

                    // 视频能力
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        MediaCodecInfo.VideoCapabilities videoCaps = capabilities.getVideoCapabilities();
                        if (videoCaps != null) {
                            details.append("分辨率范围: ")
                                    .append(videoCaps.getSupportedWidths()).append("x")
                                    .append(videoCaps.getSupportedHeights()).append("\n");
                            details.append("帧率范围: ").append(videoCaps.getSupportedFrameRates()).append("\n");
                            details.append("比特率范围: ").append(videoCaps.getBitrateRange()).append("\n");
                        }

                        // 音频能力
                        MediaCodecInfo.AudioCapabilities audioCaps = capabilities.getAudioCapabilities();
                        if (audioCaps != null) {
                            details.append("采样率范围: ").append(audioCaps.getSupportedSampleRateRanges()).append("\n");
                            details.append("最大声道数: ").append(audioCaps.getMaxInputChannelCount()).append("\n");
                        }
                    }

//                    // 特性
//                    if (capabilities.features != null && capabilities.features.length > 0) {
//                        details.append("支持特性: ");
//                        for (String feature : capabilities.features) {
//                            details.append(feature).append(", ");
//                        }
//                        details.append("\n");
//                    }
                }
            } catch (Exception e) {
                details.append("获取详细能力失败: ").append(e.getMessage());
            }

            return details.toString();
        }

        private String formatToString(int colorFormat) {
            switch (colorFormat) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    return "YUV420Planar";
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    return "YUV420SemiPlanar";
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface:
                    return "Surface";
                default:
                    return "0x" + Integer.toHexString(colorFormat);
            }
        }



        public String getName() { return name; }
        public String getCanonicalName() { return canonicalName; }
        public String getMediaType() { return mediaType; }
        public int getDecodeType() { return decodeType; }
        public String getDecodeTypeString() {
            switch (decodeType) {
                case 0: return "硬解码";
                case 1: return "软解码";
                default: return "未知";
            }
        }
        public String getMaxBitrateString() {
            if (maxBitrate <= 0) return "未知";
            if (maxBitrate < 1000) return maxBitrate + " bps";
            if (maxBitrate < 1000000) return (maxBitrate / 1000) + " Kbps";
            return (maxBitrate / 1000000) + " Mbps";
        }
        public int getMaxBitrate() { return maxBitrate; }
        public String[] getSupportedTypes() { return supportedTypes; }
        public String getDetailedInfo() { return detailedInfo; }

        public int getRank() { return rank; }

        public String getRankString() {
            if (rank < 0) return "";
            return String.valueOf(rank);
        }
    }

    /**
     * 解码器列表适配器
     */
    private static class DecoderAdapter extends ArrayAdapter<DecoderInfo> {
        private int resourceId;

        public DecoderAdapter(Context context, int resource, List<DecoderInfo> objects) {
            super(context, resource, objects);
            resourceId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DecoderInfo decoderInfo = getItem(position);
            View view;
            ViewHolder viewHolder;

            if (convertView == null) {
                view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.tvDecoderName = view.findViewById(R.id.tvDecoderName);
                viewHolder.tvMediaType = view.findViewById(R.id.tvMediaType);
                viewHolder.tvDecodeType = view.findViewById(R.id.tvDecodeType);
//                viewHolder.tvMaxBitrate = view.findViewById(R.id.tvMaxBitrate);
                viewHolder.tvSupportedTypes = view.findViewById(R.id.tvSupportedTypes);
                view.setTag(viewHolder);
            } else {
                view = convertView;
                viewHolder = (ViewHolder) view.getTag();
            }

            viewHolder.tvDecoderName.setText(decoderInfo.getName());
//            viewHolder.tvMediaType.setText(decoderInfo.getMediaType() + " | " + decoderInfo.getMaxBitrateString());
            // 构建媒体类型显示文本
            String mediaTypeText = decoderInfo.getMediaType() + " | " + decoderInfo.getMaxBitrateString();

            // 如果是视频解码器且rank不为-1，才显示RANK
            if ("视频".equals(decoderInfo.getMediaType()) && decoderInfo.getRank() > -1) {
                mediaTypeText += " | RANK " + decoderInfo.getRankString();
            }
            viewHolder.tvMediaType.setText(mediaTypeText);
            viewHolder.tvDecodeType.setText(decoderInfo.getDecodeTypeString());
//            viewHolder.tvMaxBitrate.setText("最大码率: " + decoderInfo.getMaxBitrateString());

            // 显示支持的类型
            StringBuilder typesBuilder = new StringBuilder("支持格式: ");
            String[] supportedTypes = decoderInfo.getSupportedTypes();
            for (int i = 0; i < Math.min(supportedTypes.length, 3); i++) {
                if (i > 0) typesBuilder.append(", ");
                typesBuilder.append(supportedTypes[i]);
            }
            if (supportedTypes.length > 3) {
                typesBuilder.append("...等").append(supportedTypes.length).append("种格式");
            }
            viewHolder.tvSupportedTypes.setText(typesBuilder.toString());

            return view;
        }

        class ViewHolder {
            TextView tvDecoderName;
            TextView tvMediaType;
            TextView tvDecodeType;
//            TextView tvMaxBitrate;
            TextView tvSupportedTypes;
        }
    }
}
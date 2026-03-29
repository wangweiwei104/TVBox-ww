package com.github.tvbox.osc.player;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.text.TextUtils;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.PlayerHelper;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.player.misc.IjkTrackInfo;
import xyz.doikki.videoplayer.ijk.IjkPlayer;
import xyz.doikki.videoplayer.ijk.RawDataSourceProvider;

public class IjkmPlayer extends IjkPlayer {

    private IJKCode codec = null;

    public IjkmPlayer(Context context, IJKCode codec) {
        super(context);
        this.codec = codec;
    }

    @Override
    public void setOptions() {
        IJKCode codecTmp = this.codec == null ? ApiConfig.get().getCurrentIJKCode() : this.codec;
        LinkedHashMap<String, String> options = codecTmp.getOption();
        if (options != null) {
            for (String key : options.keySet()) {
                String value = options.get(key);
                String[] opt = key.split("\\|");
                int category = Integer.parseInt(opt[0].trim());
                String name = opt[1].trim();
                try {
                    long valLong = Long.parseLong(value);
                    mMediaPlayer.setOption(category, name, valLong);
                } catch (Exception e) {
                    mMediaPlayer.setOption(category, name, value);
                }
            }
        }
        //开启内置字幕
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "subtitle", 1);
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1);
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_timeout", -1);
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"safe",0);
        super.setOptions();
    }

    @Override
    public void setDataSource(String path, Map<String, String> headers) {
        try {
            if (path != null && !TextUtils.isEmpty(path)) {
                if(path.startsWith("rtsp")){
                    mMediaPlayer.setOption(1, "infbuf", 1); // 无限读
                    mMediaPlayer.setOption(1, "rtsp_transport", "tcp");
                    mMediaPlayer.setOption(1, "rtsp_flags", "prefer_tcp");
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "buffer_size", 1316);
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1L);
                    //  关闭播放器缓冲，这个必须关闭，否则会出现播放一段时间后，一直卡住，控制台打印 FFP_MSG_BUFFERING_START
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
                } else if (!path.contains(".m3u8") && (path.contains(".mp4") || path.contains(".mkv") || path.contains(".avi"))) {
                    if (Hawk.get(HawkConfig.IJK_CACHE_PLAY, false)) {
                        String cachePath = FileUtils.getExternalCachePath() + "/ijkcaches/";
                        String cacheMapPath = cachePath;
                        File cacheFile = new File(cachePath);
                        if (!cacheFile.exists()) cacheFile.mkdirs();
                        String tmpMd5 = MD5.string2MD5(path);
                        cachePath += tmpMd5 + ".file";
                        cacheMapPath += tmpMd5 + ".map";
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_file_path", cachePath);
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_map_path", cacheMapPath);
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "parse_cache_map", 1);
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "auto_save_map", 1);
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_max_capacity", 60 * 1024 * 1024);
                        path = "ijkio:cache:ffio:" + path;
                    }
                } else if (isMulticastToUnicastHttpStrict(path)){ //如果是组播转单播的链接，进行特殊优化，加快换台速度
                    //控制播放器分析媒体流信息的时间长度（单位：微秒）
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 500000L);
                    //控制播放器探测数据的大小（单位：字节）
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 32768L);
                    // 关闭包缓冲，减少延迟
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0L);
                    // 立即刷新数据包
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1L);
                    // 启用丢帧处理，保证流畅性
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1L);
                    // 跳过非参考帧的环路滤波，降低CPU占用
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48L);
                    // 设置最大分析时长
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", 100L);
                    // 开启无限缓冲区模式（适合直播场景）
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1L);
                }
            }
            setDataSourceHeader(headers);
        } catch (Exception e) {
            mPlayerEventListener.onError(-1, PlayerHelper.getRootCauseMessage(e));
        }
        //mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", "ijkio,ffio,async,cache,crypto,file,http,https,ijkhttphook,ijkinject,ijklivehook,ijklongurl,ijksegment,ijktcphook,pipe,rtp,tcp,tls,udp,ijkurlhook,data");
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", "ijkio,ffio,async,cache,crypto,file,dash,http,https,ijkhttphook,ijkinject,ijklivehook,ijklongurl,ijksegment,ijktcphook,pipe,rtp,tcp,tls,udp,ijkurlhook,data,concat,subfile,ffconcat");
        //mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0L);
//        try {
//            path = encodeSpaceChinese(path);//会导致本地文件无法播放，故注释掉
//        } catch (Exception ignored) {
//
//        }
        super.setDataSource(path, headers);
    }

    public static boolean isMulticastToUnicastHttpStrict(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // 第一步：基本格式检查
        if (!url.matches("(?i)^https?://.*")) {
            return false;
        }

        // 第二步：提取路径中的IP地址
        Pattern ipPattern = Pattern.compile("/(rtp|udp)/(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?::\\d+)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = ipPattern.matcher(url);

        if (!matcher.find()) {
            return false;
        }

        // 第三步：验证IP地址
        String ip = matcher.group(2);
        return isValidMulticastIP(ip);
    }

    /**
     * 验证是否为合法的D类组播IP地址
     */
    private static boolean isValidMulticastIP(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            int first = Integer.parseInt(parts[0]);
//            int second = Integer.parseInt(parts[1]);
//            int third = Integer.parseInt(parts[2]);
//            int fourth = Integer.parseInt(parts[3]);

            // D类地址范围: 224.0.0.0 - 239.255.255.255
            // 第一个字节必须在224-239之间
            if (first < 224 || first > 239) {
                return false;
            }

            // 验证每个字节都在0-255范围内
            for (int i = 0; i < 4; i++) {
                int octet = Integer.parseInt(parts[i]);
                if (octet < 0 || octet > 255) {
                    return false;
                }
            }

            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }
    private String encodeSpaceChinese(String str) throws UnsupportedEncodingException {
        Pattern p = Pattern.compile("[\u4e00-\u9fa5 ]+");
        Matcher m = p.matcher(str);
        StringBuffer b = new StringBuffer();
        while (m.find()) m.appendReplacement(b, URLEncoder.encode(m.group(0), "UTF-8"));
        m.appendTail(b);
        return b.toString();
    }

    @Override
    public void setDataSource(AssetFileDescriptor fd) {
        try {
            mMediaPlayer.setDataSource(new RawDataSourceProvider(fd));
        } catch (Exception e) {
            mPlayerEventListener.onError(-1, PlayerHelper.getRootCauseMessage(e));
        }
    }
    private void setDataSourceHeader(Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            String userAgent = headers.get("User-Agent");
            if (!TextUtils.isEmpty(userAgent)) {
                mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user_agent", userAgent);
                // 移除header中的User-Agent，防止重复
                headers.remove("User-Agent");
            }
            if (headers.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    sb.append(entry.getKey());
                    sb.append(":");
                    String value = entry.getValue();
                    if (!TextUtils.isEmpty(value))
                        sb.append(entry.getValue());
                    sb.append("\r\n");
                    mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "headers", sb.toString());
                }
            }
        }
    }
    public TrackInfo getTrackInfo() {
        IjkTrackInfo[] trackInfo = mMediaPlayer.getTrackInfo();
        if (trackInfo == null) return null;
        TrackInfo data = new TrackInfo();
        int subtitleSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT);
        int audioSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO);
        int index = 0;
        for (IjkTrackInfo info : trackInfo) {
            if (info.getTrackType() == ITrackInfo.MEDIA_TRACK_TYPE_AUDIO) {//音轨信息
                String trackName = (data.getAudio().size() + 1) + "：" + info.getInfoInline();
                TrackInfoBean t = new TrackInfoBean();
                t.name = trackName;
                t.language = info.getLanguage();
                t.trackId = index;
                t.selected = index == audioSelected;
                data.addAudio(t);
            }
            if (info.getTrackType() == ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {//内置字幕
                String trackName = (data.getSubtitle().size() + 1) + "：" + info.getInfoInline();
                TrackInfoBean t = new TrackInfoBean();
                t.name = trackName;
                t.language = info.getLanguage();
                t.trackId = index;
                t.selected = index == subtitleSelected;
                data.addSubtitle(t);
            }
            index++;
        }
        return data;
    }

    public void setTrack(int trackIndex) {
        mMediaPlayer.selectTrack(trackIndex);
    }

    public void setOnTimedTextListener(IMediaPlayer.OnTimedTextListener listener) {
        mMediaPlayer.setOnTimedTextListener(listener);
    }

}

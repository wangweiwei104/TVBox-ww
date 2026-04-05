package com.github.tvbox.osc.util;

import android.content.Context;

import androidx.media3.exoplayer.DefaultRenderersFactory;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.IJKCode;
import com.orhanobut.hawk.Hawk;

import java.util.List;

import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory;

public class HawkUtils {

    private static final String DANMU_OPEN = "danmu_open";
    private static final String DANMU_MAXLINE = "danmu_maxline";
    private static final String DANMU_SPEED = "danmu_speed";
    private static final String DANMU_ALPHA = "danmu_alpha";
    private static final String DANMU_SIZESCALE = "danmu_sizescale";
    private static final String DANMU_COLOR = "danmu_color";

    public static boolean getDanmuOpen() {
        return Hawk.get(DANMU_OPEN, true);
    }

    public static void setDanmuOpen(boolean danmuOpen) {
        Hawk.put(DANMU_OPEN, danmuOpen);
    }

    public static int getDanmuMaxLine() {
        return Hawk.get(DANMU_MAXLINE, 3);
    }

    public static void setDanmuMaxLine(int danmuMaxLine) {
        Hawk.put(DANMU_MAXLINE,danmuMaxLine);
    }

    public static float getDanmuSpeed() {
        return Hawk.get(DANMU_SPEED, 1.5f);
    }

    public static void setDanmuSpeed(float danmuSpeed) {
        Hawk.put(DANMU_SPEED,danmuSpeed);
    }

    public static float getDanmuAlpha() {
        return Hawk.get(DANMU_ALPHA, 90 / 100.0f);
    }

    public static void setDanmuAlpha(float danmuAlpha) {
        Hawk.put(DANMU_ALPHA,danmuAlpha);
    }

    public static float getDanmuSizeScale() {
        return Hawk.get(DANMU_SIZESCALE, 0.8f);
    }

    public static void setDanmuSizeScale(float danmuSizeScale) {
        Hawk.put(DANMU_SIZESCALE,danmuSizeScale);
    }
    public static boolean getDanmuColor() {
        return Hawk.get(DANMU_COLOR, false);
    }
    public static void setDanmuColor(boolean color) {
        Hawk.put(DANMU_COLOR,color);
    }
    public static String getIJKCodec() {
        return Hawk.get(HawkConfig.IJK_CODEC, "");
    }

    public static void nextIJKCodec() {
        List<IJKCode> ijkCodes = ApiConfig.get().getIjkCodes();
        String ijkCodec = getIJKCodec();
        int index = 0;
        for (int i = 0; i < ijkCodes.size(); i++) {
            IJKCode ijkCode = ijkCodes.get(i);
            if (ijkCode.getName().equals(ijkCodec)) {
                index = i;
                break;
            }
        }
        ijkCodes.get(index).selected(false);
        index++;
        index %= ijkCodes.size();
        ijkCodes.get(index).selected(true);
    }

    public static boolean getIJKCache() {
        return Hawk.get(HawkConfig.IJK_CACHE_PLAY, false);
    }

    public static void nextIJKCache() {
        boolean ijkCache = getIJKCache();
        Hawk.put(HawkConfig.IJK_CACHE_PLAY, !ijkCache);
    }

    public static String getIJKCacheDesc() {
        return getIJKCache() ? "开启" : "关闭";
    }

    // 在 HawkUtils 类中添加以下方法

    // 分析时长 - 索引获取与切换
    public static int getIJKAnalyzeDuration() {
        return Hawk.get(HawkConfig.IJK_ANALYZE_DURATION, 0); // 默认索引为0，即“默认”选项
    }

    public static void nextIJKAnalyzeDuration() {
        int index = getIJKAnalyzeDuration();
        App app = App.getInstance();
        String[] array = app.getResources().getStringArray(R.array.media_content_IjkPlayer_analyzeduration);
        index++;
        index %= array.length;
        Hawk.put(HawkConfig.IJK_ANALYZE_DURATION, index);
    }

    public static String getIJKAnalyzeDurationDesc() {
        App app = App.getInstance();
        String[] array = app.getResources().getStringArray(R.array.media_content_IjkPlayer_analyzeduration);
        return array[getIJKAnalyzeDuration()];
    }

    // 分析时长 - 返回实际的微秒值，如果选中“默认”则返回-1
    public static long getIJKAnalyzeDurationActualValue() {
        int index = getIJKAnalyzeDuration();
        if (index == 0) {
            return -1; // 特殊值，表示“默认”，不应设置此选项
        }
        // 索引 1-8 对应：0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2
        float[] values = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
        return (long)(values[index - 1] * 1000000L); // 转换为微秒
    }

    // 探测大小 - 索引获取与切换
    public static int getIJKProbeSize() {
        return Hawk.get(HawkConfig.IJK_PROBE_SIZE, 0); // 默认索引为0，即“默认”选项
    }

    public static void nextIJKProbeSize() {
        int index = getIJKProbeSize();
        App app = App.getInstance();
        String[] array = app.getResources().getStringArray(R.array.media_content_IjkPlayer_probesize);
        index++;
        index %= array.length;
        Hawk.put(HawkConfig.IJK_PROBE_SIZE, index);
    }

    public static String getIJKProbeSizeDesc() {
        App app = App.getInstance();
        String[] array = app.getResources().getStringArray(R.array.media_content_IjkPlayer_probesize);
        return array[getIJKProbeSize()];
    }

    // 探测大小 - 返回实际的字节值，如果选中“默认”则返回-1
    public static long getIJKProbeSizeActualValue() {
        int index = getIJKProbeSize();
        if (index == 0) {
            return -1; // 特殊值，表示“默认”，不应设置此选项
        }
        // 索引 1-8 对应：8KB, 16KB, 32KB, 64KB, 128KB, 256KB, 512KB, 1MB
        long[] values = {
                8L * 1024,      // 8KB
                16L * 1024,     // 16KB
                32L * 1024,     // 32KB
                64L * 1024,     // 64KB
                128L * 1024,    // 128KB
                256L * 1024,    // 256KB
                512L * 1024,    // 512KB
                1024L * 1024    // 1MB
        };
        return values[index - 1];
    }

    /**
     * 获取exo渲染器 自己存储的数据
     *
     * @return int
     */
    public static int getExoRenderer() {
        return Hawk.get(HawkConfig.EXO_RENDERER, 0);
    }

    public static void nextExoRenderer() {
        App app = App.getInstance();
        String[] array = app.getResources().getStringArray(R.array.media_content_ExoPlayer_renderer);
        int renderer = getExoRenderer();
        renderer++;
        renderer %= array.length;
        Hawk.put(HawkConfig.EXO_RENDERER, renderer);
    }

    /**
     * 创建exo渲染器
     *
     * @param context 上下文
     * @return {@link DefaultRenderersFactory }
     */
    public static DefaultRenderersFactory createExoRendererActualValue(Context context) {
        int renderer = getExoRenderer();
        switch (renderer) {
            case 1:
                return new NextRenderersFactory(context);
            case 0:
            default:
                return new DefaultRenderersFactory(context);
        }
    }

    /**
     * 获取exo渲染器描述
     *
     * @return {@link String }
     */
    public static String getExoRendererDesc() {
        App app = App.getInstance();
        String[] array = app.getResources().getStringArray(R.array.media_content_ExoPlayer_renderer);
        return array[getExoRenderer()];
    }

    /**
     * 获取exo渲染器模式 自己存储的 值
     *
     * @return int
     */
    public static int getExoRendererMode() {
        return Hawk.get(HawkConfig.EXO_RENDERER_MODE, 1);
    }

    public static void nextExoRendererMode() {
        int rendererMode = getExoRendererMode();
        App app = App.getInstance();
        String[] array = app.getResources().getStringArray(R.array.media_content_ExoPlayer_renderer_mode);
        rendererMode++;
        rendererMode %= array.length;
        Hawk.put(HawkConfig.EXO_RENDERER_MODE, rendererMode);
    }


    /**
     * 返回程序 需要的值 exo渲染器模式
     */
    public static int getExoRendererModeActualValue() {
        int i = getExoRendererMode();
        switch (i) {
            case 0:
                return DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;
            case 2:
                return DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
            case 1:
            default:
                return DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;
        }
    }

    /**
     * 获取exo渲染器模式描述
     *
     * @return {@link String }
     */
    public static String getExoRendererModeDesc() {
        App app = App.getInstance();
        String[] array = app.getResources().getStringArray(R.array.media_content_ExoPlayer_renderer_mode);
        return array[getExoRendererMode()];
    }

    // Vod 播放器首选
    public static int getVodPlayerPreferred() {
        return Hawk.get(HawkConfig.VOD_PLAYER_PREFERRED, 0);
    }

    public static void nextVodPlayerPreferred() {
        int index = getVodPlayerPreferred();
        App app = App.getInstance();
        String[] array = app.getResources().getStringArray(R.array.media_content_General_VodPlayerPreferred);
        index++;
        index %= array.length;
        Hawk.put(HawkConfig.VOD_PLAYER_PREFERRED, index);
    }

    public static boolean getVodPlayerPreferredConfigurationFile() {
        int i = getVodPlayerPreferred();
        return i == 0;
    }

    public static String getVodPlayerPreferredDesc() {
        App app = App.getInstance();
        String[] array = app.getResources().getStringArray(R.array.media_content_General_VodPlayerPreferred);
        return array[getVodPlayerPreferred()];
    }

    public static String getLastLiveChannelGroup() {
        return Hawk.get(HawkConfig.LIVE_CHANNEL_GROUP, "");
    }

    public static void setLastLiveChannelGroup(String group) {
        Hawk.put(HawkConfig.LIVE_CHANNEL_GROUP, group);
    }

    public static String getLastLiveChannel() {
        return Hawk.get(HawkConfig.LIVE_CHANNEL, "");
    }

    public static void setLastLiveChannel(String channel) {
        Hawk.put(HawkConfig.LIVE_CHANNEL, channel);
    }
}

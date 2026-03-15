package com.didadida.danmaku.constant;

/**
 * 弹幕Redis键常量
 */
public class DanmakuRedisKey {

    /**
     * 视频弹幕列表前缀
     * 格式: danmaku:video:{videoId}:list
     */
    public static final String DANMAKU_VIDEO_LIST_PREFIX = "danmaku:video:";

    /**
     * 视频弹幕列表后缀
     */
    public static final String DANMAKU_VIDEO_LIST_SUFFIX = ":list";

    /**
     * 视频弹幕计数前缀
     * 格式: danmaku:video:{videoId}:count
     */
    public static final String DANMAKU_VIDEO_COUNT_PREFIX = "danmaku:video:";

    /**
     * 视频弹幕计数后缀
     */
    public static final String DANMAKU_VIDEO_COUNT_SUFFIX = ":count";

    /**
     * 构建视频弹幕列表键
     * @param videoId 视频ID
     * @return Redis键
     */
    public static String buildDanmakuVideoListKey(Long videoId) {
        return DANMAKU_VIDEO_LIST_PREFIX + videoId + DANMAKU_VIDEO_LIST_SUFFIX;
    }

    /**
     * 构建视频弹幕计数键
     * @param videoId 视频ID
     * @return Redis键
     */
    public static String buildDanmakuVideoCountKey(Long videoId) {
        return DANMAKU_VIDEO_COUNT_PREFIX + videoId + DANMAKU_VIDEO_COUNT_SUFFIX;
    }
}

package com.didadida.video.constant;

/**
 * 视频模块Redis Key常量
 */
public class VideoRedisKey {

    /**
     * 视频元信息缓存前缀
     * 格式：video:info:{videoId}
     */
    public static final String VIDEO_INFO_PREFIX = "video:info:";

    /**
     * 视频播放量缓存前缀
     * 格式：video:view:count:{videoId}
     */
    public static final String VIDEO_VIEW_COUNT_PREFIX = "video:view:count:";

    /**
     * 视频点赞数缓存前缀
     * 格式：video:like:count:{videoId}
     */
    public static final String VIDEO_LIKE_COUNT_PREFIX = "video:like:count:";

    /**
     * 视频投币数缓存前缀
     * 格式：video:coin:count:{videoId}
     */
    public static final String VIDEO_COIN_COUNT_PREFIX = "video:coin:count:";

    /**
     * 视频收藏数缓存前缀
     * 格式：video:collect:count:{videoId}
     */
    public static final String VIDEO_COLLECT_COUNT_PREFIX = "video:collect:count:";

    /**
     * 视频分享数缓存前缀
     * 格式：video:share:count:{videoId}
     */
    public static final String VIDEO_SHARE_COUNT_PREFIX = "video:share:count:";

    /**
     * 用户点赞记录前缀
     * 格式：video:user:like:{userId}:{videoId}
     */
    public static final String VIDEO_USER_LIKE_PREFIX = "video:user:like:";

    /**
     * 用户收藏记录前缀
     * 格式：video:user:collect:{userId}:{videoId}
     */
    public static final String VIDEO_USER_COLLECT_PREFIX = "video:user:collect:";

    /**
     * 视频元信息缓存过期时间（小时）
     */
    public static final long VIDEO_INFO_EXPIRE_HOUR = 24;

    /**
     * 互动数据过期时间（天）
     */
    public static final long INTERACTION_EXPIRE_DAY = 7;

    /**
     * 播放量同步Kafka Topic
     */
    public static final String VIDEO_VIEW_SYNC_TOPIC = "video_view_sync_topic";

    /**
     * 互动数据同步Kafka Topic
     */
    public static final String VIDEO_INTERACTION_SYNC_TOPIC = "video_interaction_sync_topic";
}

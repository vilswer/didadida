package com.didadida.interaction.constant;

/**
 * 评论Redis键常量
 */
public class CommentRedisKey {

    /**
     * 视频评论列表缓存
     * 格式: comment:video:{videoId}:list
     */
    public static final String VIDEO_COMMENT_LIST = "comment:video:%s:list";

    /**
     * 评论回复列表缓存
     * 格式: comment:replies:{commentId}:list
     */
    public static final String COMMENT_REPLIES_LIST = "comment:replies:%s:list";

    /**
     * 评论点赞数缓存
     * 格式: comment:like:count:{commentId}
     */
    public static final String COMMENT_LIKE_COUNT = "comment:like:count:%s";

    /**
     * 评论回复数缓存
     * 格式: comment:reply:count:{commentId}
     */
    public static final String COMMENT_REPLY_COUNT = "comment:reply:count:%s";

    /**
     * 用户评论点赞状态
     * 格式: comment:like:status:{userId}:{commentId}
     */
    public static final String COMMENT_LIKE_STATUS = "comment:like:status:%s:%s";

    /**
     * 评论缓存
     * 格式: comment:info:{commentId}
     */
    public static final String COMMENT_INFO = "comment:info:%s";

    /**
     * 评论消息队列
     */
    public static final String COMMENT_MESSAGE_QUEUE = "comment:message:queue";

}

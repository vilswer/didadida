package com.didadida.interaction.dto;

import lombok.Data;

/**
 * 评论消息DTO类
 */
@Data
public class CommentMessageDTO {

    /**
     * 操作类型：add, like, unlike, delete
     */
    private String operationType;

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 父评论ID
     */
    private Long parentId;

    /**
     * 回复的用户ID
     */
    private Long replyUserId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 时间戳
     */
    private Long timestamp;

}

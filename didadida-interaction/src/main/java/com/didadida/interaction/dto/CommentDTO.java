package com.didadida.interaction.dto;

import lombok.Data;

/**
 * 评论DTO类
 */
@Data
public class CommentDTO {

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 父评论ID（0表示一级评论）
     */
    private Long parentId;

    /**
     * 回复的用户ID（仅二级评论有效）
     */
    private Long replyUserId;

    /**
     * 评论内容
     */
    private String content;
}

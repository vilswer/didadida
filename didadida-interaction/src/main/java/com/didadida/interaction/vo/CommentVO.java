package com.didadida.interaction.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论VO类
 */
@Data
public class CommentVO {

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 评论者ID
     */
    private Long userId;

    /**
     * 评论者昵称
     */
    private String nickname;

    /**
     * 评论者头像
     */
    private String avatar;

    /**
     * 父评论ID（0表示一级评论）
     */
    private Long parentId;

    /**
     * 回复的用户ID（仅二级评论有效）
     */
    private Long replyUserId;

    /**
     * 回复的用户昵称（仅二级评论有效）
     */
    private String replyUserNickname;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 回复数
     */
    private Integer replyCount;

    /**
     * 评论时间
     */
    private LocalDateTime createTime;

    /**
     * 当前用户是否点赞
     */
    private Boolean isLiked;
}

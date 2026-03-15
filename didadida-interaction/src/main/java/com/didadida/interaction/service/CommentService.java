package com.didadida.interaction.service;

import com.didadida.interaction.dto.CommentDTO;
import com.didadida.interaction.vo.CommentVO;

import java.util.List;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 发表评论
     * @param commentDTO 评论数据
     * @param userId 用户ID
     * @return 评论ID
     */
    Long addComment(CommentDTO commentDTO, Long userId);

    /**
     * 获取视频的一级评论列表
     * @param videoId 视频ID
     * @param page 页码
     * @param size 每页大小
     * @param currentUserId 当前用户ID（可选，用于判断是否点赞）
     * @return 评论列表
     */
    List<CommentVO> getVideoComments(Long videoId, Integer page, Integer size, Long currentUserId);

    /**
     * 获取评论的回复列表
     * @param commentId 评论ID
     * @param page 页码
     * @param size 每页大小
     * @param currentUserId 当前用户ID（可选，用于判断是否点赞）
     * @return 回复列表
     */
    List<CommentVO> getCommentReplies(Long commentId, Integer page, Integer size, Long currentUserId);

    /**
     * 点赞评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否点赞成功
     */
    boolean likeComment(Long commentId, Long userId);

    /**
     * 取消点赞评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否取消成功
     */
    boolean unlikeComment(Long commentId, Long userId);

    /**
     * 删除评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteComment(Long commentId, Long userId);
}

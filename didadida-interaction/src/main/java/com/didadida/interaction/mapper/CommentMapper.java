package com.didadida.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.didadida.interaction.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论Mapper接口
 */
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 根据视频ID查询一级评论列表
     * @param videoId 视频ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 评论列表
     */
    List<Comment> selectFirstLevelComments(@Param("videoId") Long videoId, @Param("offset") Long offset, @Param("limit") Integer limit);

    /**
     * 根据父评论ID查询回复列表
     * @param parentId 父评论ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 回复列表
     */
    List<Comment> selectRepliesByParentId(@Param("parentId") Long parentId, @Param("offset") Long offset, @Param("limit") Integer limit);

    /**
     * 增加评论的回复数
     * @param commentId 评论ID
     */
    void incrementReplyCount(@Param("commentId") Long commentId);

    /**
     * 增加评论的点赞数
     * @param commentId 评论ID
     */
    void incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 减少评论的点赞数
     * @param commentId 评论ID
     */
    void decrementLikeCount(@Param("commentId") Long commentId);
}

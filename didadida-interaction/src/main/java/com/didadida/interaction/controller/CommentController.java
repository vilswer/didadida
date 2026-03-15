package com.didadida.interaction.controller;

import com.didadida.interaction.dto.CommentDTO;
import com.didadida.interaction.service.CommentService;
import com.didadida.interaction.vo.CommentVO;
import com.didadida.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/interaction/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 发表评论
     * @param commentDTO 评论数据
     * @param userId 用户ID
     * @return 评论ID
     */
    @PostMapping
    public Result<Long> addComment(@RequestBody CommentDTO commentDTO, @RequestParam Long userId) {
        Long commentId = commentService.addComment(commentDTO, userId);
        return Result.success("评论成功", commentId);
    }

    /**
     * 获取视频的评论列表
     * @param videoId 视频ID
     * @param page 页码
     * @param size 每页大小
     * @param userId 当前用户ID（可选）
     * @return 评论列表
     */
    @GetMapping("/video/{videoId}")
    public Result<List<CommentVO>> getVideoComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId) {
        List<CommentVO> comments = commentService.getVideoComments(videoId, page, size, userId);
        return Result.success(comments);
    }

    /**
     * 获取评论的回复列表
     * @param commentId 评论ID
     * @param page 页码
     * @param size 每页大小
     * @param userId 当前用户ID（可选）
     * @return 回复列表
     */
    @GetMapping("/replies/{commentId}")
    public Result<List<CommentVO>> getCommentReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId) {
        List<CommentVO> replies = commentService.getCommentReplies(commentId, page, size, userId);
        return Result.success(replies);
    }

    /**
     * 点赞评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/like/{commentId}")
    public Result<Boolean> likeComment(@PathVariable Long commentId, @RequestParam Long userId) {
        boolean success = commentService.likeComment(commentId, userId);
        return Result.success(success ? "点赞成功" : "已经点赞过", success);
    }

    /**
     * 取消点赞评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/unlike/{commentId}")
    public Result<Boolean> unlikeComment(@PathVariable Long commentId, @RequestParam Long userId) {
        boolean success = commentService.unlikeComment(commentId, userId);
        return Result.success(success ? "取消点赞成功" : "没有点赞过", success);
    }

    /**
     * 删除评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/{commentId}")
    public Result<Boolean> deleteComment(@PathVariable Long commentId, @RequestParam Long userId) {
        boolean success = commentService.deleteComment(commentId, userId);
        return Result.success(success ? "删除成功" : "删除失败", success);
    }
}

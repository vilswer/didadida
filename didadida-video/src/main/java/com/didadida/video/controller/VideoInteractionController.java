package com.didadida.video.controller;

import com.didadida.common.result.Result;
import com.didadida.video.service.VideoService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/interaction")
@RequiredArgsConstructor
@Validated
public class VideoInteractionController {

    private final VideoService videoService;

    /**
     * 点赞视频
     * @param videoId 视频ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/like/{videoId}")
    public Result<Void> likeVideo(
            @PathVariable @NotNull(message = "视频ID不能为空") Long videoId,
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId
    ) {
        videoService.likeVideo(videoId, userId);
        return Result.success();
    }

    /**
     * 取消点赞
     * @param videoId 视频ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/unlike/{videoId}")
    public Result<Void> unlikeVideo(
            @PathVariable @NotNull(message = "视频ID不能为空") Long videoId,
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId
    ) {
        videoService.unlikeVideo(videoId, userId);
        return Result.success();
    }

    /**
     * 投币视频
     * @param videoId 视频ID
     * @param userId 用户ID
     * @param coinCount 投币数量
     * @return 操作结果
     */
    @PostMapping("/coin/{videoId}")
    public Result<Void> coinVideo(
            @PathVariable @NotNull(message = "视频ID不能为空") Long videoId,
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId,
            @RequestParam @NotNull(message = "投币数量不能为空") Integer coinCount
    ) {
        videoService.coinVideo(videoId, userId, coinCount);
        return Result.success();
    }

    /**
     * 收藏视频
     * @param videoId 视频ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/collect/{videoId}")
    public Result<Void> collectVideo(
            @PathVariable @NotNull(message = "视频ID不能为空") Long videoId,
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId
    ) {
        videoService.collectVideo(videoId, userId);
        return Result.success();
    }

    /**
     * 取消收藏
     * @param videoId 视频ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/uncollect/{videoId}")
    public Result<Void> uncollectVideo(
            @PathVariable @NotNull(message = "视频ID不能为空") Long videoId,
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId
    ) {
        videoService.uncollectVideo(videoId, userId);
        return Result.success();
    }

    /**
     * 分享视频
     * @param videoId 视频ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/share/{videoId}")
    public Result<Void> shareVideo(
            @PathVariable @NotNull(message = "视频ID不能为空") Long videoId,
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId
    ) {
        videoService.shareVideo(videoId, userId);
        return Result.success();
    }

    /**
     * 同步互动数据到MySQL（管理员操作）
     * @return 操作结果
     */
    @PostMapping("/sync")
    public Result<Void> syncInteraction() {
        videoService.batchSyncInteractionToMysql();
        return Result.success();
    }
}
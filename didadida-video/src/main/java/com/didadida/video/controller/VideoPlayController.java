package com.didadida.video.controller;

import com.didadida.common.result.Result;
import com.didadida.video.service.VideoService;
import com.didadida.video.vo.VideoEpisodeVO;
import com.didadida.video.vo.VideoPlayDetailVO;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/play")
@RequiredArgsConstructor
@Validated
public class VideoPlayController {

    private final VideoService videoService;

    /**
     * 获取视频播放详情（核心接口，前端打开视频页调用）
     * @param videoId 视频ID
     * @return 视频播放详情（含播放地址）
     */
    @GetMapping("/detail/{videoId}")
    public Result<VideoPlayDetailVO> getVideoPlayDetail(
            @PathVariable @NotNull(message = "视频ID不能为空") Long videoId
    ) {
        VideoPlayDetailVO playDetail = videoService.getVideoPlayDetail(videoId);
        return Result.success(playDetail);
    }

    /**
     * 上报视频播放量（前端视频开始播放时调用）
     * @param videoId 视频ID
     * @return 上报结果
     */
    @PostMapping("/report/{videoId}")
    public Result<Void> reportVideoView(
            @PathVariable @NotNull(message = "视频ID不能为空") Long videoId
    ) {
        videoService.reportVideoView(videoId);
        return Result.success();
    }

    /**
     * 获取视频选集列表
     * @param videoId 视频ID
     * @return 选集列表
     */
    @GetMapping("/episodes/{videoId}")
    public Result<List<VideoEpisodeVO>> getVideoEpisodes(
            @PathVariable @NotNull(message = "视频ID不能为空") Long videoId
    ) {
        List<VideoEpisodeVO> episodes = videoService.getVideoEpisodes(videoId);
        return Result.success(episodes);
    }

    /**
     * 获取视频选集详情
     * @param episodeId 选集ID
     * @return 选集详情（含播放地址）
     */
    @GetMapping("/episode/detail/{episodeId}")
    public Result<VideoPlayDetailVO> getVideoEpisodeDetail(
            @PathVariable @NotNull(message = "选集ID不能为空") Long episodeId
    ) {
        VideoPlayDetailVO episodeDetail = videoService.getVideoEpisodeDetail(episodeId);
        return Result.success(episodeDetail);
    }

    /**
     * 获取相关视频推荐
     * @param videoId 视频ID
     * @param limit 推荐数量，默认10
     * @return 推荐视频列表
     */
    @GetMapping("/related/{videoId}")
    public Result<List<VideoPlayDetailVO>> getRelatedVideos(
            @PathVariable @NotNull(message = "视频ID不能为空") Long videoId,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<VideoPlayDetailVO> relatedVideos = videoService.getRelatedVideos(videoId, limit);
        return Result.success(relatedVideos);
    }
}

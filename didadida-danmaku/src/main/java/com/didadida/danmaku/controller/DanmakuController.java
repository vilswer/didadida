package com.didadida.danmaku.controller;

import com.didadida.danmaku.dto.DanmakuDTO;
import com.didadida.danmaku.service.DanmakuService;
import com.didadida.danmaku.vo.DanmakuVO;
import com.didadida.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 弹幕控制器
 */
@RestController
@RequestMapping("/danmaku")
public class DanmakuController {

    @Autowired
    private DanmakuService danmakuService;

    /**
     * 发送弹幕
     * @param danmakuDTO 弹幕数据
     * @param userId 用户ID
     * @return 弹幕ID
     */
    @PostMapping
    public Result<Long> sendDanmaku(@RequestBody DanmakuDTO danmakuDTO, @RequestParam Long userId) {
        Long danmakuId = danmakuService.sendDanmaku(danmakuDTO, userId);
        return Result.success("发送弹幕成功", danmakuId);
    }

    /**
     * 获取视频的弹幕列表
     * @param videoId 视频ID
     * @param startTime 开始时间（秒）
     * @param endTime 结束时间（秒）
     * @return 弹幕列表
     */
    @GetMapping("/video/{videoId}")
    public Result<List<DanmakuVO>> getVideoDanmakus(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") Double startTime,
            @RequestParam(defaultValue = "3600") Double endTime) {
        List<DanmakuVO> danmakus = danmakuService.getVideoDanmakus(videoId, startTime, endTime);
        return Result.success(danmakus);
    }

    /**
     * 获取视频的弹幕数量
     * @param videoId 视频ID
     * @return 弹幕数量
     */
    @GetMapping("/count/{videoId}")
    public Result<Integer> getVideoDanmakuCount(@PathVariable Long videoId) {
        Integer count = danmakuService.getVideoDanmakuCount(videoId);
        return Result.success(count);
    }
}

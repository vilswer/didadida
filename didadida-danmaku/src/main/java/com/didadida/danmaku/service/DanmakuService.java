package com.didadida.danmaku.service;

import com.didadida.danmaku.dto.DanmakuDTO;
import com.didadida.danmaku.vo.DanmakuVO;

import java.util.List;

/**
 * 弹幕服务接口
 */
public interface DanmakuService {

    /**
     * 发送弹幕
     * @param danmakuDTO 弹幕数据
     * @param userId 用户ID
     * @return 弹幕ID
     */
    Long sendDanmaku(DanmakuDTO danmakuDTO, Long userId);

    /**
     * 获取视频的弹幕列表
     * @param videoId 视频ID
     * @param startTime 开始时间（秒）
     * @param endTime 结束时间（秒）
     * @return 弹幕列表
     */
    List<DanmakuVO> getVideoDanmakus(Long videoId, Double startTime, Double endTime);

    /**
     * 获取视频的弹幕数量
     * @param videoId 视频ID
     * @return 弹幕数量
     */
    Integer getVideoDanmakuCount(Long videoId);

    /**
     * 同步弹幕到数据库
     * @param danmakuMessageDTO 弹幕消息
     */
    void syncDanmakuToMysql(com.didadida.danmaku.dto.DanmakuMessageDTO danmakuMessageDTO);
}

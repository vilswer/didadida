package com.didadida.danmaku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.didadida.danmaku.entity.Danmaku;

import java.util.List;

/**
 * 弹幕Mapper
 */
public interface DanmakuMapper extends BaseMapper<Danmaku> {

    /**
     * 根据视频ID和时间范围查询弹幕
     * @param videoId 视频ID
     * @param startTime 开始时间（秒）
     * @param endTime 结束时间（秒）
     * @return 弹幕列表
     */
    List<Danmaku> selectByVideoIdAndTimeRange(Long videoId, Double startTime, Double endTime);

    /**
     * 根据视频ID查询弹幕数量
     * @param videoId 视频ID
     * @return 弹幕数量
     */
    Integer selectCountByVideoId(Long videoId);
}

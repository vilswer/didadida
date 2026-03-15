package com.didadida.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.didadida.video.entity.VideoEpisode;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 视频选集Mapper
 *
 * @author 奶油盒桃
 * @date 2026-03-10
 */
public interface VideoEpisodeMapper extends BaseMapper<VideoEpisode> {

    /**
     * 根据视频ID查询所有选集
     * @param videoId 视频ID
     * @return 选集列表
     */
    List<VideoEpisode> selectByVideoId(@Param("videoId") Long videoId);

    /**
     * 根据视频ID和选集状态查询选集
     * @param videoId 视频ID
     * @param status 状态（1-公开）
     * @return 选集列表
     */
    List<VideoEpisode> selectByVideoIdAndStatus(@Param("videoId") Long videoId, @Param("status") Integer status);
}

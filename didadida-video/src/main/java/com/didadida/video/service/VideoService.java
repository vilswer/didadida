package com.didadida.video.service;

import com.didadida.video.vo.VideoEpisodeVO;
import com.didadida.video.vo.VideoPlayDetailVO;

import java.util.List;

public interface VideoService {

    /**
     * 获取视频播放详情（核心播放接口）
     * @param videoId 视频ID
     * @return 视频播放详情
     */
    VideoPlayDetailVO getVideoPlayDetail(Long videoId);

    /**
     * 上报视频播放量（高并发异步处理）
     * @param videoId 视频ID
     */
    void reportVideoView(Long videoId);

    /**
     * 批量同步播放量到MySQL（Kafka消费者调用）
     */
    void batchSyncViewCountToMysql();

    /**
     * 获取视频选集列表
     * @param videoId 视频ID
     * @return 选集列表
     */
    List<VideoEpisodeVO> getVideoEpisodes(Long videoId);

    /**
     * 获取视频选集详情
     * @param episodeId 选集ID
     * @return 选集详情
     */
    VideoPlayDetailVO getVideoEpisodeDetail(Long episodeId);

    /**
     * 获取相关视频推荐
     * @param videoId 视频ID
     * @param limit 推荐数量
     * @return 推荐视频列表
     */
    List<VideoPlayDetailVO> getRelatedVideos(Long videoId, Integer limit);

    /**
     * 点赞视频
     * @param videoId 视频ID
     * @param userId 用户ID
     */
    void likeVideo(Long videoId, Long userId);

    /**
     * 取消点赞
     * @param videoId 视频ID
     * @param userId 用户ID
     */
    void unlikeVideo(Long videoId, Long userId);

    /**
     * 投币视频
     * @param videoId 视频ID
     * @param userId 用户ID
     * @param coinCount 投币数量
     */
    void coinVideo(Long videoId, Long userId, Integer coinCount);

    /**
     * 收藏视频
     * @param videoId 视频ID
     * @param userId 用户ID
     */
    void collectVideo(Long videoId, Long userId);

    /**
     * 取消收藏
     * @param videoId 视频ID
     * @param userId 用户ID
     */
    void uncollectVideo(Long videoId, Long userId);

    /**
     * 分享视频
     * @param videoId 视频ID
     * @param userId 用户ID
     */
    void shareVideo(Long videoId, Long userId);

    /**
     * 批量同步互动数据到MySQL（Kafka消费者调用）
     */
    void batchSyncInteractionToMysql();
}

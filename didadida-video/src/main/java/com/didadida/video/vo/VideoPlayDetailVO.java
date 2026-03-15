package com.didadida.video.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频播放详情返回VO
 */
@Data
public class VideoPlayDetailVO {

    /**
     * 视频唯一ID
     */
    private Long videoId;

    /**
     * 视频标题
     */
    private String title;

    /**
     * 视频简介
     */
    private String description;

    /**
     * 封面图URL
     */
    private String coverUrl;

    /**
     * 预签名的视频播放地址（核心）
     */
    private String playUrl;

    /**
     * 视频时长（秒）
     */
    private Integer duration;

    /**
     * UP主用户ID
     */
    private Long userId;

    /**
     * UP主昵称（后续可通过用户服务远程调用补充）
     */
    private String authorNickname;

    /**
     * 播放量
     */
    private Long viewCount;

    /**
     * 实时播放量（从Redis获取）
     */
    private Long realViewCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 投币数
     */
    private Long coinCount;

    /**
     * 收藏数
     */
    private Long collectCount;

    /**
     * 转发数
     */
    private Long shareCount;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 当前选集ID
     */
    private Long currentEpisodeId;

    /**
     * 当前选集标题
     */
    private String currentEpisodeTitle;

    /**
     * 当前选集序号
     */
    private Integer currentEpisodeOrder;
}

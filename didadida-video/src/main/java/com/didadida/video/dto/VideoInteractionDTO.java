package com.didadida.video.dto;

import lombok.Data;

/**
 * 视频互动数据DTO
 * 用于Kafka消息传输
 */
@Data
public class VideoInteractionDTO {

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 互动类型：like, unlike, coin, collect, uncollect, share
     */
    private String interactionType;

    /**
     * 投币数量（仅用于投币操作）
     */
    private Integer coinCount;

    /**
     * 操作时间戳
     */
    private Long timestamp;

    public VideoInteractionDTO() {
    }

    public VideoInteractionDTO(Long videoId, Long userId, String interactionType, Long timestamp) {
        this.videoId = videoId;
        this.userId = userId;
        this.interactionType = interactionType;
        this.timestamp = timestamp;
    }

    public VideoInteractionDTO(Long videoId, Long userId, String interactionType, Integer coinCount, Long timestamp) {
        this.videoId = videoId;
        this.userId = userId;
        this.interactionType = interactionType;
        this.coinCount = coinCount;
        this.timestamp = timestamp;
    }
}
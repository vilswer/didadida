package com.didadida.video.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 播放量上报Kafka消息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoViewReportDTO {

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 上报时间戳
     */
    private Long timestamp;
}

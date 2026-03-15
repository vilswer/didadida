package com.didadida.danmaku.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 弹幕消息DTO，用于Kafka消息传输
 */
@Data
public class DanmakuMessageDTO {

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 弹幕内容
     */
    private String content;

    /**
     * 在视频中的时间点（秒，精确到毫秒）
     */
    private BigDecimal videoTime;

    /**
     * 弹幕颜色（HEX）
     */
    private String color;

    /**
     * 字体大小
     */
    private Integer fontSize;

    /**
     * 弹幕模式：1-滚动，2-顶部，3-底部
     */
    private Integer mode;

    /**
     * 时间戳
     */
    private Long timestamp;
}

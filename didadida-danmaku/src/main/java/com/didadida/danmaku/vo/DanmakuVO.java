package com.didadida.danmaku.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 弹幕视图对象
 */
@Data
public class DanmakuVO {

    /**
     * 弹幕ID
     */
    private Long danmakuId;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 发送用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

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
     * 发送时间
     */
    private LocalDateTime createTime;
}

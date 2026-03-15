package com.didadida.danmaku.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 弹幕表实体类
 *
 * @author 奶油盒桃
 * @date 2026-03-07
 */
@Data
@TableName("t_danmaku")
public class Danmaku implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 弹幕ID
     */
    @TableId(type = IdType.AUTO)
    private Long danmakuId;

    /**
     * 视频ID（分片键）
     */
    private Long videoId;

    /**
     * 发送用户ID
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
     * 发送时间
     */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
}

package com.didadida.video.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频主表实体类
 *
 * @author 奶油盒桃
 * @date 2026-03-07
 */
@Data
@TableName("t_video")
public class Video implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 视频唯一ID（AV号）
     */
    @TableId(type = IdType.AUTO)
    private Long videoId;

    /**
     * UP主用户ID
     */
    private Long userId;

    /**
     * 分区ID
     */
    private Long categoryId;

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
     * 视频播放地址（CDN）
     */
    private String videoUrl;

    /**
     * 视频时长（秒）
     */
    private Integer duration;

    /**
     * 播放量
     */
    private Long viewCount;

    /**
     * 弹幕数
     */
    private Long danmakuCount;

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
     * 审核状态：0-待审核，1-通过，2-拒绝
     */
    private Integer auditStatus;

    /**
     * 可见状态：0-下架，1-公开，2-仅自己可见
     */
    private Integer status;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 创建时间
     */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

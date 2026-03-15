package com.didadida.video.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频选集实体类
 *
 * @author 奶油盒桃
 * @date 2026-03-10
 */
@Data
@TableName("t_video_episode")
public class VideoEpisode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 选集ID
     */
    @TableId(type = IdType.AUTO)
    private Long episodeId;

    /**
     * 视频ID（关联t_video表）
     */
    private Long videoId;

    /**
     * 选集标题
     */
    private String episodeTitle;

    /**
     * 选集序号
     */
    private Integer episodeOrder;

    /**
     * 选集视频地址
     */
    private String videoUrl;

    /**
     * 选集时长（秒）
     */
    private Integer duration;

    /**
     * 审核状态：0-待审核，1-通过，2-拒绝
     */
    private Integer auditStatus;

    /**
     * 可见状态：0-下架，1-公开，2-仅自己可见
     */
    private Integer status;

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
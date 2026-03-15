package com.didadida.video.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频分区表实体类
 *
 * @author 奶油盒桃
 * @date 2026-03-07
 */
@Data
@TableName("t_video_category")
public class VideoCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分区ID
     */
    @TableId(type = IdType.AUTO)
    private Long categoryId;

    /**
     * 分区名称
     */
    private String name;

    /**
     * 分区描述
     */
    private String description;

    /**
     * 分区图标URL
     */
    private String icon;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
}

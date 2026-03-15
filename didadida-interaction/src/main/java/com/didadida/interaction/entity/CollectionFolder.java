package com.didadida.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏夹表实体类
 *
 * @author 奶油盒桃
 * @date 2026-03-07
 */
@Data
@TableName("t_collection_folder")
public class CollectionFolder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 收藏夹ID
     */
    @TableId(type = IdType.AUTO)
    private Long folderId;

    /**
     * 创建者用户ID
     */
    private Long userId;

    /**
     * 收藏夹名称
     */
    private String folderName;

    /**
     * 收藏夹描述
     */
    private String description;

    /**
     * 收藏夹封面
     */
    private String coverUrl;

    /**
     * 是否公开：0-私密，1-公开
     */
    private Integer isPublic;

    /**
     * 收藏视频数
     */
    private Integer videoCount;

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

package com.didadida.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户关注表实体类
 *
 * @author 奶油盒桃
 * @date 2026-03-07
 */
@Data
@TableName("t_user_follow")
public class UserFollow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关注者ID（粉丝）
     */
    private Long userId;

    /**
     * 被关注者ID（UP主）
     */
    private Long followUserId;

    /**
     * 关注时间
     */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
}
package com.didadida.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("t_user")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一ID
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 加密后的密码
     */
    private String password;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 性别：0-保密，1-男，2-女
     */
    private Integer gender;

    /**
     * 个性签名
     */
    private String sign;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户等级
     */
    private Integer level;

    /**
     * 硬币数
     */
    private Integer coins;

    /**
     * 账号状态：0-封禁，1-正常
     */
    private Integer status;

    /**
     * 注册时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

package com.didadida.user.dto;

import lombok.Data;

/**
 * 登录返回结果
 */
@Data
public class LoginResponseDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录令牌（Redis Key前缀）
     */
    private String token;

    /**
     * 头像
     */
    private String avatar;
}

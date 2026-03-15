package com.didadida.user.service;

import com.didadida.user.dto.LoginDTO;
import com.didadida.user.dto.LoginResponseDTO;
import com.didadida.user.dto.RegisterDTO;
import com.didadida.user.entity.User;

public interface UserService {

    /**
     * 发送注册验证码
     * @param phone 手机号
     */
    void sendRegisterCaptcha(String phone);

    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 用户ID
     */
    Long register(RegisterDTO registerDTO);

    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    LoginResponseDTO login(LoginDTO loginDTO);

    /**
     * 验证令牌
     * @param token 令牌
     * @return 用户ID
     */
    Long verifyToken(String token);

    /**
     * 用户退出登录
     * @param token 令牌
     */
    void logout(String token);

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);
}

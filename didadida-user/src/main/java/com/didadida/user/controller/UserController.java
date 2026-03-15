package com.didadida.user.controller;

import com.didadida.common.result.Result;
import com.didadida.user.dto.LoginDTO;
import com.didadida.user.dto.LoginResponseDTO;
import com.didadida.user.dto.RegisterDTO;
import com.didadida.user.entity.User;
import com.didadida.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 发送注册验证码
     * @param phone 手机号
     * @return 操作结果
     */
    @PostMapping("/send-captcha")
    public Result<Void> sendRegisterCaptcha(@RequestParam String phone) {
        userService.sendRegisterCaptcha(phone);
        return Result.success("验证码发送成功");
    }

    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 用户ID
     */
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterDTO registerDTO) {
        Long userId = userService.register(registerDTO);
        return Result.success("注册成功",userId);
    }

    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginResponseDTO loginResponse = userService.login(loginDTO);
        return Result.success("登录成功",loginResponse);
    }

    /**
     * 用户退出登录
     * @param token 令牌
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestParam String token) {
        userService.logout(token);
        return Result.success("退出登录成功");
    }

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    public Result<User> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

}

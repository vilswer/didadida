package com.didadida.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.didadida.common.result.Result;
import com.didadida.common.util.CaptchaUtil;
import com.didadida.common.util.PasswordUtil;
import com.didadida.user.dto.LoginDTO;
import com.didadida.user.dto.LoginResponseDTO;
import com.didadida.user.dto.RegisterDTO;
import com.didadida.user.entity.User;
import com.didadida.user.mapper.UserMapper;
import com.didadida.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // 验证码过期时间（5分钟）
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;
    // 登录信息过期时间（7天）
    private static final long LOGIN_EXPIRE_DAYS = 7;
    // 验证码发送频率限制（1分钟）
    private static final long CAPTCHA_SEND_INTERVAL_SECONDS = 60;

    @Override
    public void sendRegisterCaptcha(String phone) {
        // 检查发送频率
        String sendLimitKey = "didadida:user:captcha:send:limit:" + phone;
        Boolean exists = stringRedisTemplate.hasKey(sendLimitKey);
        if (exists != null && exists) {
            throw new RuntimeException("验证码发送过于频繁，请稍后再试");
        }

        // 生成6位验证码
        String captcha = CaptchaUtil.generate6DigitCaptcha();
        log.info("向手机号 {} 发送验证码：{}", phone, captcha);

        // 存储验证码到Redis
        String captchaKey = CaptchaUtil.buildCaptchaRedisKey(phone);
        stringRedisTemplate.opsForValue().set(captchaKey, captcha, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 设置发送频率限制
        stringRedisTemplate.opsForValue().set(sendLimitKey, "1", CAPTCHA_SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // TODO: 集成短信服务发送验证码
        // 这里使用模拟发送，实际项目中需要集成短信服务
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO registerDTO) {
        String phone = registerDTO.getPhone();
        String captcha = registerDTO.getCaptcha();
        String password = registerDTO.getPassword();
        String confirmPassword = registerDTO.getConfirmPassword();

        // 验证确认密码
        if (!password.equals(confirmPassword)) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        // 验证验证码
        String captchaKey = CaptchaUtil.buildCaptchaRedisKey(phone);
        String storedCaptcha = stringRedisTemplate.opsForValue().get(captchaKey);
        if (storedCaptcha == null || !storedCaptcha.equals(captcha)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 检查手机号是否已注册
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, phone);
        User existingUser = userMapper.selectOne(wrapper);
        if (existingUser != null) {
            throw new RuntimeException("手机号已注册");
        }

        // 加密密码
        String encryptedPassword = PasswordUtil.encrypt(password);

        // 创建用户
        User user = new User()
                .setUsername(phone)
                .setPassword(encryptedPassword)
                .setNickname("用户" + phone.substring(7))
                .setAvatar("https://example.com/avatar/default.jpg")
                .setGender(0)
                .setSign("")
                .setLevel(1)
                .setCoins(0)
                .setStatus(1)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);

        // 删除验证码
        stringRedisTemplate.delete(captchaKey);

        log.info("用户注册成功，手机号：{}", phone);
        return user.getUserId();
    }

    @Override
    public LoginResponseDTO login(LoginDTO loginDTO) {
        String phone = loginDTO.getPhone();
        String password = loginDTO.getPassword();

        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, phone);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("手机号或密码错误");
        }

        // 验证密码
        if (!PasswordUtil.verify(password, user.getPassword())) {
            throw new RuntimeException("手机号或密码错误");
        }

        // 生成令牌
        String token = UUID.randomUUID().toString();

        // 存储登录信息到Redis
        String loginKey = CaptchaUtil.buildLoginRedisKey(token);
        stringRedisTemplate.opsForValue().set(loginKey, user.getUserId().toString(), LOGIN_EXPIRE_DAYS, TimeUnit.DAYS);

        // 构建登录响应
        LoginResponseDTO responseDTO = new LoginResponseDTO();
        responseDTO.setUserId(user.getUserId());
        responseDTO.setUsername(user.getUsername());
        responseDTO.setToken(token);
        responseDTO.setAvatar(user.getAvatar());

        log.info("用户登录成功，手机号：{}", phone);
        return responseDTO;
    }

    @Override
    public Long verifyToken(String token) {
        String loginKey = CaptchaUtil.buildLoginRedisKey(token);
        String userIdStr = stringRedisTemplate.opsForValue().get(loginKey);
        if (userIdStr == null) {
            // 令牌过期，尝试从令牌中获取用户ID（如果可能）
            // 注意：这里我们假设令牌格式可能包含用户信息，或者我们有其他方式获取用户ID
            // 由于当前实现中令牌是UUID，无法直接从中获取用户ID，所以这里不做处理
            // 在实际项目中，可能需要使用JWT等可以包含用户信息的令牌格式
            throw new RuntimeException("令牌无效或已过期");
        }
        return Long.parseLong(userIdStr);
    }

    @Override
    public void logout(String token) {
        String loginKey = CaptchaUtil.buildLoginRedisKey(token);
        // 获取用户ID
        String userIdStr = stringRedisTemplate.opsForValue().get(loginKey);
        if (userIdStr != null) {
            Long userId = Long.parseLong(userIdStr);
            // 清除用户互动记录缓存
            // 清除用户点赞记录
            String likePattern = "video:user:like:" + userId + ":*";
            // 清除用户收藏记录
            String collectPattern = "video:user:collect:" + userId + ":*";
            // 执行删除操作
            stringRedisTemplate.delete(stringRedisTemplate.keys(likePattern));
            stringRedisTemplate.delete(stringRedisTemplate.keys(collectPattern));
            log.info("用户退出登录，清除互动缓存，用户ID：{}", userId);
        }
        // 删除登录信息
        stringRedisTemplate.delete(loginKey);
        log.info("用户退出登录，令牌：{}", token);
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }
}
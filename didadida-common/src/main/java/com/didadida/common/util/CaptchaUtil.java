package com.didadida.common.util;

import cn.hutool.core.util.RandomUtil;

/**
 * 验证码生成工具
 */
public class CaptchaUtil {

    /**
     * 生成6位数字验证码
     */
    public static String generate6DigitCaptcha() {
        return RandomUtil.randomNumbers(6);
    }

    /**
     * 构建验证码Redis Key
     */
    public static String buildCaptchaRedisKey(String phone) {
        return "didadida:user:captcha:" + phone;
    }

    /**
     * 构建登录信息Redis Key
     */
    public static String buildLoginRedisKey(String token) {
        return "didadida:user:login:" + token;
    }
}

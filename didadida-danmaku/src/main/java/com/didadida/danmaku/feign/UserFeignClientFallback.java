package com.didadida.danmaku.feign;

import com.didadida.common.result.Result;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务Feign客户端降级处理
 */
@Component
public class UserFeignClientFallback implements UserFeignClient {

    @Override
    public Result<Map<String, Object>> getUserInfo(Long id) {
        // 降级处理，返回默认用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", id);
        userInfo.put("nickname", "未知用户");
        userInfo.put("avatar", "");
        return Result.success(userInfo);
    }
}

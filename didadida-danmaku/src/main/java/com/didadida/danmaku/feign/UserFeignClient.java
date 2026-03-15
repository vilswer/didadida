package com.didadida.danmaku.feign;

import com.didadida.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 用户服务Feign客户端
 */
@FeignClient(name = "didadida-user", contextId = "danmakuUserFeignClient", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {

    /**
     * 根据用户ID获取用户信息
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/user/{id}")
    Result<Map<String, Object>> getUserInfo(@PathVariable("id") Long id);
}

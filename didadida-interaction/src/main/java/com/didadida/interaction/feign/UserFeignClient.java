package com.didadida.interaction.feign;

import com.didadida.user.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务Feign客户端
 */
@FeignClient(name = "didadida-user", contextId = "interactionUserFeignClient")
public interface UserFeignClient {

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/user/{userId}")
    User getUserById(@PathVariable("userId") Long userId);

}

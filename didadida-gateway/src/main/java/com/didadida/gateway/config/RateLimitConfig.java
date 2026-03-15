package com.didadida.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 网关限流Key配置（按IP限流）
 */
@Configuration
public class RateLimitConfig {
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                // 兼容IPv6/IPv4，避免空指针
                Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                        .getAddress().getHostAddress()
        );
    }

    /**
     * 配置RedisRateLimiter（令牌桶算法）
     * 可自定义令牌生成速率、桶容量（也可在yml中配置）
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // 默认值：replenishRate=10, burstCapacity=20（yml配置会覆盖此默认值）
        return new RedisRateLimiter(10, 20);
    }


}

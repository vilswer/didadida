package com.didadida.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动类
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) // 排除数据源自动配置
@EnableDiscoveryClient // 开启服务发现（注册到Nacos）
public class DidadidaGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(DidadidaGatewayApplication.class, args);
        System.out.println("==================================");
        System.out.println("  DidaDida-Gateway 启动成功！");
        System.out.println("==================================");
    }
}

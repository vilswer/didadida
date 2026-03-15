package com.didadida.danmaku;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 弹幕服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.didadida.danmaku")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.didadida.danmaku.feign")
@MapperScan("com.didadida.danmaku.mapper")
public class DidadidaDanmakuApplication {
    public static void main(String[] args) {
        SpringApplication.run(DidadidaDanmakuApplication.class, args);
        System.out.println("==================================");
        System.out.println("  DidaDida-Danmaku 启动成功！");
        System.out.println("==================================");
    }
}
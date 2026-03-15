package com.didadida.video;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.alibaba.cloud.sentinel.annotation.SentinelRestTemplate;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * 视频服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.didadida")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.didadida.video.feign")
@MapperScan("com.didadida.video.mapper")
@EnableScheduling
public class DidadidaVideoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DidadidaVideoApplication.class, args);
        System.out.println("==================================");
        System.out.println("  DidaDida-Video 启动成功！");
        System.out.println("==================================");
    }
    
    /**
     * 配置RestTemplate，用于服务间调用
     */
    @Bean
    @LoadBalanced
    @SentinelRestTemplate
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
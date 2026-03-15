package com.didadida.interaction;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 互动服务启动类
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.didadida.interaction.feign")
@MapperScan("com.didadida.interaction.mapper")
public class DidadidaInteractionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DidadidaInteractionApplication.class, args);

        System.out.println("==================================");
        System.out.println("  DidaDida-Interaction 启动成功！");
        System.out.println("==================================");
    }

}

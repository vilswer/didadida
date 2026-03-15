package com.didadida.danmaku.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 弹幕服务安全配置
 */
@Configuration
@EnableWebSecurity
public class DanmakuSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 关闭CSRF保护
            .csrf().disable()
            // 关闭HTTP Basic认证
            .httpBasic().disable()
            // 关闭表单登录
            .formLogin().disable()
            // 配置请求授权
            .authorizeHttpRequests(authorize -> authorize
                // 允许匿名访问所有弹幕接口
                .requestMatchers("/danmaku/**").permitAll()
                // 允许匿名访问API文档
                .requestMatchers(
                    "/doc.html",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/v3/api-docs/**"
                ).permitAll()
                // 其他接口需要认证
                .anyRequest().authenticated()
            );

        return http.build();
    }
}

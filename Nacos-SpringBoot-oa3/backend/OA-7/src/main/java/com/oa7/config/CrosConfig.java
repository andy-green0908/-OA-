package com.oa7.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @name: chenle
 * @Date: 2021/10/24 0:09
 * @Author: IAO
 * @Description: 跨域配置类
 */
@Configuration
public class CrosConfig implements WebMvcConfigurer {
    public void addCorsMappings(CorsRegistry registry) {
        //前后端分离CORS配置 (兼容Spring Boot 2.3.7)
        registry
                .addMapping("/**")
                .allowedHeaders("*")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)      // 允许发送凭证（如cookies, session等）
                .maxAge(3600);              // 预检请求缓存时间
    }
}

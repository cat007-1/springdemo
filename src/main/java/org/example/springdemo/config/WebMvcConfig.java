package org.example.springdemo.config;

import org.example.springdemo.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")                 // 拦截所有
                .excludePathPatterns(                   // 放行
                        "/api/files/upload",
                        "/api/files/download/**",
                        "/login",
                        "/error",
                        // Swagger UI 与其数据源必须放行，否则页面和 JSON 都会被拦成 401
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        // Actuator 健康检查：探活不该要求登录
                        "/actuator/health",
                        "/actuator/health/**",
                        "/static/**",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                );
    }
}
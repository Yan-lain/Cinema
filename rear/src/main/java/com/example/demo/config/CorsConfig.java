package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS跨域配置类
 * 
 * 【架构说明】
 * CORS（Cross-Origin Resource Sharing）是浏览器的安全机制，用于控制跨域请求
 * 当前端和后端部署在不同域名时，需要配置CORS允许跨域访问
 * 
 * 【当前配置分析】
 * 当前配置相对安全：
 * 1. 只允许localhost和127.0.0.1来源：开发环境合理
 * 2. 允许的HTTP方法有限：GET、POST、PUT、DELETE、OPTIONS
 * 3. 允许携带凭证（AllowCredentials）：配合Cookie或Token使用
 * 
 * 【安全风险】
 * 1. 生产环境需要修改允许的来源：不能使用通配符
 * 2. 允许所有请求头：可能被利用进行攻击
 * 
 * 【改进建议】
 * 1. 在生产环境中明确指定允许的域名
 * 2. 限制允许的请求头数量
 */
@Configuration
public class CorsConfig {

    /**
     * 创建CORS过滤器
     * 
     * CorsFilter用于处理跨域请求，根据配置决定是否允许请求通过
     * 
     * @return CORS过滤器实例
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许携带凭证（Cookie、Token等）
        // 【技术说明】如果前端需要发送认证信息，必须设置为true
        config.setAllowCredentials(true);
        
        // 允许的来源（白名单）
        // 【安全说明】开发环境使用localhost，生产环境应替换为实际域名
        config.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));
        
        // 允许的请求头
        // 【技术说明】Authorization用于JWT Token，Content-Type用于POST请求
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin"));
        
        // 允许的HTTP方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 暴露给前端的响应头
        // 【技术说明】默认情况下，只有基本的响应头会暴露给前端
        config.setExposedHeaders(Arrays.asList("Authorization"));
        
        // 预检请求缓存时间（秒）
        // 【技术说明】OPTIONS预检请求的结果缓存时间，避免频繁发送预检请求
        config.setMaxAge(3600L);

        // 注册CORS配置到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 【技术说明】/**表示所有路径，根据实际情况调整
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
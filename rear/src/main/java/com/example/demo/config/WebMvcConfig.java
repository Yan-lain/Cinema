package com.example.demo.config;

import com.example.demo.interceptor.AuthInterceptor;
import com.example.demo.interceptor.LogInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 
 * 【架构说明】
 * WebMvcConfigurer是Spring MVC的配置接口，用于定制Spring MVC的行为
 * 本类主要用于注册拦截器（Interceptor）
 * 
 * 【拦截器说明】
 * 拦截器是Spring MVC提供的一种机制，用于在请求处理前后执行自定义逻辑
 * 常见用途：
 * 1. 认证检查（AuthInterceptor）
 * 2. 日志记录（LogInterceptor）
 * 3. 请求限流
 * 4. 参数校验
 * 
 * 【安全风险】
 * 1. 白名单路径硬编码：如果需要修改白名单，必须修改代码并重新部署
 * 2. 缺少限流拦截器：接口可能被恶意调用导致服务崩溃
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private LogInterceptor logInterceptor;

    /**
     * 注册拦截器
     * 
     * InterceptorRegistry用于管理所有拦截器的注册
     * 
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册日志拦截器，对所有请求生效
        // 【技术说明】日志拦截器用于记录请求的进入和退出时间，便于性能监控和问题排查
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**");

        // 注册认证拦截器
        //jwt认证拦截器
        // 【技术说明】认证拦截器用于验证用户是否登录，未登录用户无法访问受保护的接口
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")  // 对所有路径生效
                
                // 白名单路径（不需要登录即可访问）
                // 【安全说明】白名单中的接口任何人都可以访问，需要谨慎设置
                .excludePathPatterns(
                        "/api/auth/login",          // 用户登录
                        "/api/auth/register",       // 用户注册
                        "/api/auth/sendCode",       // 发送验证码
                        "/api/auth/verifyCode",     // 验证验证码
                        "/api/auth/forgotPassword", // 忘记密码
                        "/api/auth/refresh",        // 令牌刷新（用 refreshToken 自证身份）
                        "/api/movies",             // 电影列表（公开）
                        "/api/movies/showing",      // 正在上映（公开）
                        "/api/movies/search",       // 搜索电影（公开）
                        "/api/movies/*",            // 电影详情（公开）
                        "/api/cinemas",            // 影院列表（公开）
                        "/api/cinemas/*",          // 影院详情（公开）
                        "/api/schedules",          // 场次列表（公开）
                        "/api/schedules/*",        // 场次详情（公开）
                        "/api/admin/login",        // 管理员登录（公开）
                        "/api/admin/announcements/latest",  // 最新公告（公开，项目硬约束要求放行）
                        "/static/**",              // 静态资源
                        "/error",                   // 错误页面
                        "/doc.html",                // Knife4j 文档页面
                        "/v3/api-docs/**",          // OpenAPI JSON 规范
                        "/webjars/**"               // Knife4j 静态资源
                );
    }
}
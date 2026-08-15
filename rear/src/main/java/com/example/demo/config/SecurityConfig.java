package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security安全配置类
 * 
 * 【架构说明】
 * 本配置类定义了Spring Security的安全规则，包括：
 * 1. CSRF防护配置
 * 2. 会话管理策略
 * 3. URL访问权限规则
 * 4. JWT认证过滤器集成
 * 5. 密码编码器Bean定义
 * 
 * 【请求处理流程】
 * 请求进入 → JwtAuthenticationFilter → Spring Security权限检查 → Controller处理
 *                                  ↑
 *                    这里解析JWT Token并设置SecurityContext
 * 
 * 【权限检查规则】
 * - hasRole("ADMIN")：检查SecurityContext中是否有"ROLE_ADMIN"权限
 * - JwtAuthenticationFilter负责将Token中的角色转换为Spring Security格式
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 密码编码器Bean
     * 
     * 【技术说明】
     * BCryptPasswordEncoder是Spring Security提供的密码编码器实现
     * 使用BCrypt算法对密码进行加密和验证
     * 
     * 【使用方式】
     * 其他组件可以通过注入PasswordEncoder接口来使用：
     * @Autowired
     * private PasswordEncoder passwordEncoder;
     * 
     * 【注意】
     * 不要直接注入BCryptPasswordEncoder具体实现类，应该注入PasswordEncoder接口
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置安全过滤链
     * 
     * @param http HttpSecurity配置对象
     * @return 安全过滤链实例
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF防护（使用JWT无状态认证，不需要CSRF保护）
            .csrf(AbstractHttpConfigurer::disable)
            
            // 设置会话管理策略为无状态
            // JWT认证不需要Session，所有认证信息通过Token传递
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 【关键配置】将JWT认证过滤器添加到Spring Security过滤链中
            // 过滤器在UsernamePasswordAuthenticationFilter之前执行
            // 确保在Spring Security权限检查之前，SecurityContext已经设置好
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // URL访问权限规则（按顺序匹配，第一个匹配的规则生效）
            .authorizeHttpRequests(auth -> auth
               // 【第一层】公开接口 - 任何人都可以访问
               .requestMatchers(
                   "/api/auth/**",                          // 用户认证（登录、注册、忘记密码等）
                   "/api/movies",                          // 电影列表
                   "/api/movies/showing",                   // 正在上映电影
                   "/api/movies/search",                    // 电影搜索
                   "/api/movies/*",                         // 电影详情
                   "/api/cinemas",                          // 影院列表
                   "/api/cinemas/*",                        // 影院详情
                   "/api/schedules",                        // 场次列表
                   "/api/schedules/*",                      // 场次详情
                   "/api/admin/login",                      // 管理员登录（放行）
                   "/api/admin/announcements/latest",        // 最新公告
                   "/api/user/register",                    // 用户注册
                   "/static/**",                            // 静态资源
                   "/error",                                // 错误页面
                   "/doc.html",                             // Knife4j 文档页面
                   "/v3/api-docs/**",                       // OpenAPI JSON 规范
                   "/webjars/**",                           // Knife4j 静态资源
                   "/favicon.ico"
               ).permitAll()
               
               // 【第二层】管理员接口 - 需要ADMIN角色
               // hasRole("ADMIN")会检查SecurityContext中是否有"ROLE_ADMIN"权限
               // JwtAuthenticationFilter会将Token中的"admin"角色转换为"ROLE_ADMIN"
               .requestMatchers(
                   "/api/admin/movies/**",                  // 管理员-电影管理
                   "/api/admin/cinemas/**",                 // 管理员-影院管理
                   "/api/admin/halls/**",                   // 管理员-放映厅管理
                   "/api/admin/schedules/**",               // 管理员-排片管理
                   "/api/admin/orders/**",                  // 管理员-订单管理
                   "/api/admin/seats/**",                   // 管理员-座位管理
                   "/api/admin/users/**",                   // 管理员-用户管理
                   "/api/admin/announcements/**",            // 管理员-公告管理（除latest外）
                   "/api/admin/**"                          // 其他管理员接口
               ).hasRole("ADMIN")
               
               // 【第三层】其他所有请求 - 需要认证
               // 只要登录过（有合法的JWT Token）即可访问
               .anyRequest().authenticated()
            );

        return http.build();
    }
}
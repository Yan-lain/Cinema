package com.example.demo.security;

import com.example.demo.constant.Constants;
import com.example.demo.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * 
 * 【架构说明】
 * 本过滤器是Spring Security和JWT认证的桥梁，负责：
 * 1. 从请求头中提取JWT Token
 * 2. 解析Token获取用户信息（userId、username、role）
 * 3. 创建Spring Security的Authentication对象
 * 4. 将Authentication对象设置到SecurityContext中
 * 
 * 【执行时机】
 * 在Spring Security的UsernamePasswordAuthenticationFilter之前执行
 * 确保Spring Security的@PreAuthorize、hasRole()等权限注解能正常工作
 * 
 * 【工作流程】
 * 请求 → JwtAuthenticationFilter → SecurityContext设置Authentication → Spring Security权限检查
 * 
 * 【问题背景】
 * 之前的问题：Spring Security的hasRole()检查无法识别JWT中的角色信息
 * 原因：Spring Security需要SecurityContext中有Authentication对象才能进行权限检查
 * 本过滤器的作用就是创建这个Authentication对象
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * 执行JWT认证过滤
     * 
     * 【执行流程】
     * 1. 从请求头获取Authorization字段
     * 2. 验证Token格式（Bearer前缀）
     * 3. 解析Token获取用户信息
     * 4. 创建Authentication对象并设置到SecurityContext
     * 5. 继续执行过滤器链
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 获取请求路径，用于白名单检查
            String requestUri = request.getRequestURI();
            logger.info("JWT过滤器 - 请求路径: {}", requestUri);
            
            // 白名单路径检查
            // 这些路径不需要JWT认证，直接放行
            if (isWhiteListed(requestUri)) {
                logger.info("JWT过滤器 - 白名单放行: {}", requestUri);
                filterChain.doFilter(request, response);
                return;
            }

            // 从请求头获取Authorization字段
            String authHeader = request.getHeader(Constants.JWT_HEADER);
            
            // 如果没有Token，直接放行（后续由Spring Security的权限检查处理）
            if (authHeader == null || !authHeader.startsWith(Constants.JWT_TOKEN_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 提取纯Token（去除"Bearer "前缀）
            String token = authHeader.substring(Constants.JWT_TOKEN_PREFIX.length());

            // 验证Token有效性
            if (!JwtUtil.validateToken(token)) {
                logger.warn("JWT Token验证失败");
                filterChain.doFilter(request, response);
                return;
            }

            // 从Token中解析用户信息
            Long userId = JwtUtil.getUserIdFromToken(token);
            String username = JwtUtil.getUsernameFromToken(token);
            String role = JwtUtil.getRoleFromToken(token);

            // 根据角色构建权限列表
            // Spring Security要求角色必须以"ROLE_"开头
            // 这里我们将"admin"转换为"ROLE_ADMIN"，"user"转换为"ROLE_USER"
            String springRole = convertToSpringRole(role);
            
            // 创建Authentication对象
            // UsernamePasswordAuthenticationToken是Spring Security的认证令牌
            // 参数：principal（主体）、credentials（凭证）、authorities（权限列表）
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    username,                                    // 主体：用户名
                    null,                                        // 凭证：密码（已验证，设为null）
                    Collections.singletonList(new SimpleGrantedAuthority(springRole))  // 权限列表
                );
            
            // 将用户ID存储到details中，方便后续使用
            authentication.setDetails(userId);
            
            // 将Authentication对象设置到SecurityContext中
            // 这是关键步骤！设置后Spring Security的权限检查才能正常工作
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            logger.debug("JWT认证成功: userId={}, username={}, role={}", userId, username, role);
            
        } catch (Exception e) {
            logger.error("JWT认证异常: ", e);
            // 异常时清除SecurityContext，确保不会设置无效的认证信息
            SecurityContextHolder.clearContext();
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 检查路径是否在白名单中
     * 
     * 【白名单说明】
     * 这些路径不需要JWT认证：
     * - 认证相关接口（登录、注册、忘记密码等）
     * - 公开信息接口（电影、影院、场次等）
     * - 管理员登录接口
     * 
     * @param uri 请求路径
     * @return true表示在白名单中，false表示需要认证
     */
    private boolean isWhiteListed(String uri) {
        // 管理员登录接口
        if (uri.equals("/api/admin/login")) {
            return true;
        }
        
        // 用户认证接口
        if (uri.startsWith("/api/auth/")) {
            return true;
        }
        
        // 公开查询接口
        if (uri.equals("/api/movies") || 
            uri.startsWith("/api/movies/") && !uri.contains("/admin")) {
            return true;
        }
        
        if (uri.equals("/api/cinemas") || 
            uri.startsWith("/api/cinemas/") && !uri.contains("/admin")) {
            return true;
        }
        
        if (uri.equals("/api/schedules") || 
            uri.startsWith("/api/schedules/") && !uri.contains("/admin")) {
            return true;
        }
        
        // 最新公告接口
        if (uri.equals("/api/admin/announcements/latest")) {
            return true;
        }
        
        // 静态资源
        if (uri.startsWith("/static/") || uri.startsWith("/error")) {
            return true;
        }
        
        return false;
    }

    /**
     * 将系统角色转换为Spring Security格式
     * 
     * 【转换规则】
     * - "admin" → "ROLE_ADMIN"
     * - "user" → "ROLE_USER"
     * - 其他角色 → "ROLE_" + 原角色名
     * 
     * 【技术说明】
     * Spring Security的hasRole()方法要求角色名必须以"ROLE_"开头
     * 例如hasRole("ADMIN")实际检查的是"ROLE_ADMIN"
     * 
     * @param role 系统角色名
     * @return Spring Security格式的角色名
     */
    private String convertToSpringRole(String role) {
        if (role == null) {
            return "ROLE_USER";
        }
        
        // 确保角色以"ROLE_"开头
        if (role.startsWith("ROLE_")) {
            return role;
        }
        
        return "ROLE_" + role.toUpperCase();
    }
}

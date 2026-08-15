package com.example.demo.interceptor;

import com.example.demo.common.ApiResponse;
import com.example.demo.constant.Constants;
import com.example.demo.constant.ErrorCode;
import com.example.demo.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 * 
 * 【架构说明】
 * Spring MVC拦截器，在请求到达Controller之前执行认证检查
 * 
 * 【工作流程】
 * 1. 检查请求路径是否在白名单中（白名单路径不需要认证）
 * 2. 从请求头中获取JWT Token
 * 3. 验证Token的有效性（格式、签名、过期时间）
 * 4. 解析Token中的用户信息（userId、role）
 * 5. 将用户信息存入request属性，供后续Controller使用
 * 
 * 【安全风险】
 * 1. 白名单路径硬编码：如果需要修改白名单，必须修改代码并重新部署
 * 2. Token验证仅依赖JwtUtil：一旦JwtUtil有漏洞，认证就会失效
 * 3. 缺少权限校验：只验证是否登录，不验证用户角色权限
 * 
 * 【改进建议】
 * 1. 将白名单配置到配置文件中
 * 2. 添加基于角色的权限校验
 * 3. 考虑使用Spring Security的方法级注解（@PreAuthorize）
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    /**
     * ObjectMapper用于将Java对象转换为JSON字符串
     * 【技术说明】用于在拦截器中返回JSON格式的错误响应
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 在请求处理之前执行（进入Controller方法之前）
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 处理器（通常是Controller方法）
     * @return true表示继续处理请求，false表示拦截请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求路径
        String requestUri = request.getRequestURI();

        // 检查是否在白名单中
        // 【安全说明】白名单中的路径不需要认证，任何人都可以访问
        for (String path : Constants.WHITE_LIST_PATHS) {
            if (requestUri.startsWith(path)) {
                return true;
            }
        }

        // 从请求头中获取JWT Token
        // 【技术说明】JWT通常通过Authorization请求头传递，格式为"Bearer xxx"
        String token = request.getHeader(Constants.JWT_HEADER);
        
        // 检查Token是否存在且格式正确
        if (token == null || !token.startsWith(Constants.JWT_TOKEN_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(ErrorCode.AUTH_NOT_LOGIN, "未登录，请先登录")
            ));
            return false;
        }

        // 去除Token前缀（"Bearer "），获取纯Token字符串
        token = token.substring(Constants.JWT_TOKEN_PREFIX.length());

        // 验证Token的有效性
        if (!JwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(ErrorCode.TOKEN_INVALID, "Token无效或已过期")
            ));
            return false;
        }

        // 从Token中提取用户信息
        Long userId = JwtUtil.getUserIdFromToken(token);
        String role = JwtUtil.getRoleFromToken(token);

        // 将用户信息存入request属性，供后续Controller使用
        // 【技术说明】使用request.setAttribute可以在同一个请求的不同组件间传递数据
        request.setAttribute("userId", userId);
        request.setAttribute("role", role);

        // 认证通过，继续处理请求
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 在请求处理完成后执行（Controller方法执行后）
        // 【技术说明】用于在请求处理完成后执行一些清理操作，如关闭数据库连接、释放资源等
        // 【注意】这里没有实际的清理操作，仅作为示例
        //没有usercontext工具类，没有使用ThreadLocal存储用户信息
        //ThreadLocal.remove();

    }
}
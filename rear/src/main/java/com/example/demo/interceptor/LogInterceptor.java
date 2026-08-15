package com.example.demo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 日志拦截器
 * 
 * 【架构说明】
 * Spring MVC拦截器，用于记录请求的进入和退出时间
 * 
 * 【工作流程】
 * 1. 请求进入时（preHandle）：记录请求方法、路径、客户端IP，并保存开始时间
 * 2. 请求完成时（afterCompletion）：计算请求耗时，记录请求状态码
 * 
 * 【使用场景】
 * - 性能监控：统计每个接口的响应时间
 * - 问题排查：追踪请求的处理过程
 * - 安全审计：记录所有请求日志
 * 
 * 【安全风险】
 * 1. 日志信息过多：可能影响性能
 * 2. 敏感信息泄露：日志中可能包含密码等敏感信息
 * 
 * 【改进建议】
 * 1. 添加日志级别配置，生产环境降低日志级别
 * 2. 对敏感请求参数进行脱敏处理
 */
@Component
public class LogInterceptor implements HandlerInterceptor {
    
    /**
     * SLF4J日志记录器
     * 【技术说明】SLF4J是Java日志框架的门面，支持多种日志实现（如Logback、Log4j）
     */
    private static final Logger log = LoggerFactory.getLogger(LogInterceptor.class);

    /**
     * 在请求处理之前执行
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 处理器
     * @return true表示继续处理请求
     * 登录前后的日志记录
     * 
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录请求开始时间，用于计算请求耗时
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        // 记录请求信息：HTTP方法、请求路径、客户端IP
        log.info("请求开始 - {} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());

        return true;
    }

    /**
     * 在请求完成后执行（无论成功还是失败）
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 处理器
     * @param ex 请求过程中抛出的异常（如果有的话）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
       //token验证
       //
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("未授权请求 - {} {} - {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr());
            return;
        }
        // 获取请求开始时间，计算请求耗时
        long startTime = (Long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;

        // 根据是否有异常，记录不同级别的日志
        if (ex != null) {
            // 请求异常：记录ERROR级别日志
            log.error("请求异常 - {} {} - {}ms - {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    duration,
                    ex.getMessage());
        } else {
            // 请求正常：记录INFO级别日志
            log.info("请求结束 - {} {} - {}ms - {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    duration,
                    response.getStatus());
        }
    }
}
package com.example.demo.exception;

import com.example.demo.common.ApiResponse;
import com.example.demo.constant.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 
 * 【架构说明】
 * 使用@RestControllerAdvice注解，统一处理所有Controller抛出的异常
 * 避免每个Controller重复编写异常处理代码
 * 
 * 【处理的异常类型】
 * 1. BusinessException：业务异常（用户不存在、密码错误等）
 * 2. MethodArgumentNotValidException：参数校验异常（@Valid校验失败）
 * 3. RuntimeException：运行时异常（其他未知异常）
 * 
 * 【安全风险】
 * 1. 异常信息泄露：RuntimeException可能暴露系统内部信息
 * 2. 缺少请求上下文信息：日志中缺少请求路径、参数等信息
 * 
 * 【改进建议】
 * 1. 对RuntimeException返回通用错误信息
 * 2. 在日志中记录更多请求上下文信息
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     * 
     * @param e 业务异常
     * @return 统一响应格式
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        // 记录业务异常日志（WARN级别，因为业务异常是预期的）
        logger.warn("业务异常: 错误码={}, 错误信息={}", e.getErrorCode().getCode(), e.getMessage());
        return ApiResponse.error(e.getErrorCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     * 
     * 【技术说明】当使用@Valid注解校验请求参数失败时，Spring会抛出此异常
     * 
     * @param e 参数校验异常
     * @return 统一响应格式，包含具体的字段错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        // 收集所有字段的错误信息
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        logger.warn("参数校验失败: {}", errors);
        return ApiResponse.error(ErrorCode.VALIDATION_ERROR, "参数校验失败", errors);
    }

    /**
     * 处理运行时异常
     * 
     * 【安全说明】对于未知的运行时异常，不应该返回详细信息给前端
     * 避免泄露系统内部信息（如数据库表结构、代码路径等）
     * 
     * @param e 运行时异常
     * @return 统一响应格式，只返回通用错误信息
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e) {
        // 记录异常日志（ERROR级别，因为这是未预期的错误）
        // 【安全说明】只记录日志，不把详细信息返回给前端
        logger.error("系统运行时异常: ", e);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR, "系统错误，请稍后重试");
    }
}
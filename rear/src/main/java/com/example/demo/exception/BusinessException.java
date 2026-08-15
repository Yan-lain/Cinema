package com.example.demo.exception;

import com.example.demo.constant.ErrorCode;

/**
 * 业务异常类
 * 
 * 【架构说明】
 * 自定义异常类，用于封装业务逻辑中的错误情况
 * 与系统异常（如NullPointerException）区分开
 * 
 * 【使用场景】
 * 当业务逻辑执行失败时抛出此异常，例如：
 * - 用户不存在
 * - 密码错误
 * - 电影不存在
 * - 座位已售罄
 * 
 * 【安全风险】
 * 1. 异常信息泄露：详细的错误信息可能被攻击者利用
 * 2. 缺少异常日志记录：异常发生时没有记录日志
 * 
 * 【改进建议】
 * 1. 在全局异常处理器中过滤敏感信息
 * 2. 添加异常日志记录
 */
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final ErrorCode errorCode;

    /**
     * 创建业务异常
     * 
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 创建业务异常（自定义错误信息）
     * 
     * @param errorCode 错误码枚举
     * @param message 自定义错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 创建业务异常（仅消息，使用默认错误码SYSTEM_ERROR）
     * 
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.errorCode = ErrorCode.SYSTEM_ERROR;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
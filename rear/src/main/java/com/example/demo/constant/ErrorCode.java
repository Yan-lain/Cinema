package com.example.demo.constant;

/**
 * 业务错误码枚举类
 * 
 * 【架构说明】
 * 定义业务层面的错误码，与HTTP状态码区分开
 * HTTP状态码表示请求的整体结果，业务错误码表示具体的业务失败原因
 * 
 * 【错误码分类】
 * 1. 1001-1099：用户相关错误
 * 2. 2001-2099：电影相关错误
 * 3. 3001-3099：订单相关错误
 * 4. 4001-4099：系统相关错误
 * 
 * 【安全风险】
 * 1. 错误信息过于详细：可能泄露系统内部信息
 * 2. 错误码无规律：不利于前端处理
 * 
 * 【改进建议】
 * 1. 对敏感操作返回模糊错误信息
 * 2. 统一错误码格式和范围
 */
public enum ErrorCode {
    
    /** 用户不存在 */
    USER_NOT_FOUND(1001, "用户不存在"),
    
    /** 用户名或密码错误 */
    WRONG_PASSWORD(1002, "用户名或密码错误"),
    
    /** 用户已存在 */
    USER_EXISTS(1003, "用户名已存在"),
    
    /** 用户未登录 */
    AUTH_NOT_LOGIN(1004, "用户未登录"),
    
    /** Token过期 */
    TOKEN_EXPIRED(1005, "登录已过期，请重新登录"),
    
    /** Token无效 */
    TOKEN_INVALID(1006, "Token无效"),

    /** RefreshToken 无效或已过期 */
    REFRESH_TOKEN_INVALID(1015, "刷新令牌无效或已过期，请重新登录"),
    
    /** 用户无权限 */
    USER_NO_PERMISSION(1007, "用户无权限"),
    
    /** 用户已禁用 */
    USER_DISABLED(1008, "账号已禁用"),
    
    /** 邮箱格式错误 */
    INVALID_EMAIL(1009, "邮箱格式不正确"),
    
    /** 邮箱已存在 */
    EMAIL_EXISTS(1010, "邮箱已被注册"),
    
    /** 密码强度不足 */
    PASSWORD_WEAK(1011, "密码强度不足"),
    
    /** 密码不匹配 */
    PASSWORD_NOT_MATCH(1012, "密码不匹配"),
    
    /** 验证码已过期 */
    CODE_EXPIRED(1013, "验证码已过期"),
    
    /** 验证码错误 */
    CODE_ERROR(1014, "验证码错误"),
    
    /** 参数校验失败 */
    VALIDATION_ERROR(4001, "参数校验失败"),
    
    /** 验证码错误 */
    VERIFY_CODE_ERROR(4002, "验证码错误"),
    
    /** 邮箱发送失败 */
    EMAIL_SEND_ERROR(4003, "邮箱发送失败"),
    
    /** 电影不存在 */
    MOVIE_NOT_FOUND(2001, "电影不存在"),
    
    /** 影院不存在 */
    CINEMA_NOT_FOUND(2002, "影院不存在"),
    
    /** 场次不存在 */
    SCHEDULE_NOT_FOUND(2003, "场次不存在"),
    
    /** 场次已结束 */
    SCHEDULE_ENDED(2004, "场次已结束"),
    
    /** 座位已售罄 */
    SEAT_SOLD_OUT(3001, "座位已售罄"),
    
    /** 座位不存在 */
    SEAT_NOT_FOUND(3004, "座位不存在"),
    
    /** 座位不可用 */
    SEAT_NOT_AVAILABLE(3005, "座位不可用"),
    
    /** 订单不存在 */
    ORDER_NOT_FOUND(3002, "订单不存在"),
    
    /** 订单状态错误 */
    ORDER_STATUS_ERROR(3003, "订单状态错误"),
    
    /** 订单退票错误 */
    ORDER_REFUND_ERROR(3006, "订单退票错误"),
    
    /** 数据库操作失败 */
    DATABASE_ERROR(4004, "数据库操作失败"),
    
    /** 系统错误 */
    SYSTEM_ERROR(5001, "系统错误");

    /** 错误码 */
    private final int code;
    
    /** 错误信息 */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
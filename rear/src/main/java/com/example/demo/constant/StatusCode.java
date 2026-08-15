package com.example.demo.constant;

/**
 * 响应状态码枚举类
 * 
 * 【架构说明】
 * 统一管理API响应的状态码，便于前端根据状态码进行不同的处理
 * 
 * 【状态码分类】
 * 1. 200系列：成功状态码
 * 2. 400系列：客户端错误（请求参数错误、未认证等）
 * 3. 500系列：服务端错误
 * 
 * 【安全风险】
 * 1. 状态码信息泄露：详细的错误码可能被攻击者利用
 * 2. 缺少自定义状态码：某些业务错误没有对应的状态码
 * 
 * 【改进建议】
 * 1. 对敏感错误返回通用错误码
 * 2. 添加更多业务相关的状态码
 */
public enum StatusCode {
    
    /** 操作成功 */
    SUCCESS(200, "操作成功"),
    
    /** 请求参数错误 */
    BAD_REQUEST(400, "请求参数错误"),
    
    /** 未认证（未登录或Token无效） */
    UNAUTHORIZED(401, "未认证，请先登录"),
    
    /** 无权限访问 */
    FORBIDDEN(403, "无权限访问"),
    
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),
    
    /** 请求方式不支持 */
    METHOD_NOT_ALLOWED(405, "请求方式不支持"),
    
    /** 服务器内部错误 */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    
    /** 服务不可用 */
    SERVICE_UNAVAILABLE(503, "服务不可用");

    /** 状态码 */
    private final int code;
    
    /** 状态信息 */
    private final String message;

    StatusCode(int code, String message) {
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
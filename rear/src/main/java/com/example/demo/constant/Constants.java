package com.example.demo.constant;

/**
 * 系统常量类
 * 
 * 【架构说明】
 * 集中管理系统中使用的常量值，避免硬编码
 * 
 * 【常量分类】
 * 1. JWT相关常量（密钥、过期时间、Token前缀等）
 * 2. Redis相关常量（缓存Key前缀、过期时间等）
 * 3. 密码策略常量（最小长度、正则表达式等）
 * 4. 用户角色常量（ADMIN、USER）
 * 
 * 【安全风险】
 * 1. JWT密钥硬编码：生产环境应使用环境变量
 * 2. 密码策略可被绕过：仅靠前端验证不够
 * 
 * 【改进建议】
 * 1. 将敏感常量配置到环境变量中
 * 2. 在后端增加密码策略验证
 */
public class Constants {
    
    /**
     * JWT相关常量
     */
    
    /** JWT签名密钥 */
    public static final String JWT_SECRET = "cinema-management-system-jwt-secret-key-256-bit-minimum";
    
    /** JWT过期时间（毫秒）- 30分钟（accessToken 寿命） */
    public static final long JWT_EXPIRE_TIME = 30 * 60 * 1000L;

    /**
     * RefreshToken 过期时间（秒）- 7 天
     * 【设计说明】accessToken 过期后用 refreshToken 换取新 accessToken，
     * 避免用户频繁重新登录；RefreshToken 寿命远长于 accessToken。
     */
    public static final long REFRESH_TOKEN_EXPIRE_SECONDS = 7 * 24 * 60 * 60L;

    /** JWT在HTTP请求头中的名称 */
    public static final String JWT_HEADER = "Authorization";

    /** JWT Token前缀 */
    public static final String JWT_TOKEN_PREFIX = "Bearer ";

    /**
     * Redis相关常量
     */
    
    /** 用户信息缓存Key前缀 */
    public static final String REDIS_USER_KEY_PREFIX = "user:";
    
    /** 电影列表缓存Key */
    public static final String REDIS_MOVIES_KEY = "movies:list";
    
    /** 邮箱验证码缓存Key前缀 */
    public static final String REDIS_VERIFY_CODE_KEY_PREFIX = "verify:code:";
    
    /** 邮箱验证码过期时间（秒）- 5分钟 */
    public static final long REDIS_VERIFY_CODE_EXPIRE_TIME = 5 * 60L;
    
    /** 分布式锁Key前缀 */
    public static final String REDIS_LOCK_KEY_PREFIX = "lock:seat:";
    
    /** 分布式锁过期时间（秒）- 5分钟 */
    public static final long REDIS_LOCK_EXPIRE_TIME = 5 * 60L;

    /**
     * 密码策略常量
     */
    
    /** 密码最小长度 */
    public static final int PASSWORD_MIN_LENGTH = 8;
    
    /** 密码正则表达式 */
    /**
     * 【技术说明】密码正则表达式要求：
     * - 至少包含一个小写字母 (?=.*[a-z])
     * - 至少包含一个大写字母 (?=.*[A-Z])
     * - 至少包含一个数字 (?=.*\d)
     * - 至少包含一个特殊字符 (?=.*[@$!%*?&])
     * - 长度至少8位 ([A-Za-z\d@$!%*?&]{8,})
     */
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    /**
     * 用户角色常量
     */
    
    /** 管理员角色 */
    public static final String ROLE_ADMIN = "ADMIN";
    
    /** 普通用户角色 */
    public static final String ROLE_USER = "USER";

    /**
     * 订单状态常量
     */
    
    /** 待支付 */
    public static final String ORDER_STATUS_PENDING = "PENDING";
    
    /** 已支付 */
    public static final String ORDER_STATUS_PAID = "PAID";
    
    /** 已完成 */
    public static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    
    /** 已取消 */
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";

    /**
     * 支付状态常量
     */
    
    /** 未支付 */
    public static final String PAY_STATUS_UNPAID = "UNPAID";
    
    /** 已支付 */
    public static final String PAY_STATUS_PAID = "PAID";
    
    /** 已退款 */
    public static final String PAY_STATUS_REFUNDED = "REFUNDED";

    /**
     * 退款状态常量
     */
    
    /** 无退款 */
    public static final String REFUND_STATUS_NONE = "NONE";
    
    /** 退款中 */
    public static final String REFUND_STATUS_PROCESSING = "PROCESSING";
    
    /** 已退款 */
    public static final String REFUND_STATUS_REFUNDED = "REFUNDED";

    /**
     * 座位状态常量
     */
    
    /** 可用 */
    public static final String SEAT_STATUS_AVAILABLE = "AVAILABLE";
    
    /** 已锁定（订单未支付） */
    public static final String SEAT_STATUS_LOCKED = "LOCKED";
    
    /** 已售出 */
    public static final String SEAT_STATUS_SOLD = "SOLD";

    /**
     * 业务规则常量
     */
    
    /** 订单超时时间（分钟）- 未支付订单自动取消 */
    public static final int ORDER_TIMEOUT_MINUTES = 15;
    
    /** 退票截止时间（分钟）- 电影开场前多少分钟内不允许退票 */
    public static final int REFUND_MINUTES_BEFORE_SHOW = 30;
    
    /** 邮箱验证码过期时间（秒）- 5分钟 */
    public static final long EMAIL_CODE_EXPIRE_TIME = 5 * 60L;
    
    /** 邮箱验证码缓存Key前缀 */
    public static final String CACHE_EMAIL_CODE_PREFIX = "email:code:";
    
    /** 用户状态 - 活跃 */
    public static final String STATUS_ACTIVE = "active";
    
    /** 用户状态 - 禁用 */
    public static final String STATUS_DISABLED = "disabled";

    /**
     * 白名单路径（不需要登录即可访问的接口）
     */
    public static final String[] WHITE_LIST_PATHS = {
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/sendCode",
        "/api/auth/verifyCode",
        "/api/auth/forgotPassword",
        "/api/auth/refresh",                       // 令牌刷新（公开接口，使用 refreshToken 自证身份）
        "/api/movies",
        "/api/movies/showing",
        "/api/movies/search",
        "/api/movies/*",
        "/api/cinemas",
        "/api/cinemas/*",
        "/api/schedules",
        "/api/schedules/*",
        "/api/admin/announcements/latest",         // 最新公告（公开，项目硬约束要求放行）
        "/static/",
        "/error"
    };
}
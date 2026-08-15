package com.example.demo.util;

import com.example.demo.constant.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 
 * 【架构说明】
 * JWT（JSON Web Token）是一种用于身份认证的令牌格式
 * 本类负责：
 * 1. 生成JWT Token（用户和管理员共用）
 * 2. 解析JWT Token
 * 3. 验证JWT Token的有效性
 * 
 * 【技术说明】
 * JWT由三部分组成：
 * 1. Header（头部）：声明Token类型和算法
 * 2. Payload（载荷）：存储用户信息（如userId、username、role）
 * 3. Signature（签名）：对前两部分进行签名，防止篡改
 * 
 * 【安全风险】
 * 1. 密钥硬编码：JWT_SECRET常量直接写在代码中，生产环境应使用环境变量
 * 2. 密钥强度：当前密钥长度可能不足以抵抗暴力破解
 * 3. Token过期时间：30分钟可能太短或太长，需根据业务需求调整
 * 
 * 【改进建议】
 * 1. 将密钥存储在环境变量或配置中心
 * 2. 使用更长的随机密钥（至少256位）
 * 3. 考虑实现Token刷新机制
 */
public class JwtUtil {

    /**
     * 从常量中获取JWT密钥，并转换为SecretKey对象
     * 
     * 【技术说明】JJWT库需要SecretKey对象来进行签名和验证
     * 使用HmacSHA256算法要求密钥长度至少256位
     */
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
        Constants.JWT_SECRET.getBytes(StandardCharsets.UTF_8)
    );

    /**
     * 生成JWT Token
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param role 用户角色（ADMIN或USER）
     * @return 生成的JWT Token字符串
     */
    public static String generateToken(Long userId, String username, String role) {
        // 创建载荷（Payload），存储用户信息
        // 【安全说明】不要在载荷中存储敏感信息（如密码），因为载荷只是Base64编码，不是加密
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        
        // 根据角色设置不同的主题
        String subject = "user";
        if ("admin".equalsIgnoreCase(role)) {
            subject = "admin";
        }

        // 构建JWT Token
        // 【安全说明】Token过期时间（30分钟）根据业务需求调整
        // .subject("user") 表示Token用于用户认证 是指普通用户还是游客 那管理员呢？
        // 管理员用户也可以访问普通用户页面，但是不能访问管理员页面
        // 例如：管理员用户可以访问/admin/movies/list，但是不能访问/admin/login
        // 所以，管理员用户需要单独的Token，不能直接使用普通用户的Token
        // 该怎么做呢？
        // 1. 为管理员用户生成单独的Token，存储在数据库中
        // 2. 在请求头中添加Authorization字段，值为Bearer + 管理员Token
        // 3. 服务器端在验证Token时，根据角色判断是否有权限访问
        // 4. 如果没有权限，返回403 Forbidden错误
        return Jwts.builder()
            .claims(claims)                              // 设置载荷
            .subject(subject)                            // 设置主题（标识Token用途：user或admin）
            .issuedAt(new Date())                        // 设置签发时间
            .expiration(new Date(System.currentTimeMillis() + Constants.JWT_EXPIRE_TIME))  // 设置过期时间
            .signWith(SECRET_KEY)                        // 使用密钥签名
            .compact();                                  // 压缩成字符串
    }

    /**
     * 解析JWT Token，获取载荷中的信息
     * 
     * @param token JWT Token字符串
     * @return Claims对象，包含所有载荷信息
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(SECRET_KEY)           // 使用密钥验证签名
            .build()
            .parseSignedClaims(token)         // 解析Token
            .getPayload();                    // 获取载荷
    }

    /**
     * 验证JWT Token是否有效
     * 
     * @param token JWT Token字符串
     * @return true表示Token有效，false表示无效（过期或签名错误）
     */
    public static boolean validateToken(String token) {
        try {
            // 验证Token签名和过期时间 返回的是Claims对象 为什么不用接收Claims对象？
            // 因为验证Token时，只需要验证签名和过期时间，不需要获取载荷中的信息
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从Token中提取用户ID
     * 
     * @param token JWT Token字符串
     * @return 用户ID
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中提取用户名
     * 
     * @param token JWT Token字符串
     * @return 用户名
     */
    public static String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从Token中提取用户角色
     * 
     * @param token JWT Token字符串
     * @return 用户角色（ADMIN或USER）
     */
    public static String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 从Token中提取主题
     * 
     * @param token JWT Token字符串
     * @return 主题（user或admin）
     */
    public static String getSubjectFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 检查Token是否过期
     * 
     * @param token JWT Token字符串
     * @return true表示已过期，false表示未过期
     */
    public static boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}

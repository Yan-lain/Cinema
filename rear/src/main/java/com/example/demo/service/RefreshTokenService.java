package com.example.demo.service;

import com.example.demo.constant.Constants;
import com.example.demo.constant.RedisKey;
import com.example.demo.dto.response.RefreshTokenResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.constant.ErrorCode;
import com.example.demo.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * RefreshToken 服务
 *
 * 【架构说明】
 * 配合 JwtUtil（accessToken）实现"双 Token"无感续期机制：
 * - accessToken：短寿命 JWT（30 分钟），放 Authorization 头用于业务接口鉴权
 * - refreshToken：长寿命随机字符串（7 天），存 Redis，仅用于 /api/auth/refresh 换取新 accessToken
 *
 * 【安全特性】
 * 1. refreshToken 为不可猜测的 UUID，不携带任何用户信息（避免泄露）
 * 2. 存储于 Redis，可主动吊销（删除 Key 即令其失效）
 * 3. 采用"旋转"策略：每次刷新签发新的 refreshToken 并删除旧的，降低泄露风险
 *
 * 【存储结构】
 * Key:   refresh:token:{refreshTokenString}
 * Value: Map{ userId, username, role }
 * TTL:   7 天
 */
@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    /** Map 中的字段名常量，避免拼写错误 */
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_ROLE = "role";

    @Autowired
    private RedisService redisService;

    /**
     * 签发一个新的 refreshToken 并存入 Redis
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色（admin/user 原始值）
     * @return 生成的 refreshToken 字符串
     */
    public String generate(Long userId, String username, String role) {
        // 使用 UUID 作为 refreshToken 本体，不可预测且全局唯一
        String refreshToken = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> payload = new HashMap<>();
        payload.put(FIELD_USER_ID, userId);
        payload.put(FIELD_USERNAME, username);
        payload.put(FIELD_ROLE, role);

        // 写入 Redis 并设置 TTL，到期自动失效
        redisService.set(
                RedisKey.refreshToken(refreshToken),
                payload,
                Constants.REFRESH_TOKEN_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );

        logger.debug("签发 refreshToken: userId={}, username={}", userId, username);
        return refreshToken;
    }

    /**
     * 校验 refreshToken 是否有效，有效则返回其携带的用户身份信息
     *
     * @param refreshToken 前端传入的 refreshToken 字符串
     * @return 身份信息 Map（含 userId/username/role）；无效或过期返回 null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> validate(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return null;
        }
        Object value = redisService.get(RedisKey.refreshToken(refreshToken));
        // RedisService.get 在 Key 不存在或存的是 NULL_VALUE 占位时都会返回 null
        if (!(value instanceof Map)) {
            return null;
        }
        return (Map<String, Object>) value;
    }

    /**
     * 吊销指定的 refreshToken（删除 Redis 中的 Key）
     * 【使用场景】登出、refresh 旋转时删除旧 token、管理员强制下线
     *
     * @param refreshToken 要吊销的 refreshToken 字符串
     */
    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return;
        }
        redisService.delete(RedisKey.refreshToken(refreshToken));
    }

    /**
     * 从身份信息 Map 中提取 userId
     */
    public Long extractUserId(Map<String, Object> payload) {
        Object val = payload.get(FIELD_USER_ID);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof String) {
            return Long.parseLong((String) val);
        }
        return null;
    }

    /**
     * 从身份信息 Map 中提取 username
     */
    public String extractUsername(Map<String, Object> payload) {
        Object val = payload.get(FIELD_USERNAME);
        return val == null ? null : val.toString();
    }

    /**
     * 从身份信息 Map 中提取 role
     */
    public String extractRole(Map<String, Object> payload) {
        Object val = payload.get(FIELD_ROLE);
        return val == null ? null : val.toString();
    }

    /**
     * 用 refreshToken 换取新的 accessToken（含 refreshToken 旋转）
     *
     * 【执行流程】
     * 1. 校验 refreshToken 是否有效（存在且未过期）
     * 2. 从 Redis 取出用户身份信息
     * 3. 删除旧 refreshToken（旋转策略，使其立即失效）
     * 4. 签发新的 accessToken（JWT）和新的 refreshToken
     * 5. 返回新的双 Token
     *
     * @param oldRefreshToken 前端传入的旧 refreshToken
     * @return 新的 accessToken + refreshToken
     * @throws BusinessException 当 refreshToken 无效或已过期时抛出
     */
    public RefreshTokenResponse refresh(String oldRefreshToken) {
        // 1. 校验 refreshToken
        Map<String, Object> payload = validate(oldRefreshToken);
        if (payload == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = extractUserId(payload);
        String username = extractUsername(payload);
        String role = extractRole(payload);
        if (userId == null || username == null || role == null) {
            // 数据残缺视为非法，清理后拒绝
            revoke(oldRefreshToken);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 2. 旋转：删除旧 refreshToken，使其无法被重复使用
        revoke(oldRefreshToken);

        // 3. 签发新的 accessToken 和 refreshToken
        String newAccessToken = JwtUtil.generateToken(userId, username, role);
        String newRefreshToken = generate(userId, username, role);

        logger.info("刷新令牌成功: userId={}, username={}", userId, username);
        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }
}

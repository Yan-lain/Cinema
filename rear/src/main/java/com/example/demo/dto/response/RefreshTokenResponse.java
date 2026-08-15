package com.example.demo.dto.response;

import lombok.Data;

/**
 * 令牌刷新响应 DTO
 *
 * 【字段说明】
 * - token：新的短寿命 accessToken，前端用于后续业务请求的 Authorization 头
 * - refreshToken：新的长寿命 refreshToken（旋转策略下每次刷新都换发新的）
 *
 * 【设计说明】采用 refreshToken 旋转策略：每次刷新后旧 refreshToken 立即失效，
 * 降低 refreshToken 泄露后被长期利用的风险。
 */
@Data
public class RefreshTokenResponse {
    /** 新签发的 accessToken */
    private String token;

    /** 新签发的 refreshToken（替换前端旧值） */
    private String refreshToken;

    public RefreshTokenResponse(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }
}

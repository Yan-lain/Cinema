package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 令牌刷新请求 DTO
 *
 * 【使用场景】
 * 前端在 accessToken 临期或收到 401 时，携带 refreshToken 调用 /api/auth/refresh，
 * 后端校验通过后签发新的 accessToken（和新的 refreshToken）。
 */
@Data
public class RefreshTokenRequest {

    /** 长寿命刷新令牌（登录/注册时由后端下发） */
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}

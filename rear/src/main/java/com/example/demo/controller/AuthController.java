package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.request.*;
import com.example.demo.dto.response.RefreshTokenResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "用户登录、注册、Token 刷新、密码重置等接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Operation(summary = "发送验证码", description = "发送邮箱验证码，有效期5分钟")
    @PostMapping("/sendCode")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        userService.sendVerificationCode(request);
        return ApiResponse.success("验证码已发送，有效期5分钟", null);
    }

    @Operation(summary = "验证验证码")
    @PostMapping("/verifyCode")
    public ApiResponse<Void> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        userService.verifyCode(request);
        return ApiResponse.success("验证成功", null);
    }

    @Operation(summary = "用户注册", description = "新用户注册账号，返回用户信息和 JWT Token")
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);
        return ApiResponse.success("注册成功", response);
    }

    @Operation(summary = "用户登录", description = "用户名密码登录，返回 accessToken 和 refreshToken")
    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        UserResponse response = userService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 刷新访问令牌
     *
     * 【接口说明】
     * 前端在 accessToken 临期或收到 401 时调用此接口，用 refreshToken 换取新的 accessToken。
     * 该接口为公开接口（在 SecurityConfig 与 AuthInterceptor 白名单中），通过 refreshToken 自证身份。
     *
     * 【刷新策略】采用 refreshToken 旋转：每次刷新后旧 refreshToken 立即失效，并下发新的 refreshToken。
     *
     * @param request 包含 refreshToken 的请求体
     * @return 新的 accessToken + refreshToken
     */
    @Operation(summary = "刷新令牌", description = "用 refreshToken 换取新的 accessToken 和 refreshToken（令牌旋转策略）")
    @PostMapping("/refresh")
    public ApiResponse<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = refreshTokenService.refresh(request.getRefreshToken());
        return ApiResponse.success("令牌刷新成功", response);
    }

    @Operation(summary = "用户登出", description = "前端清除本地 Token 完成登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success("退出成功", null);
    }

    @Operation(summary = "获取用户信息", description = "根据用户 ID 查询用户信息")
    @GetMapping("/userinfo")
    public ApiResponse<UserResponse> getUserInfo(@RequestParam Long userId) {
        UserResponse response = userService.getUserById(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "上传头像")
    @PostMapping("/uploadAvatar")
    public ApiResponse<UserResponse> uploadAvatar(@RequestBody java.util.Map<String, Object> params) {
        Long userId = Long.parseLong(params.get("userId").toString());
        String avatar = params.get("avatar") != null ? params.get("avatar").toString() : null;
        UserResponse response = userService.uploadAvatar(userId, avatar);
        return ApiResponse.success("头像上传成功", response);
    }

    @Operation(summary = "更新用户信息")
    @PostMapping("/update")
    public ApiResponse<UserResponse> updateUser(@RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(request);
        return ApiResponse.success("更新成功", response);
    }

    @Operation(summary = "修改密码")
    @PostMapping("/changePassword")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.success("密码修改成功", null);
    }

    @Operation(summary = "忘记密码（邮箱验证码重置）")
    @PostMapping("/forgotPassword")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return ApiResponse.success("密码重置成功", null);
    }

    // @PostMapping("/movies/search")
    // public ApiResponse<UserResponse> searchMovies(@RequestBody SearchMoviesRequest request) {
    //     UserResponse response = userService.searchMovies(request);
    //     return ApiResponse.success(response);
    // }
}
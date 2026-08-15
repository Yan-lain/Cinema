package com.example.demo.service;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.UserResponse;

public interface UserService {
    UserResponse login(LoginRequest request);

    UserResponse register(RegisterRequest request);

    UserResponse getUserById(Long id);

    UserResponse updateUser(UpdateUserRequest request);

    void changePassword(ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void sendVerificationCode(SendCodeRequest request);

    void verifyCode(VerifyCodeRequest request);

    UserResponse uploadAvatar(Long userId, String avatar);
}
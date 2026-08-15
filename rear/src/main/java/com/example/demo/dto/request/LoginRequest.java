package com.example.demo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {
    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 4, max = 12, message = "用户名长度必须在4-12位之间")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 8, max = 128, message = "密码长度必须在8-128位之间")
    @NotBlank(message = "密码不能为空")
    private String password;
}
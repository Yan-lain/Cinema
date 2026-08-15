package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private Long id;
    private String nickname;
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;
    private String avatar;
}
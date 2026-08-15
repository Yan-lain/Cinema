package com.example.demo.dto.response;

import com.example.demo.entity.User;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String avatar;
    private String status;
    private String email;
    private String role;
    /** 访问令牌（短寿命 JWT），放 Authorization 头用于业务接口鉴权 */
    private String token;
    /** 刷新令牌（长寿命），仅用于调用 /api/auth/refresh 换取新 accessToken */
    private String refreshToken;

    // 从User实体转换为UserResponse
    // 用于返回登录成功后的用户信息和JWT Token
    //为什么不直接在实体那里添加token字段？
    // 因为实体类中添加token字段会导致数据库中存储的token字段与前端返回的token字段不一致
    // 而且token字段是敏感信息，不应该直接暴露给前端
    // 所以，在UserResponse中添加token字段，只用于返回登录成功后的token，不存储到数据库中
    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setAvatar(user.getAvatar());
        response.setStatus(user.getStatus());
        response.setEmail(user.getEmail());
        // 统一将角色转为小写，确保前端判断一致
        response.setRole(user.getRole() != null ? user.getRole().toLowerCase() : null);
        return response;
    }

    public static UserResponse fromEntity(User user, String token) {
        UserResponse response = fromEntity(user);
        response.setToken(token);
        return response;
    }

    /**
     * 同时设置 accessToken 和 refreshToken 的转换方法
     * 【使用场景】登录/注册成功后，后端同时签发两个 Token 返回给前端
     */
    public static UserResponse fromEntity(User user, String token, String refreshToken) {
        UserResponse response = fromEntity(user);
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        return response;
    }
}
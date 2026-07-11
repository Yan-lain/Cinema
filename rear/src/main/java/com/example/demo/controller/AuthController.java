package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.EmailService;
import com.example.demo.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private EmailService emailService;
    
    private static final String EMAIL_CODE_PREFIX = "email:code:";
    private static final long CODE_EXPIRE_TIME = 300; // 5分钟过期

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/sendCode")
    public Map<String, Object> sendCode(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        String email = params.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "邮箱不能为空");
            return result;
        }
        
        // 生成6位验证码
        String code = generateCode();
        
        // 存储到Redis，5分钟过期
        redisService.set(EMAIL_CODE_PREFIX + email, code, CODE_EXPIRE_TIME, TimeUnit.SECONDS);
        
        // 使用真实邮件服务发送验证码
        boolean sendSuccess = emailService.sendVerificationCode(email, code);
        
        if (sendSuccess) {
            result.put("success", true);
            result.put("message", "验证码已发送，有效期5分钟");
        } else {
            // 发送失败，删除已存储的验证码
            redisService.delete(EMAIL_CODE_PREFIX + email);
            result.put("success", false);
            result.put("message", "邮件发送失败，请稍后重试");
        }
        
        // 同时打印到控制台（方便测试）
        System.out.println("【验证码】发送到 " + email + " 的验证码是：" + code);
        
        return result;
    }
    
    /**
     * 验证邮箱验证码（验证成功后不删除，允许后续使用一次）
     */
    @PostMapping("/verifyCode")
    public Map<String, Object> verifyCode(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        String email = params.get("email");
        String code = params.get("code");
        
        if (email == null || email.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "邮箱不能为空");
            return result;
        }
        
        if (code == null || code.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "验证码不能为空");
            return result;
        }
        
        Object storedCodeObj = redisService.get(EMAIL_CODE_PREFIX + email);
        String storedCode = storedCodeObj != null ? storedCodeObj.toString() : null;
        if (storedCode == null) {
            result.put("success", false);
            result.put("message", "验证码已过期，请重新获取");
            return result;
        }
        
        if (!storedCode.equals(code)) {
            result.put("success", false);
            result.put("message", "验证码错误");
            return result;
        }
        
        // 验证成功后不删除验证码，允许在有效期内用于重置密码
        // 验证码会在5分钟后自动过期
        
        result.put("success", true);
        result.put("message", "验证成功");
        return result;
    }
    
    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        String username = params.get("username") != null ? params.get("username").toString() : null;
        String password = params.get("password") != null ? params.get("password").toString() : null;
        String email = params.get("email") != null ? params.get("email").toString() : null;
        String code = params.get("code") != null ? params.get("code").toString() : null;
        
        if (username == null || username.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "用户名不能为空");
            return result;
        }

        if (password == null || password.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "密码不能为空");
            return result;
        }

        if (email == null || email.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "邮箱不能为空");
            return result;
        }

        if (code == null || code.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "验证码不能为空");
            return result;
        }

        // 验证验证码
        Object storedCodeObj = redisService.get(EMAIL_CODE_PREFIX + email);
        String storedCode = storedCodeObj != null ? storedCodeObj.toString() : null;
        if (storedCode == null) {
            result.put("success", false);
            result.put("message", "验证码已过期，请重新获取");
            return result;
        }
        
        if (!storedCode.equals(code)) {
            result.put("success", false);
            result.put("message", "验证码错误");
            return result;
        }

        if (userMapper.countByUsername(username) > 0) {
            result.put("success", false);
            result.put("message", "用户名已存在");
            return result;
        }

        if (userMapper.countByEmail(email) > 0) {
            result.put("success", false);
            result.put("message", "邮箱已被注册");
            return result;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setNickname(username);
        user.setStatus("active");
        user.setRole("user");
        userMapper.insert(user);
        
        // 删除已使用的验证码
        redisService.delete(EMAIL_CODE_PREFIX + email);

        result.put("success", true);
        result.put("message", "注册成功");
        result.put("data", user);
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        String username = params.get("username");
        String password = params.get("password");

        if (username == null || password == null) {
            result.put("success", false);
            result.put("message", "用户名或密码不能为空");
            return result;
        }

        User user = userMapper.findByUsername(username);
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        } else if (!password.equals(user.getPassword())) {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        } else if ("disabled".equals(user.getStatus())) {
            result.put("success", false);
            result.put("message", "账号异常，请联系管理员");
        } else {
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("data", user);
        }
        return result;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "退出成功");
        return result;
    }

    @GetMapping("/userinfo")
    public Map<String, Object> getUserInfo(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        User user = userMapper.findById(userId);
        if (user != null) {
            result.put("success", true);
            result.put("data", user);
        } else {
            result.put("success", false);
            result.put("message", "用户不存在");
        }
        return result;
    }

    @PostMapping("/uploadAvatar")
    public Map<String, Object> uploadAvatar(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        if (!params.containsKey("userId")) {
            result.put("success", false);
            result.put("message", "用户ID不能为空");
            return result;
        }
        
        Long userId = Long.parseLong(params.get("userId").toString());
        String avatar = params.get("avatar") != null ? params.get("avatar").toString() : null;
        
        User user = userMapper.findById(userId);
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        
        user.setAvatar(avatar);
        userMapper.update(user);
        
        User updatedUser = userMapper.findById(userId);
        result.put("success", true);
        result.put("message", "头像上传成功");
        result.put("data", updatedUser);
        return result;
    }

    @PostMapping("/update")
    public Map<String, Object> updateUser(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        if (!params.containsKey("id")) {
            result.put("success", false);
            result.put("message", "用户ID不能为空");
            return result;
        }

        Long userId = Long.parseLong(params.get("id").toString());
        User existingUser = userMapper.findById(userId);
        if (existingUser == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        // 更新用户信息，支持独立修改昵称、手机号、邮箱
        // 昵称更新（支持前端发送的 name 或 nickname 字段）
        if (params.containsKey("name")) {
            existingUser.setNickname(params.get("name").toString());
        } else if (params.containsKey("nickname")) {
            existingUser.setNickname(params.get("nickname").toString());
        }
        
        // 手机号更新（支持绑定和修改）
        if (params.containsKey("phone")) {
            existingUser.setPhone(params.get("phone").toString());
        }
        
        // 邮箱更新（支持绑定和修改）
        if (params.containsKey("email")) {
            existingUser.setEmail(params.get("email").toString());
        }
        
        // 头像更新
        if (params.containsKey("avatar")) {
            existingUser.setAvatar(params.get("avatar").toString());
        }

        userMapper.update(existingUser);
        User updatedUser = userMapper.findById(userId);
        result.put("success", true);
        result.put("message", "更新成功");
        result.put("data", updatedUser);
        return result;
    }

    @PostMapping("/changePassword")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        Long userId = Long.parseLong(params.get("userId"));
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        User user = userMapper.findById(userId);
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        if (!oldPassword.equals(user.getPassword())) {
            result.put("success", false);
            result.put("message", "原密码错误");
            return result;
        }

        userMapper.updatePassword(userId, newPassword);
        result.put("success", true);
        result.put("message", "密码修改成功");
        return result;
    }
    
    /**
     * 忘记密码 - 通过邮箱验证码重置密码
     */
    @PostMapping("/forgotPassword")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        String email = params.get("email") != null ? params.get("email").toString() : null;
        String code = params.get("code") != null ? params.get("code").toString() : null;
        String newPassword = params.get("newPassword") != null ? params.get("newPassword").toString() : null;
        
        if (email == null || email.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "邮箱不能为空");
            return result;
        }
        
        if (code == null || code.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "验证码不能为空");
            return result;
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "新密码不能为空");
            return result;
        }
        
        // 验证验证码
        Object storedCodeObj = redisService.get(EMAIL_CODE_PREFIX + email);
        String storedCode = storedCodeObj != null ? storedCodeObj.toString() : null;
        if (storedCode == null) {
            result.put("success", false);
            result.put("message", "验证码已过期，请重新获取");
            return result;
        }
        
        if (!storedCode.equals(code)) {
            result.put("success", false);
            result.put("message", "验证码错误");
            return result;
        }
        
        // 查找用户
        User user = userMapper.findByEmail(email);
        if (user == null) {
            result.put("success", false);
            result.put("message", "该邮箱未注册");
            return result;
        }
        
        // 更新密码
        userMapper.updatePassword(user.getId(), newPassword);
        
        // 删除已使用的验证码
        redisService.delete(EMAIL_CODE_PREFIX + email);
        
        result.put("success", true);
        result.put("message", "密码重置成功");
        return result;
    }
}

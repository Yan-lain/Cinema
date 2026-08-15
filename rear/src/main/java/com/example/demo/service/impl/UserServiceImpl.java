package com.example.demo.service.impl;

import com.example.demo.constant.Constants;
import com.example.demo.constant.ErrorCode;
import com.example.demo.dto.request.*;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.EmailService;
import com.example.demo.service.RedisService;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.service.UserService;
import com.example.demo.util.JwtUtil;
import com.example.demo.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 * 
 * 【架构说明】
 * 实现用户相关的业务逻辑，包括登录、注册、修改密码、获取用户信息等
 * 
 * 【核心功能】
 * 1. 用户登录（login）：验证用户名密码，生成JWT Token
 * 2. 用户注册（register）：校验邮箱和密码强度，发送验证码，创建用户
 * 3. 获取用户信息（getUserById）：根据ID查询用户
 * 4. 更新用户信息（updateUser）：修改用户昵称、手机号、邮箱、头像
 * 5. 修改密码（changePassword）：验证原密码，设置新密码
 * 6. 忘记密码（forgotPassword）：通过验证码重置密码
 * 7. 发送验证码（sendVerificationCode）：生成验证码并发送到邮箱
 * 8. 验证验证码（verifyCode）：校验验证码是否正确
 * 
 * 【安全风险】
 * 1. 验证码在控制台打印：生产环境不应打印敏感信息
 * 2. 无登录失败次数限制：可能被暴力破解
 * 3. 无会话管理：Token过期后无法强制下线
 * 
 * 【改进建议】
 * 1. 删除控制台打印验证码的代码
 * 2. 添加登录失败次数限制（使用Redis）
 * 3. 实现Token黑名单机制
 */
@Service
public class UserServiceImpl implements UserService {

    /** 用户数据访问层 */
    @Autowired
    private UserMapper userMapper;

    /** Redis服务 */
    @Autowired
    private RedisService redisService;

    /** 邮件服务 */
    @Autowired
    private EmailService emailService;

    /** RefreshToken 服务（用于签发长寿命刷新令牌，实现无感续期） */
    @Autowired
    private RefreshTokenService refreshTokenService;

    /**
     * 用户登录
     * 
     * @param request 登录请求（包含username和password）
     * @return 用户响应（包含用户信息和JWT Token）
     */
    @Override
    public UserResponse login(LoginRequest request) {
        // System.out.println("================用户进行登录=================");
        // System.out.println("用户名: " + request.getUsername());
        // System.out.println("密码: " + request.getPassword()); 
        // 根据用户名查询用户 邮箱认证待定
        User user = userMapper.findByUsername(request.getUsername());        
        
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户名或密码错误");
        }
        // System.out.println("查询到的用户: " + user.getUsername());
        // System.out.println("数据库存储的密码: " + user.getPassword());
        // System.out.println("密码验证结果: " + (PasswordUtil.matches(request.getPassword(), user.getPassword()) ? "成功" : "失败"));
       
        // 验证密码是否匹配
        if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.WRONG_PASSWORD, "用户名或密码错误");
        }
        

        // 检查用户状态是否正常
        if (Constants.STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "账号异常，请联系管理员");
        }

        // 为管理员用户生成独立的JWT Token（兼容大小写）
        if (user.getRole() != null && Constants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
            // 同时签发短寿命 accessToken（JWT）和长寿命 refreshToken（存 Redis）
            String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            String refreshToken = refreshTokenService.generate(user.getId(), user.getUsername(), user.getRole());
            return UserResponse.fromEntity(user, token, refreshToken);
        }

        // 同时签发短寿命 accessToken（JWT）和长寿命 refreshToken（存 Redis）
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = refreshTokenService.generate(user.getId(), user.getUsername(), user.getRole());
        return UserResponse.fromEntity(user, token, refreshToken);
    }

    /**
     * 用户注册
     * 
     * @param request 注册请求（包含username、password、email、code）
     * @return 用户响应（包含用户信息和JWT Token）
     */
    @Override
    public UserResponse register(RegisterRequest request) {
        // 验证邮箱格式
        if (!PasswordUtil.isValidEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL, "邮箱格式不正确");
        }

        // 验证密码强度
        if (!PasswordUtil.isValid(request.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK, "密码强度不足，需要包含大小写字母、数字和特殊字符");
        }

        // 验证邮箱验证码
        verifyEmailCode(request.getEmail(), request.getCode());

        // 检查用户名是否已存在
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.USER_EXISTS, "用户名已存在");
        }

        // 检查邮箱是否已被注册
        if (userMapper.countByEmail(request.getEmail()) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS, "邮箱已被注册");
        }

        // 创建用户对象
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtil.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getUsername());
        user.setStatus(Constants.STATUS_ACTIVE);
        user.setRole(Constants.ROLE_USER);
        userMapper.insert(user);

        // 注册成功后删除验证码
        redisService.delete(Constants.CACHE_EMAIL_CODE_PREFIX + request.getEmail());

        // 同时签发 accessToken 和 refreshToken（注册即登录，与登录保持一致）
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = refreshTokenService.generate(user.getId(), user.getUsername(), user.getRole());
        return UserResponse.fromEntity(user, token, refreshToken);
    }

    /**
     * 根据ID获取用户信息
     * 
     * @param id 用户ID
     * @return 用户响应
     */
    @Override
    public UserResponse getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return UserResponse.fromEntity(user);
    }

    /**
     * 更新用户信息
     * 
     * @param request 更新请求（包含用户ID和要更新的字段）
     * @return 更新后的用户响应
     */
    @Override
    public UserResponse updateUser(UpdateUserRequest request) {
        User existingUser = userMapper.findById(request.getId());
        if (existingUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        // 更新昵称（如果提供）
        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            existingUser.setNickname(request.getNickname());
        }
        // 更新手机号（如果提供）
        if (request.getPhone() != null) {
            existingUser.setPhone(request.getPhone());
        }
        // 更新邮箱（如果提供，且格式正确）
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!PasswordUtil.isValidEmail(request.getEmail())) {
                throw new BusinessException(ErrorCode.INVALID_EMAIL, "邮箱格式不正确");
            }
            existingUser.setEmail(request.getEmail());
        }
        // 更新头像（如果提供）
        if (request.getAvatar() != null) {
            existingUser.setAvatar(request.getAvatar());
        }

        // 保存更新
        userMapper.update(existingUser);
        return UserResponse.fromEntity(existingUser);
    }

    /**
     * 修改密码
     * 
     * @param request 修改密码请求（包含userId、oldPassword、newPassword）
     */
    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = userMapper.findById(request.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        // 验证原密码是否正确
        if (!PasswordUtil.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH, "原密码错误");
        }

        // 验证新密码强度
        if (!PasswordUtil.isValid(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK, "密码强度不足，需要包含大小写字母、数字和特殊字符");
        }

        // 更新密码（加密后存储）
        userMapper.updatePassword(request.getUserId(), PasswordUtil.encode(request.getNewPassword()));
    }

    /**
     * 忘记密码（通过邮箱验证码重置）
     * 
     * @param request 忘记密码请求（包含email、code、newPassword）
     */
    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // 验证邮箱格式
        if (!PasswordUtil.isValidEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL, "邮箱格式不正确");
        }

        // 验证新密码强度
        if (!PasswordUtil.isValid(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK, "密码强度不足，需要包含大小写字母、数字和特殊字符");
        }

        // 验证邮箱验证码
        verifyEmailCode(request.getEmail(), request.getCode());

        // 根据邮箱查找用户
        User user = userMapper.findByEmail(request.getEmail());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "该邮箱未注册");
        }

        // 更新密码并删除验证码
        userMapper.updatePassword(user.getId(), PasswordUtil.encode(request.getNewPassword()));
        redisService.delete(Constants.CACHE_EMAIL_CODE_PREFIX + request.getEmail());
    }

    /**
     * 发送邮箱验证码
     * 
     * @param request 发送验证码请求（包含email）
     */
    @Override
    public void sendVerificationCode(SendCodeRequest request) {
        // 验证邮箱格式
        if (!PasswordUtil.isValidEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL, "邮箱格式不正确");
        }

        // 生成6位数字验证码
        String code = generateCode();
        
        // 将验证码存入Redis（5分钟过期）
        redisService.set(Constants.CACHE_EMAIL_CODE_PREFIX + request.getEmail(), code, Constants.EMAIL_CODE_EXPIRE_TIME, TimeUnit.SECONDS);

        // 发送验证码邮件
        boolean sendSuccess = emailService.sendVerificationCode(request.getEmail(), code);
        if (!sendSuccess) {
            // 发送失败，删除Redis中的验证码
            redisService.delete(Constants.CACHE_EMAIL_CODE_PREFIX + request.getEmail());
            throw new BusinessException("邮件发送失败，请稍后重试");
        }

        // ⚠️ 【安全风险】生产环境不应打印验证码
        System.out.println("【验证码】发送到 " + request.getEmail() + " 的验证码是：" + code);
    }

    /**
     * 验证邮箱验证码
     * 
     * @param request 验证验证码请求（包含email和code）
     */
    @Override
    public void verifyCode(VerifyCodeRequest request) {
        verifyEmailCode(request.getEmail(), request.getCode());
    }

    /**
     * 上传头像
     * 
     * @param userId 用户ID
     * @param avatar 头像URL
     * @return 更新后的用户响应
     */
    @Override
    public UserResponse uploadAvatar(Long userId, String avatar) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        user.setAvatar(avatar);
        userMapper.update(user);
        return UserResponse.fromEntity(user);
    }

    /**
     * 验证邮箱验证码（私有方法）
     * 
     * @param email 用户邮箱
     * @param code 用户输入的验证码
     */
    private void verifyEmailCode(String email, String code) {
        // 从Redis中获取存储的验证码
        Object storedCodeObj = redisService.get(Constants.CACHE_EMAIL_CODE_PREFIX + email);
        String storedCode = storedCodeObj != null ? storedCodeObj.toString() : null;

        // 验证码不存在（已过期）
        if (storedCode == null) {
            throw new BusinessException(ErrorCode.CODE_EXPIRED, "验证码已过期，请重新获取");
        }

        // 验证码不匹配
        if (!storedCode.equals(code)) {
            throw new BusinessException(ErrorCode.CODE_ERROR, "验证码错误");
        }
    }

    /**
     * 生成6位数字验证码（私有方法）
     * 
     * @return 6位数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
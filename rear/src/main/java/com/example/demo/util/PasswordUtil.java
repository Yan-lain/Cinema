package com.example.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类
 * 
 * 【架构说明】
 * 本类负责密码的加密和验证，使用BCrypt算法
 * 
 * 【技术说明】BCrypt算法特点：
 * 1. 单向哈希：无法从密文还原明文
 * 2. 加盐处理：即使相同密码，每次加密结果也不同
 * 3. 可配置强度：通过cost factor控制计算复杂度
 * 4. 抗彩虹表攻击：加盐使得彩虹表失效
 * 
 * 【安全风险】
 * 1. BCrypt强度固定：当前使用默认强度（10），可根据需要调整
 * 2. 无密码历史检查：用户可以重复使用旧密码
 * 
 * 【改进建议】
 * 1. 将BCrypt强度配置到配置文件中
 * 2. 添加密码历史检查功能，防止用户使用旧密码
 */
public class PasswordUtil {

    /**
     * BCrypt密码编码器实例
     * 
     * 【技术说明】BCryptPasswordEncoder是Spring Security提供的密码编码器
     * 默认使用强度为10（cost factor），数值越大计算越慢，安全性越高
     */
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 对密码进行加密
     * 
     * @param rawPassword 明文密码
     * @return BCrypt加密后的密码字符串
     */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证密码是否匹配
     * 
     * @param rawPassword 用户输入的明文密码
     * @param encodedPassword 数据库中存储的加密密码
     * @return true表示密码匹配，false表示不匹配
     * BCrypt每次加密的salt不一样不能这么验证密码，你怎么不告诉我？
     * 因为BCrypt每次加密的salt不一样，所以不能直接比较明文密码和数据库中的加密密码
     * 只能使用BCryptPasswordEncoder的matches方法来验证密码是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 验证邮箱格式是否正确
     * 
     * @param email 待验证的邮箱地址
     * @return true表示格式正确，false表示格式错误
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        //验证邮箱格式是否正确
        //^[A-Za-z0-9+_.-]+：以字母、数字、下划线、点、短横线开头
        //@：包含@符号
        //@[A-Za-z0-9.-]+$：以字母、数字、点、短横线结尾
        //$：以什么结尾
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(regex);
    }

    /**
     * 验证密码强度是否符合要求
     * 
     * 【技术说明】密码要求：
     * - 至少包含一个小写字母
     * - 至少包含一个大写字母
     * - 至少包含一个数字
     * - 至少包含一个特殊字符 (@$!%*?&)
     * - 长度至少8位
     * 
     * @param password 待验证的密码
     * @return true表示密码强度足够，false表示密码强度不足
     */
    public static boolean isValid(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        //?=.*[a-z])：至少包含一个小写字母
        //?=.*[A-Z])：至少包含一个大写字母
        //?=.*\\d)：至少包含一个数字
        //?=.*[@$!%*?&])：至少包含一个特殊字符 (@$!%*?&)
        //^：以什么开头
        //[$A-Za-z\\d@$!%*?&]{8,}：8位以上，只能包含字母、数字、特殊字符
        //$：以什么结尾
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(regex);
        //返回true表示密码强度足够，false表示密码强度不足
        //它在哪些地方被引用了？//在UserController中被引用了，用于验证用户注册时的密码强度是否符合要求
    }
}
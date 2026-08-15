package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务类
 * 
 * 【架构说明】
 * 封装邮件发送功能，提供统一的接口供其他模块使用
 * 
 * 【核心功能】
 * 1. 发送验证码邮件
 * 
 * 【安全风险】
 * 1. 发件人邮箱硬编码：应配置到配置文件中
 * 2. 无发送频率限制：可能被滥用发送大量邮件
 * 3. 无邮件模板：邮件内容硬编码，不易维护
 * 
 * 【改进建议】
 * 1. 将邮箱配置移到application.properties中
 * 2. 添加发送频率限制（使用Redis）
 * 3. 使用Thymeleaf等模板引擎渲染邮件内容
 */
@Service
public class EmailService {

    /**
     * JavaMailSender是Spring提供的邮件发送器
     * 【技术说明】需要在application.properties中配置邮件服务器信息
     */
    @Autowired
    private JavaMailSender mailSender;

    /**
     * 发送验证码邮件
     * 
     * @param to 收件人邮箱
     * @param code 验证码
     * @return true表示发送成功，false表示发送失败
     */
    public boolean sendVerificationCode(String to, String code) {
        try {
            // 创建简单邮件消息对象
            SimpleMailMessage message = new SimpleMailMessage();
            
            // 设置发件人邮箱（需与配置文件中的spring.mail.username一致）
            message.setFrom("1379476510@qq.com");
            
            // 设置收件人邮箱
            message.setTo(to);
            
            // 设置邮件主题
            message.setSubject("【MC电影院】验证码");
            
            // 设置邮件正文
            message.setText("这是您的验证码：" + code + "\n\n有效期仅为5分钟，请尽快使用。\n\n如果不是您本人操作，请忽略此邮件。");
            
            // 发送邮件
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            // 打印异常信息（生产环境应使用日志框架）
            e.printStackTrace();
            return false;
        }
    }
}
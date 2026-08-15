package com.example.demo.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * 
 * 【架构说明】
 * 配置Redis连接工厂、序列化器、模板等
 * 
 * 【核心功能】
 * 1. 配置Redis连接工厂
 * 2. 配置Redis序列化器
 * 3. 配置Redis模板
 * 4. 配置StringRedis模板
 * 
 * 【安全风险】
 * 1. 无权限校验：所有Redis操作都需要管理员权限
 * 2. 缓存数据无版本管理：可能出现缓存一致性问题
 * 
 * 【改进建议】
 * 1. 添加管理员权限校验
 * 2. 考虑使用缓存版本号或消息队列保证缓存一致性 
 */
@Configuration
// 当RedisTemplate类存在时才加载配置
@ConditionalOnClass(RedisTemplate.class)
// 当spring.redis.host属性存在时才加载配置
// matchIfMissing = false 表示如果属性不存在，也加载配置
@ConditionalOnProperty(name = "spring.redis.host", matchIfMissing = false)
public class RedisConfig {

    /**
     * 配置Redis连接工厂
     * 
     * @param factory Redis连接工厂
     * @return Redis连接工厂
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 配置Redis连接工厂
        // factory 是Redis连接工厂，用于创建Redis连接
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 配置Redis序列化器
        // objectMapper 是Jackson的ObjectMapper，用于序列化和反序列化Java对象
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置ObjectMapper，支持JavaTime类型 setVisibility是设置所有属性的可见性为ANY
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 注册JavaTime模块，支持序列化和反序列化JavaTime类型
        objectMapper.registerModule(new JavaTimeModule());
        //configure是配置ObjectMapper，支持序列化和反序列化JavaTime类型
        // false 表示不将日期时间类型转换为时间戳，而是保持原始格式
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // 配置ObjectMapper，支持默认类型转换
        // ObjectMapper.DefaultTyping.NON_FINAL 表示不支持final类，只支持非final类
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // 配置Redis模板
        // template 是Redis模板，用于执行Redis操作
        // 设置键序列化器为字符串序列化器
        // 设置哈希键序列化器为字符串序列化器
        // 设置值序列化器为JSON序列化器
        // 设置哈希值序列化器为JSON序列化器
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置StringRedis模板
     * 
     * @param factory Redis连接工厂
     * @return StringRedis模板
     */
    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);// 设置Redis连接工厂
        return template;
    }
}

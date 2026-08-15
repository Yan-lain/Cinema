package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/redis")
public class RedisHealthController {

    @Autowired
    private RedisService redisService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis健康检查端点
     * 
     * 【使用场景】
     * 用于监控Redis服务的健康状态，可配置到监控系统（如Prometheus、Zabbix）
     * 
     * 【返回指标】
     * - status: UP/DOWN
     * - timestamp: 检查时间戳
     * - ping: 响应时间（毫秒）
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        
        long startTime = System.currentTimeMillis();
        
        try {
            String testKey = "health:check:" + System.currentTimeMillis();
            String testValue = "OK";
            
            redisService.set(testKey, testValue, 10, java.util.concurrent.TimeUnit.SECONDS);
            Object value = redisService.get(testKey);
            redisService.delete(testKey);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            result.put("status", "UP");
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("ping", responseTime + "ms");
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            result.put("status", "DOWN");
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("ping", responseTime + "ms");
            result.put("error", e.getMessage());
            
            return ApiResponse.success(result);
        }
    }

    /**
     * 检查Redis连接状态
     * 
     * 【使用场景】
     * 在应用启动或异常恢复后检查Redis是否可用
     */
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        try {
            redisService.set("ping:test", "pong", 5, java.util.concurrent.TimeUnit.SECONDS);
            Object value = redisService.get("ping:test");
            redisService.delete("ping:test");
            
            if ("pong".equals(value)) {
                return ApiResponse.success("PONG");
            } else {
                return ApiResponse.error(500, "Redis响应异常");
            }
        } catch (Exception e) {
            return ApiResponse.error(500, "Redis连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取Redis统计信息
     * 
     * 【使用场景】
     * 运维人员查看Redis运行状态和缓存使用情况
     * 
     * 【返回指标】
     * - cacheKeys: 当前缓存Key数量（通过模式匹配估算）
     * - memoryUsage: 内存使用估算
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("movieCacheCount", redisService.keys("movie:*").size());
            stats.put("scheduleCacheCount", redisService.keys("schedule:*").size());
            stats.put("lockCount", redisService.keys("lock:*").size());
            
            stats.put("timestamp", LocalDateTime.now().toString());
            stats.put("status", "UP");
            
            return ApiResponse.success(stats);
        } catch (Exception e) {
            stats.put("error", e.getMessage());
            stats.put("status", "DOWN");
            return ApiResponse.success(stats);
        }
    }

    /**
     * 手动清除所有缓存（仅限管理员）
     * 
     * 【使用场景】
     * 数据变更后需要强制刷新所有缓存
     * 
     * 【安全说明】
     * 此操作会清除所有Redis数据，生产环境应谨慎使用
     */
    @DeleteMapping("/flush")
    public ApiResponse<Void> flushCache() {
        try {
            redisService.flushDb();
            return ApiResponse.success("缓存已全部清除", null);
        } catch (Exception e) {
            return ApiResponse.error(500, "清除缓存失败: " + e.getMessage());
        }
    }

    /**
     * 清除指定模式的缓存
     * 
     * @param pattern 缓存Key模式，如 "movie:*"
     */
    @DeleteMapping("/flush/pattern")
    public ApiResponse<Void> flushByPattern(@RequestParam String pattern) {
        try {
            var keys = redisService.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisService.delete(keys);
                return ApiResponse.success("成功清除 " + keys.size() + " 个缓存", null);
            }
            return ApiResponse.success("未找到匹配的缓存", null);
        } catch (Exception e) {
            return ApiResponse.error(500, "清除缓存失败: " + e.getMessage());
        }
    }
}

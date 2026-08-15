package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 *
 * 【架构说明】
 * 为 @Async 异步任务和 @Scheduled 定时任务提供专用线程池，
 * 避免使用默认线程池（SimpleAsyncTaskExecutor 每次创建新线程）。
 *
 * 【线程池参数说明】
 * - corePoolSize（核心线程数）：常驻线程，即使空闲也不销毁
 * - maxPoolSize（最大线程数）：当核心线程满且队列也满时，扩容到的最大线程数
 * - queueCapacity（队列容量）：线程池满时，任务排队等待的队列大小
 * - keepAliveSeconds（空闲线程存活时间）：非核心线程空闲超过此时间后销毁
 * - policy（拒绝策略）：当线程池和队列都满时的处理策略
 *   - CallerRunsPolicy：由调用线程执行（不丢弃任务）
 *
 * 【拒绝策略说明】
 * 当线程池资源耗尽时，CallerRunsPolicy 会让提交任务的线程自己执行，
 * 保证任务不丢失，但会阻塞业务线程。适合对任务丢失敏感的场景。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * 异步任务线程池
     *
     * 【设计考量】
     * - 核心线程 5 个：日常异步任务（如订单超时处理、缓存预热）足够使用
     * - 最大线程 20 个：高峰时可扩展，避免线程过多导致上下文切换开销
     * - 队列 200：中等队列，堆积时由 CallerRunsPolicy 兜底
     * - 拒绝策略：CallerRunsPolicy，不丢弃任何任务
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        logger.info("异步任务线程池初始化完成：core=5, max=20, queue=200");
        return executor;
    }
}
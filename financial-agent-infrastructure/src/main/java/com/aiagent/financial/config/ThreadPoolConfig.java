package com.aiagent.financial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * JDK 25 虚拟线程配置。
 * Spring Boot 3.4+ 配合 spring.threads.virtual.enabled=true 已为 Tomcat 启用虚拟线程。
 * 此处为批量 LLM 调用提供专用的虚拟线程执行器。
 */
@Configuration
public class ThreadPoolConfig {

    @Bean("virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 有界虚拟线程执行器，限制并发数。
     * 使用信号量限制并发虚拟线程的数量。
     */
    @Bean("boundedVirtualThreadExecutor")
    public Executor boundedVirtualThreadExecutor(RateLimiterConfig rateLimiterConfig) {
        return task -> {
            Thread.startVirtualThread(() -> {
                try {
                    rateLimiterConfig.concurrentLimiter().acquire();
                    try {
                        task.run();
                    } finally {
                        rateLimiterConfig.concurrentLimiter().release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("线程被中断", e);
                }
            });
        };
    }
}

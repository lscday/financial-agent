package com.aiagent.financial.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Semaphore;

/**
 * 使用自定义信号量的限流器（代替 synchronized 防止虚拟线程 Pinning）。
 * 使用 Semaphore 替代 synchronized 以避免虚拟线程 Pinning。
 */


@Configuration
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterConfig {

    private int maxConcurrent = 16;

    public int getMaxConcurrent() { return maxConcurrent; }
    public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }

    @Bean
    public Semaphore concurrentLimiter() {
        return new Semaphore(maxConcurrent, true);
    }
}

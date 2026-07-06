package com.aiagent.financial.service;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aiagent.financial.llm.StreamingChatService;
import com.aiagent.financial.rag.RAGService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 支持虚拟线程的并发 LLM 服务。
 * 使用 JDK 25 虚拟线程处理批量 LLM 调用，
 * 通过 Semaphore 替代 synchronized 防止虚拟线程 Pinning，
 * 并通过自定义原子计数器进行限流。
 */

@Service
public class ConcurrentLLMService {
    private static final Logger log = LoggerFactory.getLogger(ConcurrentLLMService.class);

    @Getter
    private final StreamingChatService streamingChatService;
    private final RAGService ragService;
    private final Semaphore concurrentLimiter;
    private final Scheduler virtualThreadScheduler;

    public ConcurrentLLMService(StreamingChatService streamingChatService,
                                 RAGService ragService,
                                 Semaphore concurrentLimiter,
                                 @Qualifier("virtualThreadExecutor") Executor virtualThreadExecutor) {
        this.streamingChatService = streamingChatService;
        this.ragService = ragService;
        this.concurrentLimiter = concurrentLimiter;
        this.virtualThreadScheduler = Schedulers.fromExecutor(virtualThreadExecutor);
    }

    // 自定义原子计数器，用于限流
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    private final AtomicInteger rejectedRequests = new AtomicInteger(0);

    /**
     * 带并发控制的 RAG 增强查询。
     */
    public Mono<RAGService.RAGResponse> ragQuery(String question) {
        if (!tryAcquire()) {
            return Mono.error(new RuntimeException("请求被限流，当前并发: " + activeRequests.get()));
        }

        return Mono.fromCallable(() -> {
                    try {
                        return ragService.query(question);
                    } finally {
                        concurrentLimiter.release();
                        activeRequests.decrementAndGet();
                    }
                })
                .subscribeOn(virtualThreadScheduler);
    }

    /**
     * 尝试从限流器获取许可。
     */
    private boolean tryAcquire() {
        boolean acquired = concurrentLimiter.tryAcquire();
        if (acquired) {
            activeRequests.incrementAndGet();
        } else {
            rejectedRequests.incrementAndGet();
            log.warn("超出限流。活跃：{}，被拒绝：{}",
                    activeRequests.get(), rejectedRequests.get());
        }
        return acquired;
    }

    public record RateLimiterStatus(
            int activeRequests,
            int totalRequests,
            int rejectedRequests,
            int availablePermits,
            int queueLength
    ) {}
}

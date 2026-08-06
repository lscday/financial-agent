package com.aiagent.financial.controller;

import com.aiagent.financial.application.command.RagQueryCommand;
import com.aiagent.financial.service.ConcurrentLLMService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * RAG（检索增强生成）控制器。
 * 提供基于上下文的问答端点。
 */
@RestController
@RequestMapping("/api/rag")
public class RAGController {

    private final ConcurrentLLMService concurrentLLMService;

    /**
     * 构造控制器。
     *
     * @param concurrentLLMService 并发 LLM 服务
     */
    public RAGController(ConcurrentLLMService concurrentLLMService) {
        this.concurrentLLMService = concurrentLLMService;
    }

    /**
     * 带有上下文检索的 RAG 增强查询。
     *
     * @param question 用户问题
     * @return RAG 查询响应
     */
    @GetMapping("/query")
    public Mono<RagQueryCommand.Response> query(@RequestParam String question) {
        return concurrentLLMService.ragQuery(question)
                .map(RagQueryCommand.Response::from);
    }
}

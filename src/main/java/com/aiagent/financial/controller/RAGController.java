package com.aiagent.financial.controller;

import com.aiagent.financial.rag.RAGService;
import com.aiagent.financial.service.ConcurrentLLMService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * RAG（检索增强生成）控制器。
 * 提供文档导入和基于上下文的问答端点。
 */
@RestController
@RequestMapping("/api/rag")

public class RAGController {

    private final ConcurrentLLMService concurrentLLMService;

    public RAGController(ConcurrentLLMService concurrentLLMService) {
        this.concurrentLLMService = concurrentLLMService;
    }

    /**
     * 带有上下文检索的 RAG 增强查询。
     */
    @GetMapping("/query")
    public Mono<RAGService.RAGResponse> query(@RequestParam String question) {
        return concurrentLLMService.ragQuery(question);
    }
}

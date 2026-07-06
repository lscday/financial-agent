package com.aiagent.financial.controller;

import com.aiagent.financial.llm.StreamingChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * SSE 流式聊天控制器。
 * 提供流式和非流式聊天端点。
 */
@RestController
@RequestMapping("/api/chat")

public class ChatController {

    private final StreamingChatService streamingChatService;

    public ChatController(StreamingChatService streamingChatService) {
        this.streamingChatService = streamingChatService;
    }

    /**
     * SSE 流式聊天端点。
     * 返回 text/event-stream 用于实时流式输出。
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam String question,
            @RequestParam(required = false, defaultValue = "default") String conversationId) {
        return streamingChatService.stream(question, conversationId);
    }

    /**
     * 非流式聊天端点。
     */
    @PostMapping("/ask")
    public String ask(@RequestParam String question) {
        return streamingChatService.chat(question);
    }
}

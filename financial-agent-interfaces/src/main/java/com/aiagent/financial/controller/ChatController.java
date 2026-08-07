package com.aiagent.financial.controller;

import com.aiagent.financial.application.command.ChatCommand;
import com.aiagent.financial.llm.StreamingChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * SSE 流式聊天控制器。
 * 提供流式和非流式聊天端点。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final StreamingChatService streamingChatService;

    /**
     * 构造控制器。
     *
     * @param streamingChatService 流式聊天服务
     */
    public ChatController(StreamingChatService streamingChatService) {
        this.streamingChatService = streamingChatService;
    }

    /**
     * SSE 流式聊天端点。
     * 浏览器 EventSource 仅支持 GET，故保留查询参数。
     *
     * @param question       用户问题
     * @param conversationId 会话标识（多轮对话时区分会话，可选）
     * @return 文本事件流
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam String question,
            @RequestParam(required = false, defaultValue = "default") String conversationId) {
        return streamingChatService.stream(question, conversationId);
    }

    /**
     * 非流式聊天端点。
     *
     * @param request 聊天请求
     * @return 回答文本
     */
    @PostMapping("/ask")
    public String ask(@RequestBody ChatCommand.AskRequest request) {
        return streamingChatService.chat(request.question());
    }
}

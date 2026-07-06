package com.aiagent.financial.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * DeepSeek LLM 工厂（兼容 OpenAI 的 API）。
 * 使用 langchain4j-open-ai，将自定义 base URL 指向 DeepSeek API。
 */
@Component
public class ChatModelFactory {

    private final String apiKey;
    private final String baseUrl;
    private final String chatModel;
    private final double temperature;
    private final int maxTokens;

    public ChatModelFactory(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.base-url}") String baseUrl,
            @Value("${deepseek.chat-model}") String chatModel,
            @Value("${deepseek.temperature}") double temperature,
            @Value("${deepseek.max-tokens}") int maxTokens) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.chatModel = chatModel;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public ChatModel createChatModel() {
        return dev.langchain4j.model.openai.OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(chatModel)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    public StreamingChatModel createStreamingChatModel() {
        return dev.langchain4j.model.openai.OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(chatModel)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}

package com.aiagent.financial.config;

import com.aiagent.financial.llm.ChatModelFactory;
import com.aiagent.financial.llm.EmbeddingModelFactory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 全局配置。
 * 从自定义工厂装配 ChatLanguageModel 和 EmbeddingModel 的 Bean。
 */
@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatModel chatModel(ChatModelFactory chatModelFactory) {
        return chatModelFactory.createChatModel();
    }

    @Bean
    public StreamingChatModel streamingChatModel(ChatModelFactory chatModelFactory) {
        return chatModelFactory.createStreamingChatModel();
    }

    @Bean
    public EmbeddingModel embeddingModel(EmbeddingModelFactory embeddingModelFactory) {
        return embeddingModelFactory.createEmbeddingModel();
    }
}

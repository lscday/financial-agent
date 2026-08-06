package com.aiagent.financial.llm;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * DashScope 百炼 Embedding 模型工厂。
 * 通过阿里云百炼服务兼容端点使用 text-embedding-v3。
 */
@Component
public class EmbeddingModelFactory {

    private final String apiKey;
    private final String baseUrl;
    private final String embeddingModel;

    public EmbeddingModelFactory(
            @Value("${tongyi.api-key}") String apiKey,
            @Value("${tongyi.base-url}") String baseUrl,
            @Value("${tongyi.embedding-model}") String embeddingModel) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.embeddingModel = embeddingModel;
    }

    public EmbeddingModel createEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embeddingModel)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(3)
                .build();
    }
}

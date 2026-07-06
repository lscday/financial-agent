package com.aiagent.financial.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 使用阿里云 Embedding 模型对文本片段进行向量化的服务。
 */
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 对单个文本片段进行向量化。
     */
    public Embedding embed(TextSegment segment) {
        return embeddingModel.embed(segment).content();
    }

    /**
     * 批量对多个文本片段进行向量化。
     */
    public List<Embedding> embedAll(List<TextSegment> segments) {
        return embeddingModel.embedAll(segments).content();
    }

    /**
     * 对原始文本字符串进行向量化。
     */
    public Embedding embedText(String text) {
        return embeddingModel.embed(text).content();
    }

    /**
     * 获取向量嵌入的维度。
     */
    public int dimension() {
        return embeddingModel.dimension();
    }
}

package com.aiagent.financial.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 语义检索服务。
 * 给定查询，将其向量化并在向量存储中搜索相似文档。
 */
@Service
public class RetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final int maxSegments;
    private final double minScore;

    public RetrievalService(EmbeddingService embeddingService,
                             VectorStoreService vectorStoreService,
                             @Value("${rag.max-segments:5}") int maxSegments,
                             @Value("${rag.min-score:0.6}") double minScore) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.maxSegments = maxSegments;
        this.minScore = minScore;
    }

    /**
     * 检索与查询相关的上下文。
     */
    public RetrievalResult retrieve(String query) {
        // 1. 对查询进行向量化
        var queryEmbedding = embeddingService.embedText(query);

        // 2. 搜索向量存储
        var matches = vectorStoreService.search(queryEmbedding, maxSegments, minScore);

        // 3. Build result with context text
        var contextBuilder = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            var match = matches.get(i);
            contextBuilder.append("【参考文档 ").append(i + 1).append("】")
                    .append("（来源：").append(match.embedded().metadata().getString("source"))
                    .append("，相似度：").append(String.format("%.2f", match.score())).append("）\n")
                    .append(match.embedded().text()).append("\n\n");
        }

        return new RetrievalResult(
                matches,
                contextBuilder.toString(),
                matches.stream()
                        .map(m -> m.embedded().metadata().getString("source"))
                        .distinct()
                        .toList()
        );
    }

    /**
     * 检索操作的结果。
     */
    public record RetrievalResult(
            List<EmbeddingMatch<TextSegment>> matches,
            String contextText,
            List<String> sources
    ) {}
}

package com.aiagent.financial.rag;

import com.aiagent.financial.domain.model.rag.VectorMatch;
import com.aiagent.financial.domain.repository.EmbeddedVectorRepository;
import dev.langchain4j.data.embedding.Embedding;
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

    private final EmbeddedVectorRepository vectorRepository;

    private final int maxSegments;

    private final double minScore;

    public RetrievalService(EmbeddingService embeddingService,
                            EmbeddedVectorRepository vectorRepository,
                            @Value("${rag.max-segments:5}") int maxSegments,
                            @Value("${rag.min-score:0.6}") double minScore) {
        this.embeddingService = embeddingService;
        this.vectorRepository = vectorRepository;
        this.maxSegments = maxSegments;
        this.minScore = minScore;
    }

    /**
     * 检索与查询相关的上下文。
     *
     * @param query 用户查询文本
     * @return 检索结果
     */
    public RetrievalResult retrieve(String query) {
        // 1. 对查询进行向量化
        Embedding queryEmbedding = embeddingService.embedText(query);

        // 2. 搜索向量存储
        List<VectorMatch> matches = vectorRepository.findSimilar(queryEmbedding.vector(), maxSegments, minScore);

        // 3. 拼接上下文文本
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            VectorMatch match = matches.get(i);
            contextBuilder.append("【参考文档 ").append(i + 1).append("】")
                    .append("（来源：").append(match.source())
                    .append("，相似度：").append(String.format("%.2f", match.score())).append("）\n")
                    .append(match.text()).append("\n\n");
        }

        return new RetrievalResult(
                matches,
                contextBuilder.toString(),
                matches.stream().map(VectorMatch::source).distinct().toList()
        );
    }

    /**
     * 检索操作的结果。
     *
     * @param matches     匹配结果
     * @param contextText 拼接后的上下文文本
     * @param sources     来源文档名去重列表
     */
    public record RetrievalResult(
            List<VectorMatch> matches,
            String contextText,
            List<String> sources
    ) {
    }
}

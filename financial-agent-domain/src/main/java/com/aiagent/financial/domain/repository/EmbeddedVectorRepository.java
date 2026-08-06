package com.aiagent.financial.domain.repository;

import com.aiagent.financial.domain.model.rag.VectorEntry;
import com.aiagent.financial.domain.model.rag.VectorMatch;

import java.util.List;
import java.util.Map;

/**
 * 向量存储仓储接口。
 *
 * <p>封装语义向量库的读写能力。接口定义在领域层，使用自描述类型，
 * 实现在基础设施层（本地内存 / 本地 ES / 阿里云 ES）。</p>
 */
public interface EmbeddedVectorRepository {

    /**
     * 批量存储向量条目。
     *
     * @param entries 向量条目列表
     */
    void storeAll(List<VectorEntry> entries);

    /**
     * 根据查询向量检索相似条目。
     *
     * @param queryVector 查询向量
     * @param topK        返回的最大条目数
     * @param minScore    最小相似度阈值，低于该值的结果不返回
     * @return 相似度降序的匹配结果列表
     */
    List<VectorMatch> findSimilar(float[] queryVector, int topK, double minScore);

    /**
     * 保存文档哈希（文件名 → SHA256），用于检测文档变更。
     *
     * @param hashes 文档哈希映射
     */
    void saveDocHashes(Map<String, String> hashes);

    /**
     * 读取全部文档哈希。
     *
     * @return 文档哈希映射，无记录时返回空 Map
     */
    Map<String, String> getAllDocHashes();

    /**
     * 统计向量条目数量。
     *
     * @return 条目总数
     */
    long count();
}

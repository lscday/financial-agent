package com.aiagent.financial.domain.model.rag;

/**
 * 向量存储条目值对象。
 *
 * <p>一个文本片段及其向量化结果，用于写入向量存储。
 * 领域层使用自描述类型，不依赖任何向量数据库框架。</p>
 *
 * @param id        文档条目标识
 * @param text      文本内容
 * @param source    来源文档名
 * @param category  分类
 * @param embedding 向量化结果
 */
public record VectorEntry(String id, String text, String source, String category, float[] embedding) {
}

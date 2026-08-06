package com.aiagent.financial.domain.model.rag;

/**
 * 向量检索匹配结果值对象。
 *
 * @param id       匹配条目标识
 * @param text     匹配文本
 * @param source   来源文档名
 * @param category 分类
 * @param score    相似度分数
 */
public record VectorMatch(String id, String text, String source, String category, double score) {
}

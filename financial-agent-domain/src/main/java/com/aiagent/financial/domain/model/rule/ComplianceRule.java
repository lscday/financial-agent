package com.aiagent.financial.domain.model.rule;

import lombok.Getter;

/**
 * 合规规则实体。
 *
 * <p>由 RAG 从政策文档中检索得到，作为风险评估与清算的规则依据。
 * 不可变，一旦创建即固定内容。</p>
 */
@Getter
public class ComplianceRule {

    private final String source;

    private final String category;

    private final String content;

    /**
     * 构造合规规则。
     *
     * @param source   规则来源文档名
     * @param category 规则分类
     * @param content  规则正文
     */
    public ComplianceRule(String source, String category, String content) {
        this.source = source;
        this.category = category;
        this.content = content;
    }

    /**
     * 判断规则是否包含指定关键词。
     *
     * @param keyword 关键词
     * @return true 表示规则正文包含该关键词
     */
    public boolean containsKeyword(String keyword) {
        return content != null && content.contains(keyword);
    }
}

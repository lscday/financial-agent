package com.aiagent.financial.domain.repository;

import com.aiagent.financial.domain.model.rule.ComplianceRule;

import java.util.List;

/**
 * 合规规则仓储接口。
 *
 * <p>从政策文档 / 向量库中检索合规规则，接口定义在领域层。</p>
 */
public interface ComplianceRuleRepository {

    /**
     * 根据查询文本检索相关合规规则。
     *
     * @param query 用户查询文本
     * @return 匹配的合规规则列表
     */
    List<ComplianceRule> findByQuery(String query);
}

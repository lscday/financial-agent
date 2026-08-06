package com.aiagent.financial.domain.repository;

import com.aiagent.financial.domain.model.settlement.Settlement;
import com.aiagent.financial.domain.model.settlement.SettlementId;

import java.util.Optional;

/**
 * 清算仓储接口。
 *
 * <p>接口定义在领域层，实现在基础设施层。</p>
 */
public interface SettlementRepository {

    /**
     * 保存清算记录。
     *
     * @param settlement 清算实体
     */
    void save(Settlement settlement);

    /**
     * 按清算编号查询记录。
     *
     * @param settlementId 清算编号
     * @return 匹配的清算记录，不存在时返回 {@link Optional#empty()}
     */
    Optional<Settlement> findById(SettlementId settlementId);
}

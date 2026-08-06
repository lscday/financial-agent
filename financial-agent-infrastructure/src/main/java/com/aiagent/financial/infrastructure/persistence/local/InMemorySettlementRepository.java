package com.aiagent.financial.infrastructure.persistence.local;

import com.aiagent.financial.domain.model.settlement.Settlement;
import com.aiagent.financial.domain.model.settlement.SettlementId;
import com.aiagent.financial.domain.repository.SettlementRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存清算仓储。
 *
 * <p>模拟清算记录的持久化，本地运行使用。真实场景可替换为数据库实现。</p>
 */
@Repository
public class InMemorySettlementRepository implements SettlementRepository {

    private final Map<SettlementId, Settlement> records = new ConcurrentHashMap<>();

    @Override
    public void save(Settlement settlement) {
        records.put(settlement.getSettlementId(), settlement);
    }

    @Override
    public Optional<Settlement> findById(SettlementId settlementId) {
        return Optional.ofNullable(records.get(settlementId));
    }
}

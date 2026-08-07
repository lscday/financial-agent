package com.aiagent.financial.application.service;

import com.aiagent.financial.application.command.SettlementQueryCommand;
import com.aiagent.financial.domain.model.settlement.Settlement;
import com.aiagent.financial.domain.model.settlement.SettlementId;
import com.aiagent.financial.domain.repository.SettlementRepository;
import org.springframework.stereotype.Service;

/**
 * 清算状态查询应用服务。
 *
 * <p>编排"按清算编号查询状态"的读用例。不含业务规则，
 * 直接委托领域仓储接口。查不到记录时返回 null 由接口层处理。</p>
 */
@Service
public class SettlementQueryAppService {

    private final SettlementRepository settlementRepository;

    /**
     * 构造清算状态查询服务。
     *
     * @param settlementRepository 清算仓储（领域接口）
     */
    public SettlementQueryAppService(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }

    /**
     * 按清算编号查询状态。
     *
     * @param settlementId 清算编号
     * @return 清算状态响应，记录不存在时返回 null
     */
    public SettlementQueryCommand.Response queryStatus(String settlementId) {
        Settlement settlement = settlementRepository.findById(new SettlementId(settlementId)).orElse(null);
        if (settlement == null) {
            return null;
        }
        return new SettlementQueryCommand.Response(
                settlement.getSettlementId().value(),
                settlement.getStatus().getDescription(),
                settlement.getCreatedAt() != null ? settlement.getCreatedAt().toString() : "");
    }
}

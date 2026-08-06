package com.aiagent.financial.domain.model.settlement;

import com.aiagent.financial.domain.constant.SettlementStatusEnum;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 资金清算实体（聚合根，状态机）。
 *
 * <p>状态流转：PENDING → COMPLETED / FAILED。状态变更必须通过行为方法，
 * 非法流转抛出 {@link IllegalStateException}。禁止使用 {@code @Setter}。</p>
 */
@Getter
public class Settlement {

    private final SettlementId settlementId;

    private final String query;

    private final String businessData;

    private final String riskInfo;

    private SettlementStatusEnum status;

    private final String detail;

    private final LocalDateTime createdAt;

    /**
     * 构造待处理的清算实体。
     *
     * @param settlementId 清算编号
     * @param query        原始查询
     * @param businessData 产品查询返回的数据
     * @param riskInfo     风险评估结果
     */
    public Settlement(SettlementId settlementId, String query, String businessData, String riskInfo) {
        this.settlementId = settlementId;
        this.query = query;
        this.businessData = businessData;
        this.riskInfo = riskInfo;
        this.status = SettlementStatusEnum.PENDING;
        this.detail = "";
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 标记清算成功。
     *
     * <p>仅待处理状态可流转，非法状态抛出 {@link IllegalStateException}。</p>
     *
     * @param resultDetail 清算成功的明细
     */
    public void complete(String resultDetail) {
        if (this.status != SettlementStatusEnum.PENDING) {
            throw new IllegalStateException("仅待处理状态的清算可标记成功，当前状态: " + this.status.getCode());
        }
        this.status = SettlementStatusEnum.COMPLETED;
    }

    /**
     * 标记清算失败。
     *
     * <p>仅待处理状态可流转，非法状态抛出 {@link IllegalStateException}。</p>
     *
     * @param errorDetail 失败原因
     */
    public void fail(String errorDetail) {
        if (this.status != SettlementStatusEnum.PENDING) {
            throw new IllegalStateException("仅待处理状态的清算可标记失败，当前状态: " + this.status.getCode());
        }
        this.status = SettlementStatusEnum.FAILED;
    }

    /**
     * 判断清算是否已成功。
     *
     * @return true 表示清算成功
     */
    public boolean isCompleted() {
        return this.status == SettlementStatusEnum.COMPLETED;
    }
}

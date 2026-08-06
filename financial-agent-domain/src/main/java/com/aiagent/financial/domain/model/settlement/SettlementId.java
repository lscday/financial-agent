package com.aiagent.financial.domain.model.settlement;

/**
 * 清算编号值对象。
 *
 * @param value 清算唯一编号（形如 STL20260806xxxxx）
 */
public record SettlementId(String value) {

    /**
     * 构造清算编号。
     *
     * @param value 清算编号，不可为空
     */
    public SettlementId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("清算编号不能为空");
        }
    }
}

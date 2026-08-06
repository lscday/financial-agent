package com.aiagent.financial.domain.model.product;

/**
 * 产品标识值对象。
 *
 * @param value 产品唯一标识（产品代码）
 */
public record ProductId(String value) {

    /**
     * 构造产品标识。
     *
     * @param value 产品代码，不可为空
     */
    public ProductId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("产品代码不能为空");
        }
    }
}

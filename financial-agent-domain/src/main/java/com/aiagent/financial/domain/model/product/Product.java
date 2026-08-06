package com.aiagent.financial.domain.model.product;

import lombok.Getter;

/**
 * 金融产品实体（聚合根）。
 *
 * <p>封装产品的基础属性与业务行为。状态变更必须通过行为方法，
 * 禁止使用 {@code @Setter} 直接修改字段。</p>
 */
@Getter
public class Product {

    private final ProductId productId;

    private final String name;

    private final String code;

    private final String type;

    private final String expectedRate;

    private final String riskLevel;

    private final String term;

    private final String minInvestment;

    private final boolean onSale;

    /**
     * 构造产品实体。
     *
     * @param productId      产品唯一标识
     * @param name           产品名称
     * @param code           产品代码
     * @param type           产品类型
     * @param expectedRate   预期年化收益率
     * @param riskLevel      风险等级
     * @param term           投资期限
     * @param minInvestment  起投金额
     * @param onSale         是否在售
     */
    public Product(ProductId productId, String name, String code, String type,
                   String expectedRate, String riskLevel, String term,
                   String minInvestment, boolean onSale) {
        this.productId = productId;
        this.name = name;
        this.code = code;
        this.type = type;
        this.expectedRate = expectedRate;
        this.riskLevel = riskLevel;
        this.term = term;
        this.minInvestment = minInvestment;
        this.onSale = onSale;
    }

    /**
     * 判断产品是否可销售（在售状态）。
     *
     * @return true 表示在售
     */
    public boolean isAvailable() {
        return onSale;
    }
}

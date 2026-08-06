package com.aiagent.financial.domain.model.settlement;

/**
 * 清算执行器领域接口。
 *
 * <p>定义执行资金清算的业务能力，由基础设施层提供真实/模拟实现。
 * 领域层仅依赖接口，不关心具体清算渠道。</p>
 */
public interface SettlementExecutor {

    /**
     * 执行资金清算并返回结果实体。
     *
     * @param settlement 待执行的清算实体
     * @return 执行后的清算结果文本
     */
    String execute(Settlement settlement);
}

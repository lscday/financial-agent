package com.aiagent.financial.domain.service;

import com.aiagent.financial.domain.constant.RiskLevelEnum;
import org.springframework.stereotype.Service;

/**
 * 金融咨询工作流路由领域服务。
 *
 * <p>封装 Agent 状态图的路由规则：高风险是否进入分析、清算失败是否重试等。
 * 路由判断属于业务规则，收敛到领域层，图结构（application 层）只负责装配边。</p>
 */
@Service
public class AdvisoryWorkflowPolicy {

    /**
     * 判断风险等级是否应直接进入分析节点。
     *
     * @param riskLevel 风险等级
     * @return true 表示高风险，应直接进入分析
     */
    public boolean shouldAnalyzeDirectly(RiskLevelEnum riskLevel) {
        return riskLevel == RiskLevelEnum.HIGH;
    }

    /**
     * 判断清算后应进入哪个节点。
     *
     * <p>清算成功 → 结束；失败且未超过重试上限 → 重试清算；
     * 失败且已达重试上限 → 进入分析。</p>
     *
     * @param settled    清算是否已成功
     * @param retryCount 已尝试次数
     * @param maxRetries 最大重试次数
     * @return 下一个节点的路由结果
     */
    public SettlementRoute decideSettlementRoute(boolean settled, int retryCount, int maxRetries) {
        if (settled) {
            return SettlementRoute.END;
        }
        if (retryCount >= maxRetries) {
            return SettlementRoute.ANALYZE;
        }
        return SettlementRoute.RETRY;
    }

    /**
     * 清算路由结果。
     */
    public enum SettlementRoute {
        /** 结束工作流 */
        END,
        /** 进入分析节点 */
        ANALYZE,
        /** 重试清算 */
        RETRY
    }
}

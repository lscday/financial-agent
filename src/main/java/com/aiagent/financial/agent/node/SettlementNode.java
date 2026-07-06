package com.aiagent.financial.agent.node;

import com.aiagent.financial.agent.state.AgentState;
import com.aiagent.financial.tool.FundSettlementTool;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 清算节点。
 * 执行资金清算模拟，包含重试逻辑。
 * 如果清算失败，增加 retryCount 供循环控制器使用。
 */
public class SettlementNode implements NodeAction<AgentState> {
    private static final Logger log = LoggerFactory.getLogger(SettlementNode.class);

    private final FundSettlementTool fundSettlementTool;

    public SettlementNode(FundSettlementTool fundSettlementTool) {
        this.fundSettlementTool = fundSettlementTool;
    }

    @Override
    public Map<String, Object> apply(AgentState state) {
        int retry = state.getRetryCount();
        log.info("=== SettlementNode（第 {}/{} 次尝试）", retry + 1, 3);

        try {
            String result = fundSettlementTool.settle(
                    state.getQuery(),
                    state.getQueryResult(),
                    state.getRiskResult()
            );

            boolean success = result.contains("成功") || result.contains("completed");
            int newRetryCount = success ? retry : retry + 1;

            List<String> history = new ArrayList<>(state.getHistory());
            history.add("Settlement");

            return Map.of(
                    "settlementResult", result,
                    "settlementSuccess", success,
                    "retryCount", newRetryCount,
                    "history", history
            );
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "未知异常";
            log.error("SettlementNode 异常", e);
            List<String> history = new ArrayList<>(state.getHistory());
            history.add("Settlement");
            return Map.of(
                    "settlementResult", "清算异常: " + msg,
                    "settlementSuccess", false,
                    "retryCount", retry + 1,
                    "lastError", msg,
                    "history", history
            );
        }
    }
}

package com.aiagent.financial.agent.node;

import com.aiagent.financial.agent.state.AgentState;
import com.aiagent.financial.tool.RiskAssessmentTool;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 风险检查节点。
 * 使用风险评估工具分析检索到的规则与查询之间的风险因素。
 */
public class RiskCheckNode implements NodeAction<AgentState> {
    private static final Logger log = LoggerFactory.getLogger(RiskCheckNode.class);

    private final RiskAssessmentTool riskAssessmentTool;

    public RiskCheckNode(RiskAssessmentTool riskAssessmentTool) {
        this.riskAssessmentTool = riskAssessmentTool;
    }

    @Override
    public Map<String, Object> apply(AgentState state) {
        log.info("=== RiskCheckNode 评估风险");

        try {
            String riskResult = riskAssessmentTool.assess(
                    state.getQuery(),
                    state.getRetrievedRules()
            );

            boolean hasRisk = !riskResult.contains("低风险");
            String riskLevel = riskResult.contains("高风险") ? "HIGH" :
                    riskResult.contains("中等风险") ? "MEDIUM" : "LOW";

            List<String> history = new ArrayList<>(state.getHistory());
            history.add("RiskCheck");

            return Map.of(
                    "riskResult", riskResult,
                    "hasRisk", hasRisk,
                    "riskLevel", riskLevel,
                    "history", history
            );
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "未知异常";
            log.error("RiskCheckNode 异常", e);
            List<String> history = new ArrayList<>(state.getHistory());
            history.add("RiskCheck");
            return Map.of(
                    "riskResult", "风险校验异常: " + msg,
                    "hasRisk", true,
                    "riskLevel", "UNKNOWN",
                    "lastError", msg,
                    "history", history
            );
        }
    }
}

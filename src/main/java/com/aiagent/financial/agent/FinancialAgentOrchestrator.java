package com.aiagent.financial.agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aiagent.financial.agent.graph.FinancialAgentGraph;
import com.aiagent.financial.agent.state.AgentState;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 编排多 Agent 工作流。
 * 提供执行 Agent 图的高级 API，支持检查点持久化和恢复。
 */

@Service

public class FinancialAgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(FinancialAgentOrchestrator.class);

    private final FinancialAgentGraph agentGraph;

    public FinancialAgentOrchestrator(FinancialAgentGraph agentGraph) {
        this.agentGraph = agentGraph;
    }

    /**
     * 通过完整的 Agent 流水线执行金融查询。
     */
    public AgentExecutionResult execute(String query, String businessType) {
        log.info("启动 Agent 工作流: {}", query);

        long startTime = System.currentTimeMillis();

        try {
            // 使用初始状态运行编译后的图
            var result = agentGraph.getCompiledGraph()
                    .invoke(Map.of("query", query, "businessType", businessType != null ? businessType : "general"));

            long elapsed = System.currentTimeMillis() - startTime;

            if (result.isPresent()) {
                AgentState state = result.get();
                log.info("Agent 工作流完成，耗时 {}ms，历史: {}", elapsed, state.getHistory());
                return AgentExecutionResult.success(state, elapsed);
            } else {
                return AgentExecutionResult.failure(null, "图执行返回空结果", elapsed);
            }

        } catch (Exception e) {
            log.error("Agent 工作流失败", e);
            long elapsed = System.currentTimeMillis() - startTime;
            return AgentExecutionResult.failure(null, e.getMessage(), elapsed);
        }
    }

    public record AgentExecutionResult(
            boolean success,
            AgentState finalState,
            String summary,
            java.util.List<String> history,
            long elapsedMs,
            String error
    ) {
        static AgentExecutionResult success(AgentState state, long elapsedMs) {
            return new AgentExecutionResult(true, state, buildSummary(state),
                    state.getHistory(), elapsedMs, null);
        }

        static AgentExecutionResult failure(AgentState state, String error, long elapsedMs) {
            return new AgentExecutionResult(false, state, "执行失败: " + error,
                    state != null ? state.getHistory() : java.util.List.of(), elapsedMs, error);
        }

        private static String buildSummary(AgentState state) {
            if (state.getAnalysisResult() != null && !state.getAnalysisResult().isEmpty()) {
                return state.getAnalysisResult();
            }
            if (state.getSettlementResult() != null && !state.getSettlementResult().isEmpty()) {
                return state.getSettlementResult();
            }
            if (state.getRiskResult() != null && !state.getRiskResult().isEmpty()) {
                return state.getRiskResult();
            }
            return "查询处理完成。";
        }
    }
}

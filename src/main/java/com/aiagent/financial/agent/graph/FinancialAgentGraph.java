package com.aiagent.financial.agent.graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aiagent.financial.agent.node.*;
import com.aiagent.financial.agent.state.AgentState;
import com.aiagent.financial.rag.RetrievalService;
import com.aiagent.financial.tool.FundSettlementTool;
import com.aiagent.financial.tool.ProductQueryTool;
import com.aiagent.financial.tool.RiskAssessmentTool;
import dev.langchain4j.model.chat.ChatModel;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.EdgeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 基于 LangGraph4j 的金融 Agent 状态图。
 *
 * 流程：START → RULE_RETRIEVAL → RISK_CHECK
 *         ┌── [高风险] → ANALYSIS → END
 *         └── [正常] → DATA_QUERY → SETTLEMENT
 *              ┌── [重试 < 3 且失败] → SETTLEMENT（循环）
 *              └── [成功或重试 >= 3] → ANALYSIS/END
 */
@Component
public class FinancialAgentGraph {

    private static final Logger log = LoggerFactory.getLogger(FinancialAgentGraph.class);

    private final CompiledGraph<AgentState> compiledGraph;

    public CompiledGraph<AgentState> getCompiledGraph() {
        return compiledGraph;
    }

    public FinancialAgentGraph(RetrievalService retrievalService,
                               RiskAssessmentTool riskAssessmentTool,
                               ProductQueryTool productQueryTool,
                               FundSettlementTool fundSettlementTool,
                               ChatModel chatModel) {
        try {
            var ruleRetrievalNode = new RuleRetrievalNode(retrievalService);
            var riskCheckNode = new RiskCheckNode(riskAssessmentTool);
            var dataQueryNode = new DataQueryNode(productQueryTool);
            var settlementNode = new SettlementNode(fundSettlementTool);
            var analysisNode = new AnalysisNode(chatModel);

            StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);

            // 添加节点（将同步 NodeAction 包装为 AsyncNodeAction 以兼容）
            graph.addNode(AgentNode.RULE_RETRIEVAL.nodeName(), AsyncNodeAction.node_async(ruleRetrievalNode));
            graph.addNode(AgentNode.RISK_CHECK.nodeName(), AsyncNodeAction.node_async(riskCheckNode));
            graph.addNode(AgentNode.DATA_QUERY.nodeName(), AsyncNodeAction.node_async(dataQueryNode));
            graph.addNode(AgentNode.SETTLEMENT.nodeName(), AsyncNodeAction.node_async(settlementNode));
            graph.addNode(AgentNode.ANALYSIS.nodeName(), AsyncNodeAction.node_async(analysisNode));

            // 入口点：START → RULE_RETRIEVAL
            graph.addEdge(GraphDefinition.START, AgentNode.RULE_RETRIEVAL.nodeName());

            // 直接边：RULE_RETRIEVAL → RISK_CHECK
            graph.addEdge(AgentNode.RULE_RETRIEVAL.nodeName(), AgentNode.RISK_CHECK.nodeName());

            // 条件边：RISK_CHECK → ANALYSIS（高风险）或 DATA_QUERY（正常）
            EdgeAction<AgentState> riskRouting = state -> {
                if ("HIGH".equals(state.getRiskLevel())) {
                    log.info("高风险 → 路由到分析节点");
                    return AgentNode.ANALYSIS.nodeName();
                }
                log.info("正常风险 → 路由到数据查询节点");
                return AgentNode.DATA_QUERY.nodeName();
            };
            graph.addConditionalEdges(
                    AgentNode.RISK_CHECK.nodeName(),
                    AsyncEdgeAction.edge_async(riskRouting),
                    Map.of(AgentNode.ANALYSIS.nodeName(), AgentNode.ANALYSIS.nodeName(),
                            AgentNode.DATA_QUERY.nodeName(), AgentNode.DATA_QUERY.nodeName())
            );

            // 直接边：DATA_QUERY → SETTLEMENT
            graph.addEdge(AgentNode.DATA_QUERY.nodeName(), AgentNode.SETTLEMENT.nodeName());

            // 直接边：ANALYSIS → END
            graph.addEdge(AgentNode.ANALYSIS.nodeName(), GraphDefinition.END);

            // 条件边：清算重试循环（最多 3 次）
            EdgeAction<AgentState> settlementRouting = state -> {
                if (state.isSettlementSuccess()) {
                    log.info("清算成功 → 结束");
                    return GraphDefinition.END;
                }
                if (state.getRetryCount() >= 3) {
                    log.warn("3 次重试后清算仍然失败 → 进入分析节点");
                    return AgentNode.ANALYSIS.nodeName();
                }
                log.info("清算第 {}/3 次尝试失败 → 重试", state.getRetryCount());
                return AgentNode.SETTLEMENT.nodeName();
            };
            graph.addConditionalEdges(
                    AgentNode.SETTLEMENT.nodeName(),
                    AsyncEdgeAction.edge_async(settlementRouting),
                    Map.of(GraphDefinition.END, GraphDefinition.END,
                            AgentNode.ANALYSIS.nodeName(), AgentNode.ANALYSIS.nodeName(),
                            AgentNode.SETTLEMENT.nodeName(), AgentNode.SETTLEMENT.nodeName())
            );

            this.compiledGraph = graph.compile();

        } catch (Exception e) {
            log.error("构建 Agent 图失败", e);
            throw new RuntimeException("构建 Agent 图失败", e);
        }
    }
}

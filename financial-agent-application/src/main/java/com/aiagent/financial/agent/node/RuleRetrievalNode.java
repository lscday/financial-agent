package com.aiagent.financial.agent.node;

import com.aiagent.financial.agent.state.AgentState;
import com.aiagent.financial.rag.RetrievalService;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 规则检索节点。
 * 在向量存储中搜索与当前查询相关的金融规则和政策。
 */
public class RuleRetrievalNode implements NodeAction<AgentState> {
    private static final Logger log = LoggerFactory.getLogger(RuleRetrievalNode.class);

    private final RetrievalService retrievalService;

    public RuleRetrievalNode(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Override
    public Map<String, Object> apply(AgentState state) {
        log.info("=== RuleRetrievalNode 处理查询: {}", state.getQuery());

        try {
            var result = retrievalService.retrieve(state.getQuery());
            List<String> history = new ArrayList<>(state.getHistory());
            history.add("RuleRetrieval");

            return Map.of(
                    "retrievedRules", result.contextText(),
                    "ruleCount", result.matches().size(),
                    "history", history
            );
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "未知异常";
            log.error("RuleRetrievalNode 异常", e);
            List<String> history = new ArrayList<>(state.getHistory());
            history.add("RuleRetrieval");
            return Map.of(
                    "retrievedRules", "规则检索失败: " + msg,
                    "ruleCount", 0,
                    "lastError", msg,
                    "history", history
            );
        }
    }
}

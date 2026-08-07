package com.aiagent.financial.agent.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 金融 Agent 图的状态。
 * 继承 LangGraph4j 的 AgentState。所有状态存储于底层 Map 中，
 * 通过类型化的 getter 读取。状态更新通过 NodeAction 返回的 Map 传递。
 */
public class AgentState extends org.bsc.langgraph4j.state.AgentState {

    public AgentState(Map<String, Object> data) {
        super(data);
    }

    // --- 输入 ---
    public String getQuery() {
        Object v = data().get("query");
        return v instanceof String s ? s : "";
    }

    public String getBusinessType() {
        Object v = data().get("businessType");
        return v instanceof String s ? s : "general";
    }

    // --- 规则检索 ---
    public String getRetrievedRules() {
        Object v = data().get("retrievedRules");
        return v instanceof String s ? s : "";
    }

    public int getRuleCount() {
        Object v = data().get("ruleCount");
        return v instanceof Number n ? n.intValue() : 0;
    }

    // --- 风险检查 ---
    public String getRiskResult() {
        Object v = data().get("riskResult");
        return v instanceof String s ? s : "";
    }

    public boolean isHasRisk() {
        Object v = data().get("hasRisk");
        return v instanceof Boolean b ? b : false;
    }

    public String getRiskLevel() {
        Object v = data().get("riskLevel");
        return v instanceof String s ? s : "LOW";
    }

    // --- 数据查询 ---
    public String getQueryResult() {
        Object v = data().get("queryResult");
        return v instanceof String s ? s : "";
    }

    // --- 清算 ---
    public String getSettlementResult() {
        Object v = data().get("settlementResult");
        return v instanceof String s ? s : "";
    }

    public boolean isSettlementSuccess() {
        Object v = data().get("settlementSuccess");
        return v instanceof Boolean b ? b : false;
    }

    // --- 循环控制 ---
    public int getRetryCount() {
        Object v = data().get("retryCount");
        return v instanceof Number n ? n.intValue() : 0;
    }

    public String getLastError() {
        Object v = data().get("lastError");
        return v instanceof String s ? s : "";
    }

    // --- 分析 ---
    public String getAnalysisResult() {
        Object v = data().get("analysisResult");
        return v instanceof String s ? s : "";
    }

    // --- 历史 ---
    @SuppressWarnings("unchecked")
    public List<String> getHistory() {
        Object v = data().get("history");
        if (v instanceof List<?> list) {
            return (List<String>) list;
        }
        return new ArrayList<>();
    }
}

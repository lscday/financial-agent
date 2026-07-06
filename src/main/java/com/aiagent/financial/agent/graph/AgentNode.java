package com.aiagent.financial.agent.graph;

/**
 * Agent 状态图中的节点枚举。
 * 每个枚举常量对应一个图节点，name() 作为节点标识符传入 LangGraph。
 */
public enum AgentNode {

    /** 规则检索：在向量库中搜索与查询相关的金融政策与规则 */
    RULE_RETRIEVAL("rule_retrieval"),
    /** 风险校验：基于检索结果评估交易风险等级 */
    RISK_CHECK("risk_check"),
    /** 数据查询：查询产品信息、交易记录等业务数据 */
    DATA_QUERY("data_query"),
    /** 资金清算：模拟执行清算操作，支持重试 */
    SETTLEMENT("settlement"),
    /** 分析报告：当风控或清算异常时，生成分析报告 */
    ANALYSIS("analysis");

    private final String nodeName;

    AgentNode(String nodeName) {
        this.nodeName = nodeName;
    }

    /** 返回 LangGraph 中注册的节点名称 */
    public String nodeName() {
        return nodeName;
    }
}

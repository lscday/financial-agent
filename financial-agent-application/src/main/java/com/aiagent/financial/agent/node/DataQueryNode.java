package com.aiagent.financial.agent.node;

import com.aiagent.financial.agent.state.AgentState;
import com.aiagent.financial.tool.ProductQueryTool;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据查询节点。
 * 根据用户请求和风险评估查询业务数据（产品信息、交易记录）。
 */
public class DataQueryNode implements NodeAction<AgentState> {
    private static final Logger log = LoggerFactory.getLogger(DataQueryNode.class);

    private final ProductQueryTool productQueryTool;

    public DataQueryNode(ProductQueryTool productQueryTool) {
        this.productQueryTool = productQueryTool;
    }

    @Override
    public Map<String, Object> apply(AgentState state) {
        log.info("=== DataQueryNode 查询业务数据");

        try {
            String result = productQueryTool.query(state.getQuery(), state.getBusinessType());
            List<String> history = new ArrayList<>(state.getHistory());
            history.add("DataQuery");

            return Map.of(
                    "queryResult", result,
                    "history", history
            );
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "未知异常";
            log.error("DataQueryNode 异常", e);
            List<String> history = new ArrayList<>(state.getHistory());
            history.add("DataQuery");
            return Map.of(
                    "queryResult", "数据查询异常: " + msg,
                    "lastError", msg,
                    "history", history
            );
        }
    }
}

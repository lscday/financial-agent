package com.aiagent.financial.agent.node;

import com.aiagent.financial.agent.state.AgentState;
import dev.langchain4j.model.chat.ChatModel;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分析节点。
 * 当检测到风险或清算失败时，使用 LLM 进行情况分析。
 * 生成人类可读的分析报告。
 */
public class AnalysisNode implements NodeAction<AgentState> {
    private static final Logger log = LoggerFactory.getLogger(AnalysisNode.class);

    private final ChatModel chatModel;

    public AnalysisNode(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Map<String, Object> apply(AgentState state) {
        log.info("=== AnalysisNode 正在生成分析报告");

        try {
            String prompt = buildAnalysisPrompt(state);
            String analysis = chatModel.chat(prompt);
            List<String> history = new ArrayList<>(state.getHistory());
            history.add("Analysis");

            return Map.of(
                    "analysisResult", analysis,
                    "history", history
            );
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "未知异常";
            log.error("AnalysisNode 异常", e);
            List<String> history = new ArrayList<>(state.getHistory());
            history.add("Analysis");
            return Map.of(
                    "analysisResult", "分析报告生成异常: " + msg,
                    "lastError", msg,
                    "history", history
            );
        }
    }

    private String buildAnalysisPrompt(AgentState state) {
        return """
                请基于以下信息生成一份金融清算分析报告：

                原始查询：%s
                业务类型：%s
                检索到的规则：%s
                风险评估结果：%s
                数据查询结果：%s
                清算结果：%s
                清算状态：%s

                请包含：
                1. 交易概况
                2. 风险评估结论
                3. 清算处理结果
                4. 建议措施
                """.formatted(
                state.getQuery(),
                state.getBusinessType(),
                truncate(state.getRetrievedRules(), 500),
                state.getRiskResult(),
                state.getQueryResult(),
                state.getSettlementResult(),
                state.isSettlementSuccess() ? "成功" : "失败/待处理"
        );
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : (s != null ? s : "无");
    }
}

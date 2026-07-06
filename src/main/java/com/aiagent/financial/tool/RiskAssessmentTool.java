package com.aiagent.financial.tool;

import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Risk Assessment Tool - 风险评估工具函数。
 * 结合检索到的规则和业务数据，利用LLM进行智能风险判断。
 */
@Component
public class RiskAssessmentTool {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentTool.class);

    private final ChatModel chatModel;

    public RiskAssessmentTool(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String assess(String query, String rules) {
        String prompt = """
                你是一位金融风控专家。请基于以下规则信息，对用户请求进行风险评估。

                用户请求：%s

                相关规则：%s

                请评估：
                1. 该交易是否存在合规风险？
                2. 风险等级（高风险/中等风险/低风险）
                3. 具体风险点
                4. 处理建议

                请用中文回答，简洁专业。
                """.formatted(
                query != null ? query : "无",
                rules != null ? rules.substring(0, Math.min(1000, rules.length())) : "无"
        );

        try {
            String result = chatModel.chat(prompt);
            log.info("风险评估完成，结果长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.warn("LLM 风险评估失败，使用基于规则的降级方案: {}", e.getMessage());
            return ruleBasedFallback(query, rules);
        }
    }

    private String ruleBasedFallback(String query, String rules) {
        StringBuilder result = new StringBuilder();
        result.append("【规则引擎风险评估结果】\n");

        boolean hasRisk = false;
        if (query != null) {
            if (query.contains("高风险") || query.contains("逾期") || query.contains("违约")) {
                result.append("风险等级：高风险\n");
                result.append("风险点：检测到高风险关键词\n");
                hasRisk = true;
            } else if (query.contains("大额") || query.contains("转出") || query.contains("提前赎回")) {
                result.append("风险等级：中等风险\n");
                result.append("风险点：涉及资金变动操作\n");
                hasRisk = true;
            } else {
                result.append("风险等级：低风险\n");
                result.append("风险点：未检测到明显风险\n");
            }
        }

        result.append("处理建议：").append(hasRisk ? "建议人工复核后处理" : "可按正常流程处理").append("\n");
        return result.toString();
    }
}

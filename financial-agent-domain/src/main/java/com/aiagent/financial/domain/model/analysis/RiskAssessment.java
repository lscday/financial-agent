package com.aiagent.financial.domain.model.analysis;

import com.aiagent.financial.domain.constant.RiskLevelEnum;

/**
 * 风险评估结果值对象。
 *
 * <p>不可变对象，封装一次风险评估的结论。构造时从原始文本解析风险等级，
 * 消除散落在节点中的字符串判断。</p>
 *
 * @param riskLevel   风险等级
 * @param hasRisk     是否存在风险
 * @param rawText     风险评估原始文本
 */
public record RiskAssessment(RiskLevelEnum riskLevel, boolean hasRisk, String rawText) {

    /**
     * 从风险评估原始文本解析构造结果。
     *
     * <p>解析规则与风险评估服务的输出格式约定一致：文本包含
     * "高风险" 视为 HIGH、包含 "中等风险" 视为 MEDIUM，否则 LOW。</p>
     *
     * @param rawText 风险评估原始文本
     * @return 解析后的风险评估值对象
     */
    public static RiskAssessment parse(String rawText) {
        if (rawText == null) {
            return new RiskAssessment(RiskLevelEnum.UNKNOWN, true, "");
        }
        boolean high = rawText.contains("高风险");
        boolean medium = rawText.contains("中等风险");
        RiskLevelEnum level = high ? RiskLevelEnum.HIGH : (medium ? RiskLevelEnum.MEDIUM : RiskLevelEnum.LOW);
        return new RiskAssessment(level, level != RiskLevelEnum.LOW, rawText);
    }

    /**
     * 判断是否为高风险。
     *
     * @return true 表示高风险
     */
    public boolean isHighRisk() {
        return riskLevel == RiskLevelEnum.HIGH;
    }
}

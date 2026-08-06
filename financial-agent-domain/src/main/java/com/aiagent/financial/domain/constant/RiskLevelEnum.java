package com.aiagent.financial.domain.constant;

/**
 * 风险等级枚举。
 *
 * <p>定义金融咨询工作流中评估出的风险级别，替代散落在节点代码中的
 * {@code "HIGH"/"MEDIUM"/"LOW"} 字符串判断。</p>
 */
public enum RiskLevelEnum {

    /** 高风险：需人工复核或直接进入分析 */
    HIGH("HIGH", "高风险"),
    /** 中等风险：存在资金变动操作 */
    MEDIUM("MEDIUM", "中等风险"),
    /** 低风险：未检测到明显风险 */
    LOW("LOW", "低风险"),
    /** 未知：风险评估异常时的兜底 */
    UNKNOWN("UNKNOWN", "未知");

    private final String code;

    private final String description;

    RiskLevelEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码解析风险等级。
     *
     * @param code 风险等级编码
     * @return 对应的风险等级枚举
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static RiskLevelEnum fromCode(String code) {
        for (RiskLevelEnum level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("未知风险等级: " + code);
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

package com.aiagent.financial.domain.constant;

/**
 * 清算状态枚举。
 *
 * <p>定义资金清算的执行状态，替代散落在节点代码中的
 * {@code contains("成功")} / {@code contains("completed")} 字符串判断。</p>
 */
public enum SettlementStatusEnum {

    /** 待处理：清算尚未执行 */
    PENDING("PENDING", "待处理"),
    /** 已成功：资金已划付 */
    COMPLETED("COMPLETED", "已成功"),
    /** 已失败：清算执行失败 */
    FAILED("FAILED", "已失败");

    private final String code;

    private final String description;

    SettlementStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码解析清算状态。
     *
     * @param code 清算状态编码
     * @return 对应的清算状态枚举
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static SettlementStatusEnum fromCode(String code) {
        for (SettlementStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知清算状态: " + code);
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

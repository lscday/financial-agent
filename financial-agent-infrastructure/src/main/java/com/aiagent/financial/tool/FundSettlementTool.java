package com.aiagent.financial.tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 资金清算工具函数。
 * 模拟对接外部资金清算接口，支持大模型自主调取完成智能测算。
 */

@Component
public class FundSettlementTool {

    private static final Logger log = LoggerFactory.getLogger(FundSettlementTool.class);

    private final AtomicLong settlementIdCounter = new AtomicLong(0);
    private final Map<String, SettlementRecord> records = new ConcurrentHashMap<>();

    /**
     * 执行资金清算模拟。
     *
     * @param query 原始查询
     * @param businessData 产品查询返回的数据
     * @param riskInfo 风险评估结果
     * @return 格式化后的清算结果
     */
    public String settle(String query, String businessData, String riskInfo) {
        String settlementId = "STL" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now())
                + String.format("%05d", settlementIdCounter.incrementAndGet());

        log.info("处理清算：{} | 业务数据长度：{}", settlementId,
                businessData != null ? businessData.length() : 0);

        // 模拟清算逻辑
        boolean hasError = query != null && (
                query.contains("error") ||
                query.contains("异常") ||
                query.contains("失败") ||
                query.contains("重试测试")
        );

        if (hasError && !query.contains("force_ok")) {
            String errorMsg = "清算执行失败：资金账户余额不足（错误代码: ERR_INSUFFICIENT_FUNDS）";
            records.put(settlementId, new SettlementRecord(settlementId, "FAILED", errorMsg, LocalDateTime.now()));
            log.warn("清算 {} 失败：{}", settlementId, errorMsg);
            return errorMsg;
        }

        String result = """
                资金清算执行成功【清算编号：%s】
                清算时间：%s
                交易金额：¥1,500,000.00
                收款账户：中国银行 6222 **** 1234（基金托管专户）
                付款账户：工商银行 9558 **** 5678（资管计划专户）
                清算状态：completed
                处理结果：资金已划付，待确认
                """.formatted(settlementId, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        records.put(settlementId, new SettlementRecord(settlementId, "COMPLETED", result, LocalDateTime.now()));
        log.info("清算 {} 成功完成", settlementId);
        return result;
    }

    /**
     * 根据 ID 查询清算记录。
     */
    public String querySettlement(String settlementId) {
        SettlementRecord record = records.get(settlementId);
        if (record == null) {
            return "未找到清算记录: " + settlementId;
        }
        return "清算记录: ID=%s, 状态=%s, 时间=%s".formatted(
                record.id(), record.status(), record.timestamp());
    }

    public record SettlementRecord(String id, String status, String detail, LocalDateTime timestamp) {}
}

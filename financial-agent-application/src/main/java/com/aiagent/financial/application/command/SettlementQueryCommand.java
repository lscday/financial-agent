package com.aiagent.financial.application.command;

/**
 * 清算状态查询命令。
 */
public class SettlementQueryCommand {

    /**
     * 查询请求。
     *
     * @param settlementId 清算编号
     */
    public record Request(
            String settlementId) {
    }

    /**
     * 查询响应。
     *
     * @param settlementId 清算编号
     * @param status       清算状态（待处理/已成功/已失败）
     * @param createdAt    创建时间
     */
    public record Response(
            String settlementId,
            String status,
            String createdAt) {
    }
}

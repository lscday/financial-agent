package com.aiagent.financial.application.command;

import com.aiagent.financial.agent.FinancialAgentOrchestrator;

import java.util.List;

/**
 * 执行金融 Agent 工作流命令。
 */
public class ExecuteAgentCommand {

    /**
     * 执行 Agent 请求。
     *
     * @param query        用户查询文本
     * @param businessType 业务类型（wealth、pension、general 等）
     */
    public record Request(
            String query,
            String businessType) {
    }

    /**
     * 执行 Agent 响应。
     *
     * @param success    是否成功
     * @param summary    摘要
     * @param history    节点执行历史
     * @param elapsedMs  耗时（毫秒）
     * @param error      错误信息（成功时为 null）
     */
    public record Response(
            boolean success,
            String summary,
            List<String> history,
            long elapsedMs,
            String error) {

        /**
         * 从编排器结果构造响应。
         *
         * @param result 编排器执行结果
         * @return 命令响应
         */
        public static Response from(FinancialAgentOrchestrator.AgentExecutionResult result) {
            return new Response(
                    result.success(),
                    result.summary(),
                    result.history(),
                    result.elapsedMs(),
                    result.error());
        }
    }
}

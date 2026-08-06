package com.aiagent.financial.controller;

import com.aiagent.financial.agent.FinancialAgentOrchestrator;
import com.aiagent.financial.application.command.ExecuteAgentCommand;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多 Agent 编排控制器。
 * 提供基于 LangGraph 的金融 Agent 工作流端点。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final FinancialAgentOrchestrator orchestrator;

    /**
     * 构造控制器。
     *
     * @param orchestrator Agent 编排器
     */
    public AgentController(FinancialAgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * 执行金融 Agent 工作流。
     *
     * @param request 执行请求
     * @return 执行响应
     */
    @PostMapping("/execute")
    public ExecuteAgentCommand.Response execute(@RequestBody ExecuteAgentCommand.Request request) {
        var result = orchestrator.execute(request.query(), request.businessType());
        return ExecuteAgentCommand.Response.from(result);
    }

    /**
     * 获取平台存活状态。
     *
     * @return 存活提示
     */
    @GetMapping("/status")
    public String status() {
        return "Agent 平台运行中";
    }
}

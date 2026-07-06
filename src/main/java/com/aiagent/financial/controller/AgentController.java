package com.aiagent.financial.controller;

import com.aiagent.financial.agent.FinancialAgentOrchestrator;
import org.springframework.web.bind.annotation.*;

/**
 * 多 Agent 编排控制器。
 * 提供基于 LangGraph 的金融 Agent 工作流端点。
 */
@RestController
@RequestMapping("/api/agent")

public class AgentController {

    private final FinancialAgentOrchestrator orchestrator;

    public AgentController(FinancialAgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * 执行金融 Agent 工作流。
     */
    @PostMapping("/execute")
    public FinancialAgentOrchestrator.AgentExecutionResult execute(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "general") String businessType) {
        return orchestrator.execute(query, businessType);
    }

    /**
     * 获取限流器状态。
     */
    @GetMapping("/status")
    public String status() {
        return "Agent 平台运行中";
    }
}

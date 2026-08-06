package com.aiagent.financial.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aiagent.financial.agent.FinancialAgentOrchestrator;
import com.aiagent.financial.rag.RAGService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 清算自动处理演示。
 * 模拟金融清算异常自动处理场景：
 * 1. 用户提交清算请求
 * 2. Agent 检索相关规则
 * 3. Agent 检查风险
 * 4. Agent 查询业务数据
 * 5. Agent 执行清算（含重试）
 * 6. 生成分析报告
 */

@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class SettlementDemoRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SettlementDemoRunner.class);

    private final FinancialAgentOrchestrator orchestrator;
    private final RAGService ragService;

    public SettlementDemoRunner(FinancialAgentOrchestrator orchestrator, RAGService ragService) {
        this.orchestrator = orchestrator;
        this.ragService = ragService;
    }

    @Override
    public void run(String... args) {
        log.info("==============================================");
        log.info("   金融清算异常自动处理场景演示");
        log.info("==============================================");

        // 场景1：正常清算
        demoNormalSettlement();

        // 场景2：高风险交易
        demoRiskSettlement();

        // 场景3：RAG 查询
        demoRAGQuery();

        log.info("==============================================");
        log.info("   演示完成");
        log.info("==============================================");
    }

    private void demoNormalSettlement() {
        log.info("\n--- 场景1：正常资金清算 ---");
        try {
            var result = orchestrator.execute("理财产品A到期清算 force_ok", "wealth");
            log.info("清算结果: success={}, history={}", result.success(), result.history());
            log.info("分析报告: {}", result.summary());
        } catch (Exception e) {
            log.error("场景1执行失败", e);
        }
    }

    private void demoRiskSettlement() {
        log.info("\n--- 场景2：高风险交易处理 ---");
        try {
            var result = orchestrator.execute("大额资金转出500万", "wealth");
            log.info("风控结果: success={}, riskLevel={}, history={}",
                    result.success(),
                    result.finalState() != null ? result.finalState().getRiskLevel() : "N/A",
                    result.history());
        } catch (Exception e) {
            log.error("场景2执行失败", e);
        }
    }

    private void demoRAGQuery() {
        log.info("\n--- 场景3：RAG增强检索问答 ---");
        try {
            var result = ragService.query("个人养老金每年缴费上限是多少？有哪些税收优惠？");
            log.info("问题: {}", result.question());
            log.info("回答: {}", result.answer());
            log.info("参考来源: {}", result.sources());
            log.info("检索耗时: {}ms", result.elapsedMs());
        } catch (Exception e) {
            log.error("场景3执行失败", e);
        }
    }
}

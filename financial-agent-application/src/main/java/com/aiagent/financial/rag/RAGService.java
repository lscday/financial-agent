package com.aiagent.financial.rag;

import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 完整的 RAG 流水线：检索 -> 增强 -> 生成。
 * 将语义检索与 LLM 生成相结合，产生基于上下文的回答，减少幻觉。
 */
@Service
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);

    private final RetrievalService retrievalService;
    private final ChatModel chatModel;

    public RAGService(RetrievalService retrievalService, ChatModel chatModel) {
        this.retrievalService = retrievalService;
        this.chatModel = chatModel;
    }

    private static final String SYSTEM_PROMPT = """
            你是一位专业的金融资管领域AI助手。请基于以下参考文档内容回答用户问题。

            要求：
            1. 如果参考文档中有相关信息，请优先引用并基于它们回答
            2. 如果参考文档中没有足够信息，请明确说明
            3. 回答应专业、准确、简洁
            4. 在回答末尾列出引用的文档来源

            参考文档：
            %s
            """;

    public RAGResponse query(String question) {
        long startTime = System.currentTimeMillis();

        var retrievalResult = retrievalService.retrieve(question);
        log.debug("查询 \"{}\" 检索到 {} 个片段", question, retrievalResult.matches().size());

        String prompt = String.format(SYSTEM_PROMPT, retrievalResult.contextText())
                + "\n用户问题：" + question;

        String answer = chatModel.chat(prompt);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("RAG 查询完成，耗时 {}ms，来源: {}", elapsed, retrievalResult.sources());

        return new RAGResponse(answer, question, retrievalResult.sources(),
                retrievalResult.matches().size(), elapsed);
    }

    public record RAGResponse(
            String answer,
            String question,
            java.util.List<String> sources,
            int segmentCount,
            long elapsedMs
    ) {}
}

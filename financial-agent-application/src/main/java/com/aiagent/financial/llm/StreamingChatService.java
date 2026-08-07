package com.aiagent.financial.llm;

import com.aiagent.financial.rag.RetrievalService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * SSE 流式智能聊天服务。
 *
 * <p>将 LangChain4j StreamingChatModel 包装为 Reactor Flux。升级为智能 RAG 路由：
 * 用户提问先检索知识库，按方法 C 判断——高分直接 RAG、低分纯 LLM、
 * 中间档由 LLM 确认。所有路径最终均流式输出。</p>
 */
@Service
public class StreamingChatService {

    private static final Logger log = LoggerFactory.getLogger(StreamingChatService.class);

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

    private static final String RAG_CONFIRM_PROMPT = """
            判断以下参考文档片段是否足以回答用户问题。
            只需回复"够"或"不够"。

            用户问题：%s

            参考文档片段：
            %s
            """;

    private final StreamingChatModel streamingModel;
    private final ChatModel chatModel;
    private final RetrievalService retrievalService;
    private final RetrievalRouter router;

    public StreamingChatService(StreamingChatModel streamingModel,
                                ChatModel chatModel,
                                RetrievalService retrievalService,
                                RetrievalRouter router) {
        this.streamingModel = streamingModel;
        this.chatModel = chatModel;
        this.retrievalService = retrievalService;
        this.router = router;
    }

    /**
     * 智能流式聊天：检索 → 判断 → 流式回答。
     *
     * @param question       用户问题
     * @param conversationId 会话标识
     * @return 流式回答文本
     */
    public Flux<String> stream(String question, String conversationId) {
        return Flux.create(sink -> {
            try {
                // 1. 检索知识库
                RetrievalService.RetrievalResult retrievalResult = retrievalService.retrieve(question);
                double maxScore = retrievalResult.matches().isEmpty()
                        ? 0.0
                        : retrievalResult.matches().getFirst().score();

                // 2. 方法 C 决策
                RetrievalRouter.Route route = router.decide(maxScore);
                log.info("智能路由: conversationId={}, question={}, maxScore={}, route={}",
                        conversationId, question, String.format("%.2f", maxScore), route);

                switch (route) {
                    case RAG -> streamRagAnswer(question, retrievalResult.contextText(), sink);
                    case LLM -> streamPlainAnswer(question, sink);
                    case ASK_LLM -> streamWithConfirm(question, retrievalResult, sink);
                }
            } catch (Exception e) {
                log.error("智能聊天异常", e);
                sink.error(e);
            }
        });
    }

    private void streamRagAnswer(String question, String contextText, FluxSink<String> sink) {
        String prompt = String.format(SYSTEM_PROMPT, contextText) + "\n用户问题：" + question;
        streamWithPrompt(prompt, sink);
    }

    private void streamPlainAnswer(String question, FluxSink<String> sink) {
        streamWithPrompt(question, sink);
    }

    private void streamWithConfirm(String question, RetrievalService.RetrievalResult result, FluxSink<String> sink) {
        // 中间档：先非流式让 LLM 确认片段是否足以回答
        String confirmPrompt = String.format(RAG_CONFIRM_PROMPT, question, result.contextText());
        String confirm = chatModel.chat(confirmPrompt);
        boolean enough = confirm != null && (confirm.contains("够") || confirm.contains("可以"));

        log.info("中间档 LLM 确认: 片段是否足以回答={}", enough);
        if (enough) {
            streamRagAnswer(question, result.contextText(), sink);
        } else {
            streamPlainAnswer(question, sink);
        }
    }

    private void streamWithPrompt(String prompt, FluxSink<String> sink) {
        streamingModel.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                sink.next(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                sink.complete();
            }

            @Override
            public void onError(Throwable error) {
                sink.error(error);
            }
        });
    }

    /**
     * 非流式智能回答。
     *
     * @param question 用户问题
     * @return 回答文本
     */
    public String chat(String question) {
        return chatModel.chat(question);
    }
}

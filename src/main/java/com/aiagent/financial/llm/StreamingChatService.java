package com.aiagent.financial.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * SSE 流式聊天服务。
 * 将 LangChain4j StreamingChatModel 包装为 Reactor Flux，用于 SSE 端点。
 */
@Service
public class StreamingChatService {

    private final StreamingChatModel streamingModel;
    private final ChatModel chatModel;

    public StreamingChatService(StreamingChatModel streamingModel, ChatModel chatModel) {
        this.streamingModel = streamingModel;
        this.chatModel = chatModel;
    }

    public Flux<String> stream(String question, String conversationId) {
        return Flux.create(sink -> {
            streamingModel.chat(question, new StreamingChatResponseHandler() {
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
        });
    }

    public String chat(String question) {
        return chatModel.chat(question);
    }
}

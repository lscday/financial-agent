package com.aiagent.financial.application.command;

import com.aiagent.financial.rag.RAGService;

import java.util.List;

/**
 * RAG 增强查询命令。
 */
public class RagQueryCommand {

    /**
     * RAG 查询请求。
     *
     * @param question 用户问题
     */
    public record Request(
            String question) {
    }

    /**
     * RAG 查询响应。
     *
     * @param answer        回答内容
     * @param question      原始问题
     * @param sources       引用文档来源
     * @param segmentCount  命中的片段数
     * @param elapsedMs     耗时（毫秒）
     */
    public record Response(
            String answer,
            String question,
            List<String> sources,
            int segmentCount,
            long elapsedMs) {

        /**
         * 从 RAG 服务结果构造响应。
         *
         * @param result RAG 服务结果
         * @return 命令响应
         */
        public static Response from(RAGService.RAGResponse result) {
            return new Response(
                    result.answer(),
                    result.question(),
                    result.sources(),
                    result.segmentCount(),
                    result.elapsedMs());
        }
    }
}

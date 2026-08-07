package com.aiagent.financial.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 检索路由决策器。
 *
 * <p>根据检索结果的最高相似度分数，决定回答方式（方法 C：三档判断）。
 * 高阈值命中 → 直接 RAG；低阈值未命中 → 纯 LLM；中间档 → 需 LLM 确认。</p>
 */
@Component
public class RetrievalRouter {

    private final double highThreshold;

    private final double lowThreshold;

    /**
     * 构造检索路由决策器。
     *
     * @param highThreshold 高阈值，最高分数 ≥ 该值判定为确定命中
     * @param lowThreshold  低阈值，最高分数 < 该值判定为未命中
     */
    public RetrievalRouter(@Value("${rag.route.high-threshold:0.75}") double highThreshold,
                           @Value("${rag.route.low-threshold:0.45}") double lowThreshold) {
        this.highThreshold = highThreshold;
        this.lowThreshold = lowThreshold;
    }

    /**
     * 根据最高相似度分数决定回答方式。
     *
     * @param maxScore 检索结果的最高相似度分数（无结果时为 0）
     * @return 路由决策
     */
    public Route decide(double maxScore) {
        if (maxScore >= highThreshold) {
            return Route.RAG;
        }
        if (maxScore < lowThreshold) {
            return Route.LLM;
        }
        return Route.ASK_LLM;
    }

    /**
     * 回答方式决策。
     */
    public enum Route {
        /** 直接使用 RAG（命中确定） */
        RAG,
        /** 直接使用纯 LLM（未命中） */
        LLM,
        /** 中间档，需询问 LLM 确认片段是否足以回答 */
        ASK_LLM
    }
}

package com.aiagent.financial.application.command;

/**
 * 聊天命令。
 */
public class ChatCommand {

    /**
     * 流式聊天请求。
     *
     * @param question       用户问题
     * @param conversationId 会话标识
     */
    public record StreamRequest(
            String question,
            String conversationId) {
    }

    /**
     * 非流式聊天请求。
     *
     * @param question 用户问题
     */
    public record AskRequest(
            String question) {
    }
}

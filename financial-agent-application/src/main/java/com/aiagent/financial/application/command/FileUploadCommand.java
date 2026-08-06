package com.aiagent.financial.application.command;

/**
 * 文件上传命令。
 */
public class FileUploadCommand {

    /**
     * 上传请求。
     *
     * @param filename 原始文件名
     */
    public record Request(
            String filename) {
    }

    /**
     * 上传响应。
     *
     * @param objectName 对象名
     * @param url        可访问地址
     */
    public record Response(
            String objectName,
            String url) {
    }
}

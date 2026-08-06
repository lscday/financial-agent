package com.aiagent.financial.application.service;

import com.aiagent.financial.application.command.FileUploadCommand;
import com.aiagent.financial.domain.repository.AuditLogPort;
import com.aiagent.financial.domain.repository.ObjectStorage;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 文件存储应用服务。
 *
 * <p>编排文件上传用例：生成对象名、调用对象存储、记录审计日志。
 * 不含业务规则，仅做编排。</p>
 */
@Service
public class FileStorageAppService {

    private final ObjectStorage objectStorage;

    private final AuditLogPort auditLogPort;

    /**
     * 构造文件存储应用服务。
     *
     * @param objectStorage 对象存储端口
     * @param auditLogPort  审计日志端口
     */
    public FileStorageAppService(ObjectStorage objectStorage, AuditLogPort auditLogPort) {
        this.objectStorage = objectStorage;
        this.auditLogPort = auditLogPort;
    }

    /**
     * 上传文件。
     *
     * @param request   上传请求
     * @param content   文件内容
     * @return 上传结果
     */
    public FileUploadCommand.Response upload(FileUploadCommand.Request request, InputStream content) {
        String objectName = buildObjectName(request.filename());
        String url = objectStorage.put(objectName, content);
        auditLogPort.log("file", "upload", "文件上传: " + objectName);
        return new FileUploadCommand.Response(objectName, url);
    }

    /**
     * 读取文件内容。
     *
     * @param objectName 对象名
     * @return 文件内容输入流
     */
    public InputStream load(String objectName) {
        return objectStorage.get(objectName);
    }

    private String buildObjectName(String filename) {
        String safeName = filename == null || filename.isBlank() ? "unnamed" : filename.replace(" ", "_");
        return "upload/" + UUID.randomUUID() + "_" + safeName;
    }
}

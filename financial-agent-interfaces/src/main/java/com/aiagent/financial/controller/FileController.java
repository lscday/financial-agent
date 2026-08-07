package com.aiagent.financial.controller;

import com.aiagent.financial.application.command.FileUploadCommand;
import com.aiagent.financial.application.service.FileStorageAppService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件存储控制器。
 * 提供文件上传与下载端点。
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileStorageAppService fileStorageAppService;

    /**
     * 构造控制器。
     *
     * @param fileStorageAppService 文件存储应用服务
     */
    public FileController(FileStorageAppService fileStorageAppService) {
        this.fileStorageAppService = fileStorageAppService;
    }

    /**
     * 上传文件。
     *
     * @param file 上传的文件
     * @return 上传结果
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadCommand.Response upload(@RequestParam("file") MultipartFile file) {
        FileUploadCommand.Request request = new FileUploadCommand.Request(file.getOriginalFilename());
        try (InputStream in = file.getInputStream()) {
            return fileStorageAppService.upload(request, in);
        } catch (Exception e) {
            throw new IllegalStateException("文件读取失败", e);
        }
    }

    /**
     * 下载文件。
     * 对象名可能包含多段路径（如 upload/uuid_test.txt），使用通配符匹配。
     *
     * @param request 对象名（多段路径）
     * @return 文件资源
     */
    @GetMapping("/download/**")
    public ResponseEntity<Resource> download(HttpServletRequest request) {
        String objectName = extractObjectName(request);
        InputStream in = fileStorageAppService.load(objectName);
        if (in == null) {
            return ResponseEntity.notFound().build();
        }
        String encoded = URLEncoder.encode(objectName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(in));
    }

    private String extractObjectName(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int idx = uri.indexOf("/download/");
        if (idx == -1) {
            return "";
        }
        return uri.substring(idx + "/download/".length());
    }
}

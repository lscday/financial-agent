package com.aiagent.financial.infrastructure.objectstorage;

import com.aiagent.financial.domain.repository.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地对象存储实现。
 *
 * <p>将文件写入本地目录 {@code data/objects/}，用于无阿里云环境的本机调试。
 * 激活条件：非 aliyun profile。</p>
 */
@Component
@Profile("!aliyun")
public class LocalObjectStorageAdapter implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalObjectStorageAdapter.class);

    private static final String BASE_DIR = "data/objects";

    /**
     * 构造本地对象存储，确保根目录存在。
     */
    public LocalObjectStorageAdapter() {
        try {
            Files.createDirectories(Paths.get(BASE_DIR));
        } catch (IOException e) {
            log.warn("创建本地对象存储目录失败", e);
        }
    }

    @Override
    public String put(String objectName, InputStream content) {
        try {
            Path target = Paths.get(BASE_DIR, objectName);
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return "/local-objects/" + objectName;
        } catch (IOException e) {
            throw new RuntimeException("保存文件到本地失败: " + objectName, e);
        }
    }

    @Override
    public InputStream get(String objectName) {
        try {
            Path source = Paths.get(BASE_DIR, objectName);
            if (!Files.exists(source)) {
                return null;
            }
            return Files.newInputStream(source);
        } catch (IOException e) {
            throw new RuntimeException("读取本地文件失败: " + objectName, e);
        }
    }
}

package com.aiagent.financial.domain.repository;

import java.io.InputStream;

/**
 * 对象存储端口。
 *
 * <p>封装文件的保存与读取能力，领域层仅依赖此接口，
 * 由基础设施层提供本地或阿里云 OSS 实现。</p>
 */
public interface ObjectStorage {

    /**
     * 保存文件。
     *
     * @param objectName 对象名（含路径）
     * @param content    文件内容输入流
     * @return 可访问的文件地址
     */
    String put(String objectName, InputStream content);

    /**
     * 读取文件内容。
     *
     * @param objectName 对象名
     * @return 文件内容输入流，不存在时返回 null
     */
    InputStream get(String objectName);
}

package com.aiagent.financial.demo;

import com.aiagent.financial.domain.model.rag.VectorEntry;
import com.aiagent.financial.domain.repository.EmbeddedVectorRepository;
import com.aiagent.financial.rag.DocumentSplitter;
import com.aiagent.financial.rag.EmbeddingService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;

/**
 * 启动时将金融文档导入向量存储。
 * 按文件粒度比对哈希，只导入新增或变更的文件。
 */
@Component
@ConditionalOnProperty(name = "app.data.init", havingValue = "true", matchIfMissing = true)
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final DocumentSplitter documentSplitter;
    private final EmbeddingService embeddingService;
    private final EmbeddedVectorRepository vectorRepository;

    public DataInitializer(DocumentSplitter documentSplitter,
                           EmbeddingService embeddingService,
                           EmbeddedVectorRepository vectorRepository) {
        this.documentSplitter = documentSplitter;
        this.embeddingService = embeddingService;
        this.vectorRepository = vectorRepository;
    }

    @PostConstruct
    public void initialize() {
        try {
            // 扫描文档文件，计算每个文件的哈希
            Map<String, String> fileHashes = computeFileHashes();
            if (fileHashes.isEmpty()) {
                log.warn("未找到金融文档可导入");
                return;
            }

            // 读取向量存储中上次记录的各文件哈希
            Map<String, String> storedHashes = vectorRepository.getAllDocHashes();

            // 找出新增或变更的文件
            List<String> changedFiles = new ArrayList<>();
            for (Map.Entry<String, String> entry : fileHashes.entrySet()) {
                String filename = entry.getKey();
                String currentHash = entry.getValue();
                String storedHash = storedHashes.get(filename);
                if (!currentHash.equals(storedHash)) {
                    changedFiles.add(filename);
                }
            }

            if (changedFiles.isEmpty()) {
                log.info("所有文档均未变更，跳过导入");
                return;
            }

            if (storedHashes.isEmpty()) {
                log.info("首次导入金融文档（{} 个文件）...", fileHashes.size());
            } else {
                log.info("检测到文件变更（{} 个），重新导入中...", changedFiles.size());
                changedFiles.forEach(f -> log.info("  变更文件: {}", f));
            }

            log.info("=== 开始导入金融文档 ===");

            // 加载所有文档（用于后续分块）
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            int totalSegments = 0;

            for (String filename : changedFiles) {
                Resource resource = resolver.getResource("classpath:documents/finance/" + filename);
                if (!resource.exists() || !resource.isReadable()) continue;

                String text = new String(resource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                Metadata metadata = new Metadata();
                metadata.put("source", filename);
                metadata.put("category", detectCategory(filename));
                Document doc = Document.from(text, metadata);

                // 分块
                List<TextSegment> segments = documentSplitter.split(doc);
                log.info("  文件 {} 分割为 {} 个片段", filename, segments.size());

                // 向量化
                List<Embedding> embeddings = embeddingService.embedAll(segments);
                log.info("  文件 {} 生成 {} 个向量嵌入", filename, embeddings.size());

                // 转领域类型后存储
                List<VectorEntry> entries = new ArrayList<>();
                for (int i = 0; i < segments.size(); i++) {
                    TextSegment segment = segments.get(i);
                    String source = segment.metadata().getString("source");
                    String category = segment.metadata().getString("category");
                    entries.add(new VectorEntry(
                            null, segment.text(), source, category, embeddings.get(i).vector()));
                }
                vectorRepository.storeAll(entries);
                totalSegments += segments.size();
            }

            // 保存所有文件的最新哈希
            vectorRepository.saveDocHashes(fileHashes);

            log.info("=== 文档导入完成：{} 个文件，{} 个片段 ===", changedFiles.size(), totalSegments);

        } catch (Exception e) {
            log.error("初始化文档数据失败", e);
        }
    }

    /** 扫描文档目录，返回 文件名 → SHA256 哈希 的映射 */
    private Map<String, String> computeFileHashes() {
        try {
            Map<String, String> result = new LinkedHashMap<>();
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:documents/finance/**/*.{md,txt}");

            for (Resource resource : resources) {
                if (!resource.exists() || !resource.isReadable()) continue;
                String name = resource.getFilename();
                if (name == null || name.startsWith(".")) continue;

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream is = resource.getInputStream()) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        digest.update(buf, 0, len);
                    }
                }
                byte[] hashBytes = digest.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) sb.append(String.format("%02x", b));
                result.put(name, sb.toString());
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("计算文档哈希失败", e);
        }
    }

    private String detectCategory(String filename) {
        String name = filename.toLowerCase();
        if (name.contains("wealth") || name.contains("理财")) return "wealth_management";
        else if (name.contains("pension") || name.contains("养老")) return "pension";
        else if (name.contains("regulat") || name.contains("监管") || name.contains("policy")) return "regulatory";
        return "general";
    }
}

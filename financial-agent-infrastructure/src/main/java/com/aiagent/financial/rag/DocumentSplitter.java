package com.aiagent.financial.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 金融文档的文本分割器。
 * 使用递归字符分割，支持可配置的块大小和重叠量。
 */
@Component
public class DocumentSplitter {

    private final int chunkSize;
    private final int chunkOverlap;
    private dev.langchain4j.data.document.DocumentSplitter splitter;

    public DocumentSplitter(
            @Value("${rag.chunk-size:500}") int chunkSize,
            @Value("${rag.chunk-overlap:50}") int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    @PostConstruct
    public void init() {
        this.splitter = DocumentSplitters.recursive(chunkSize, chunkOverlap);
    }

    /**
     * 将文档分割为文本片段。
     */
    public List<TextSegment> split(Document document) {
        return splitter.split(document);
    }

    /**
     * 将多个文档分割为文本片段。
     */
    public List<TextSegment> splitAll(List<Document> documents) {
        return documents.stream()
                .flatMap(doc -> split(doc).stream())
                .toList();
    }
}

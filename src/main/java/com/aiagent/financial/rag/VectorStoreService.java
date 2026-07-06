package com.aiagent.financial.rag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import jakarta.annotation.PostConstruct;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 ES 底层 REST 客户端的自定义向量存储。
 * 提供对索引和搜索操作的完全控制。
 */
@Service
public class VectorStoreService {
    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final RestClient restClient;
    private final com.aiagent.financial.config.ElasticsearchConfig esConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VectorStoreService(RestClient restClient,
                               com.aiagent.financial.config.ElasticsearchConfig esConfig) {
        this.restClient = restClient;
        this.esConfig = esConfig;
    }

    @PostConstruct
    public void initialize() {
        ensureIndexExists(esConfig.getIndex().getChunks());
    }

    private void ensureIndexExists(String indexName) {
        try {
            Request existsReq = new Request("HEAD", "/" + indexName);
            boolean exists = restClient.performRequest(existsReq).getStatusLine().getStatusCode() == 200;

            if (!exists) {
                log.info("创建索引：{}，使用 dense_vector 映射", indexName);

                String mapping = """
                        {
                          "mappings": {
                            "properties": {
                              "embedding": {
                                "type": "dense_vector",
                                "dims": 1024,
                                "index": true,
                                "similarity": "cosine"
                              },
                              "text": {
                                "type": "text",
                                "analyzer": "icu_analyzer"
                              },
                              "source": {
                                "type": "keyword"
                              },
                              "category": {
                                "type": "keyword"
                              }
                            }
                          }
                        }
                        """;

                Request createReq = new Request("PUT", "/" + indexName);
                createReq.setEntity(new StringEntity(mapping, ContentType.APPLICATION_JSON));
                var response = restClient.performRequest(createReq);
                log.info("索引 {} 已创建：{}", indexName, response.getStatusLine());
            } else {
                log.info("索引 {} 已存在", indexName);
            }
        } catch (Exception e) {
            log.error("确保索引存在时失败：{}", indexName, e);
        }
    }

    public String store(TextSegment segment, Embedding embedding) {
        try {
            var indexName = esConfig.getIndex().getChunks();
            ObjectNode doc = objectMapper.createObjectNode();

            var embArray = doc.putArray("embedding");
            embedding.vectorAsList().forEach(embArray::add);
            doc.put("text", segment.text());
            doc.put("source", segment.metadata().getString("source"));
            doc.put("category", segment.metadata().getString("category"));

            Request request = new Request("POST", "/" + indexName + "/_doc");
            request.setEntity(new StringEntity(doc.toString(), ContentType.APPLICATION_JSON));
            var response = restClient.performRequest(request);

            JsonNode result = objectMapper.readTree(response.getEntity().getContent());
            return result.get("_id").asText();
        } catch (Exception e) {
            log.error("存储向量嵌入失败", e);
            throw new RuntimeException("存储向量嵌入失败", e);
        }
    }

    public void storeAll(List<TextSegment> segments, List<Embedding> embeddings) {
        if (segments.size() != embeddings.size()) {
            throw new IllegalArgumentException("片段和嵌入向量的数量必须一致");
        }

        List<String> ids = new ArrayList<>();
        try {
            var indexName = esConfig.getIndex().getChunks();
            StringBuilder bulkBody = new StringBuilder();

            for (int i = 0; i < segments.size(); i++) {
                var segment = segments.get(i);
                var embedding = embeddings.get(i);

                // 批量索引头部
                bulkBody.append("{\"index\":{\"_index\":\"").append(indexName).append("\"}}\n");

                // 文档内容
                ObjectNode doc = objectMapper.createObjectNode();
                var embArray = doc.putArray("embedding");
                embedding.vectorAsList().forEach(embArray::add);
                doc.put("text", segment.text());
                doc.put("source", segment.metadata().getString("source"));
                doc.put("category", segment.metadata().getString("category"));
                bulkBody.append(doc).append("\n");
            }

            Request bulkRequest = new Request("POST", "/_bulk");
            bulkRequest.setEntity(new StringEntity(bulkBody.toString(), ContentType.APPLICATION_JSON));
            var response = restClient.performRequest(bulkRequest);

            // 检查批量写入结果中的错误
            JsonNode bulkResult = objectMapper.readTree(response.getEntity().getContent());
            if (bulkResult.has("errors") && bulkResult.get("errors").asBoolean()) {
                var items = bulkResult.get("items");
                if (items != null && items.isArray()) {
                    for (var item : items) {
                        var idx = item.get("index");
                        if (idx != null && idx.has("error")) {
                            log.warn("批量写入失败，文档 {} 错误: {}",
                                    idx.has("_id") ? idx.get("_id").asText() : "未知",
                                    idx.get("error"));
                        }
                    }
                }
            } else {
                log.info("批量存储了 {} 个向量嵌入到 {}", segments.size(), indexName);
            }

        } catch (Exception e) {
            log.error("批量存储向量嵌入失败", e);
            throw new RuntimeException("批量存储向量嵌入失败", e);
        }
    }

    public List<EmbeddingMatch<TextSegment>> search(Embedding queryEmbedding, int maxResults, double minScore) {
        try {
            var indexName = esConfig.getIndex().getChunks();

            // 构建 kNN 查询请求体
            ObjectNode knnNode = objectMapper.createObjectNode();
            knnNode.put("field", "embedding");
            var queryVecArray = knnNode.putArray("query_vector");
            queryEmbedding.vectorAsList().forEach(queryVecArray::add);
            knnNode.put("k", maxResults * 2);
            knnNode.put("num_candidates", maxResults * 10);

            ObjectNode queryBody = objectMapper.createObjectNode();
            queryBody.set("knn", knnNode);
            queryBody.put("size", maxResults);
            queryBody.put("min_score", minScore);

            Request searchRequest = new Request("POST", "/" + indexName + "/_search");
            searchRequest.setEntity(new StringEntity(queryBody.toString(), ContentType.APPLICATION_JSON));
            var response = restClient.performRequest(searchRequest);

            JsonNode result = objectMapper.readTree(response.getEntity().getContent());
            var hits = result.get("hits").get("hits");

            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
            if (hits != null && hits.isArray()) {
                for (var hit : hits) {
                    var source = hit.get("_source");
                    if (source == null) continue;

                    String text = source.get("text") != null ? source.get("text").asText() : "";
                    String src = source.get("source") != null ? source.get("source").asText() : "";
                    String cat = source.get("category") != null ? source.get("category").asText() : "";
                    double score = hit.get("_score") != null ? hit.get("_score").asDouble() : 0.0;
                    String id = hit.get("_id") != null ? hit.get("_id").asText() : "";

                    var segment = TextSegment.from(text);
                    segment.metadata().put("source", src);
                    segment.metadata().put("category", cat);

                    matches.add(new EmbeddingMatch<>(score, id, null, segment));
                }
            }

            return matches;
        } catch (Exception e) {
            log.error("搜索向量嵌入失败", e);
            throw new RuntimeException("搜索向量嵌入失败", e);
        }
    }

    /**
     * 统计索引中的文档数量。
     */
    public long count() {
        try {
            Request countReq = new Request("GET", "/" + esConfig.getIndex().getChunks() + "/_count");
            var response = restClient.performRequest(countReq);
            JsonNode result = objectMapper.readTree(response.getEntity().getContent());
            return result.get("count").asLong();
        } catch (Exception e) {
            log.warn("统计文档数量失败", e);
            return 0;
        }
    }

    /** 存储所有文件的哈希值（文件名 → SHA256），用于检测文档变更 */
    public void saveDocHashes(java.util.Map<String, String> hashes) {
        try {
            ObjectNode doc = objectMapper.createObjectNode();
            ObjectNode hashNode = doc.putObject("file_hashes");
            for (var entry : hashes.entrySet()) {
                hashNode.put(entry.getKey(), entry.getValue());
            }
            doc.put("updated_at", java.time.Instant.now().toString());
            Request req = new Request("PUT", "/" + esConfig.getIndex().getChunks() + "/_doc/doc_hashes");
            req.setEntity(new StringEntity(doc.toString(), ContentType.APPLICATION_JSON));
            restClient.performRequest(req);
            log.debug("文件哈希已保存");
        } catch (Exception e) {
            log.warn("保存文件哈希失败", e);
        }
    }

    /** 读取所有文件的哈希值，无记录时返回空 Map */
    public java.util.Map<String, String> getAllDocHashes() {
        try {
            Request req = new Request("GET", "/" + esConfig.getIndex().getChunks() + "/_doc/doc_hashes");
            var response = restClient.performRequest(req);
            JsonNode result = objectMapper.readTree(response.getEntity().getContent());
            JsonNode hashes = result.get("_source").get("file_hashes");
            if (hashes == null) return java.util.Map.of();

            java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
            var fields = hashes.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                map.put(entry.getKey(), entry.getValue().asText());
            }
            return map;
        } catch (Exception e) {
            return java.util.Map.of();
        }
    }

    public void clear() {
        try {
            var indexName = esConfig.getIndex().getChunks();

            Request existsReq = new Request("HEAD", "/" + indexName);
            boolean exists = restClient.performRequest(existsReq).getStatusLine().getStatusCode() == 200;

            if (exists) {
                Request deleteReq = new Request("DELETE", "/" + indexName);
                restClient.performRequest(deleteReq);
                log.info("已删除索引：{}", indexName);
                ensureIndexExists(indexName);
            }
        } catch (Exception e) {
            log.error("清空索引失败", e);
        }
    }
}

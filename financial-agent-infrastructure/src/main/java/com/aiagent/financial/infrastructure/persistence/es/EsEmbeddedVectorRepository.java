package com.aiagent.financial.infrastructure.persistence.es;

import com.aiagent.financial.domain.model.rag.VectorEntry;
import com.aiagent.financial.domain.model.rag.VectorMatch;
import com.aiagent.financial.domain.repository.EmbeddedVectorRepository;
import com.aiagent.financial.config.ElasticsearchConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 基于 Elasticsearch 的向量仓储实现。
 *
 * <p>用底层 REST 客户端建 dense_vector 索引并执行 kNN 检索。
 * 本地 ES 与阿里云 ES 均可通过 RestClient 接入。</p>
 */
@Repository
public class EsEmbeddedVectorRepository implements EmbeddedVectorRepository {

    private static final Logger log = LoggerFactory.getLogger(EsEmbeddedVectorRepository.class);

    private final RestClient restClient;

    private final ElasticsearchConfig esConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造 ES 向量仓储。
     *
     * @param restClient ES REST 客户端
     * @param esConfig   ES 索引配置
     */
    public EsEmbeddedVectorRepository(RestClient restClient, ElasticsearchConfig esConfig) {
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
                Response response = restClient.performRequest(createReq);
                log.info("索引 {} 已创建：{}", indexName, response.getStatusLine());
            } else {
                log.info("索引 {} 已存在", indexName);
            }
        } catch (Exception e) {
            log.error("确保索引存在时失败：{}，请确认 ES 已启动或使用 local 内存实现", indexName, e);
        }
    }

    @Override
    public void storeAll(List<VectorEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        try {
            String indexName = esConfig.getIndex().getChunks();
            StringBuilder bulkBody = new StringBuilder();
            for (VectorEntry entry : entries) {
                bulkBody.append("{\"index\":{\"_index\":\"").append(indexName).append("\"}}\n");
                ObjectNode doc = objectMapper.createObjectNode();
                ArrayNode embArray = doc.putArray("embedding");
                for (float value : entry.embedding()) {
                    embArray.add(value);
                }
                doc.put("text", entry.text());
                doc.put("source", entry.source());
                doc.put("category", entry.category());
                bulkBody.append(doc).append("\n");
            }
            Request bulkRequest = new Request("POST", "/_bulk");
            bulkRequest.setEntity(new StringEntity(bulkBody.toString(), ContentType.APPLICATION_JSON));
            Response response = restClient.performRequest(bulkRequest);
            JsonNode bulkResult = objectMapper.readTree(response.getEntity().getContent());
            if (bulkResult.has("errors") && bulkResult.get("errors").asBoolean()) {
                log.warn("批量写入向量失败，部分文档未成功");
            } else {
                log.info("批量存储了 {} 个向量条目到 {}", entries.size(), indexName);
            }
        } catch (Exception e) {
            log.error("批量存储向量失败", e);
            throw new RuntimeException("批量存储向量失败", e);
        }
    }

    @Override
    public List<VectorMatch> findSimilar(float[] queryVector, int topK, double minScore) {
        try {
            String indexName = esConfig.getIndex().getChunks();
            ObjectNode knnNode = objectMapper.createObjectNode();
            knnNode.put("field", "embedding");
            ArrayNode queryVecArray = knnNode.putArray("query_vector");
            for (float value : queryVector) {
                queryVecArray.add(value);
            }
            knnNode.put("k", topK * 2);
            knnNode.put("num_candidates", topK * 10);

            ObjectNode queryBody = objectMapper.createObjectNode();
            queryBody.set("knn", knnNode);
            queryBody.put("size", topK);
            queryBody.put("min_score", minScore);

            Request searchRequest = new Request("POST", "/" + indexName + "/_search");
            searchRequest.setEntity(new StringEntity(queryBody.toString(), ContentType.APPLICATION_JSON));
            Response response = restClient.performRequest(searchRequest);

            JsonNode result = objectMapper.readTree(response.getEntity().getContent());
            JsonNode hits = result.get("hits").get("hits");

            List<VectorMatch> matches = new ArrayList<>();
            if (hits != null && hits.isArray()) {
                for (JsonNode hit : hits) {
                    JsonNode source = hit.get("_source");
                    if (source == null) {
                        continue;
                    }
                    String text = source.get("text") != null ? source.get("text").asText() : "";
                    String src = source.get("source") != null ? source.get("source").asText() : "";
                    String cat = source.get("category") != null ? source.get("category").asText() : "";
                    double score = hit.get("_score") != null ? hit.get("_score").asDouble() : 0.0;
                    String id = hit.get("_id") != null ? hit.get("_id").asText() : "";
                    matches.add(new VectorMatch(id, text, src, cat, score));
                }
            }
            return matches;
        } catch (Exception e) {
            log.error("搜索向量失败", e);
            throw new RuntimeException("搜索向量失败", e);
        }
    }

    @Override
    public void saveDocHashes(Map<String, String> hashes) {
        try {
            ObjectNode doc = objectMapper.createObjectNode();
            ObjectNode hashNode = doc.putObject("file_hashes");
            for (Map.Entry<String, String> entry : hashes.entrySet()) {
                hashNode.put(entry.getKey(), entry.getValue());
            }
            doc.put("updated_at", java.time.Instant.now().toString());
            Request req = new Request("PUT", "/" + esConfig.getIndex().getChunks() + "/_doc/doc_hashes");
            req.setEntity(new StringEntity(doc.toString(), ContentType.APPLICATION_JSON));
            restClient.performRequest(req);
        } catch (Exception e) {
            log.warn("保存文档哈希失败", e);
        }
    }

    @Override
    public Map<String, String> getAllDocHashes() {
        try {
            Request req = new Request("GET", "/" + esConfig.getIndex().getChunks() + "/_doc/doc_hashes");
            Response response = restClient.performRequest(req);
            JsonNode result = objectMapper.readTree(response.getEntity().getContent());
            JsonNode hashes = result.get("_source").get("file_hashes");
            if (hashes == null) {
                return Map.of();
            }
            java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = hashes.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                map.put(entry.getKey(), entry.getValue().asText());
            }
            return map;
        } catch (Exception e) {
            return Map.of();
        }
    }

    @Override
    public long count() {
        try {
            Request countReq = new Request("GET", "/" + esConfig.getIndex().getChunks() + "/_count");
            Response response = restClient.performRequest(countReq);
            JsonNode result = objectMapper.readTree(response.getEntity().getContent());
            return result.get("count").asLong();
        } catch (Exception e) {
            log.warn("统计向量数量失败", e);
            return 0;
        }
    }
}

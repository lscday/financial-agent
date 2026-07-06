package com.aiagent.financial.config;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchConfig {

    private String hosts = "http://localhost:9200";
    private String username;
    private String password;
    private Index index = new Index();

    public String getHosts() { return hosts; }
    public void setHosts(String hosts) { this.hosts = hosts; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Index getIndex() { return index; }
    public void setIndex(Index index) { this.index = index; }

    public static class Index {
        private String chunks = "finance_chunks";
        private String docs = "fin_rag_docs";

        public String getChunks() { return chunks; }
        public void setChunks(String chunks) { this.chunks = chunks; }
        public String getDocs() { return docs; }
        public void setDocs(String docs) { this.docs = docs; }
    }

    @Bean
    public RestClient restClient() {
        List<String> hostList = List.of(hosts.split(","));
        HttpHost[] httpHosts = hostList.stream()
                .map(String::trim)
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);

        RestClientBuilder builder = RestClient.builder(httpHosts);

        if (username != null && !username.isEmpty()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password != null ? password : ""));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        builder.setDefaultHeaders(new Header[]{
                new BasicHeader("Content-Type", "application/json")
        });

        return builder.build();
    }
}

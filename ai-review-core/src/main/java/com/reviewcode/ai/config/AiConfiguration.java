package com.reviewcode.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfiguration {
    
    private Mcp mcp = new Mcp();
    private Review review = new Review();
    
    @Bean
    public WebClient aiWebClient() {
        return WebClient.builder()
                .baseUrl(mcp.getEndpoint())
                .defaultHeader("Authorization", "Bearer " + mcp.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
    
    public static class Mcp {
        private String endpoint = "http://localhost:3000";
        private String apiKey = "";
        private int timeout = 30000;
        
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
    }
    
    public static class Review {
        private int maxFileSize = 1048576;
        private List<String> supportedExtensions = List.of(".java", ".js", ".ts", ".py");
        
        public int getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(int maxFileSize) { this.maxFileSize = maxFileSize; }
        
        public List<String> getSupportedExtensions() { return supportedExtensions; }
        public void setSupportedExtensions(List<String> supportedExtensions) { this.supportedExtensions = supportedExtensions; }
    }
    
    public Mcp getMcp() { return mcp; }
    public void setMcp(Mcp mcp) { this.mcp = mcp; }
    
    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }
}
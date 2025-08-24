package com.reviewcode.ai.mcp;

import java.util.List;
import java.util.Map;

/**
 * Enhanced review result with MCP tool integration
 */
public class EnhancedReviewResult {
    
    private String sessionId;
    private boolean success;
    private String error;
    private EnhancedMCPClient.AIResponse aiResponse;
    private List<MCPToolResult> toolResults;
    private Map<String, Object> metadata;
    private long timestamp;
    
    private EnhancedReviewResult() {}
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static EnhancedReviewResult error(String error) {
        EnhancedReviewResult result = new EnhancedReviewResult();
        result.success = false;
        result.error = error;
        result.timestamp = System.currentTimeMillis();
        return result;
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public EnhancedMCPClient.AIResponse getAiResponse() { return aiResponse; }
    public List<MCPToolResult> getToolResults() { return toolResults; }
    public Map<String, Object> getMetadata() { return metadata; }
    public long getTimestamp() { return timestamp; }
    
    public static class Builder {
        private EnhancedReviewResult result = new EnhancedReviewResult();
        
        public Builder sessionId(String sessionId) {
            result.sessionId = sessionId;
            return this;
        }
        
        public Builder success(boolean success) {
            result.success = success;
            return this;
        }
        
        public Builder error(String error) {
            result.error = error;
            return this;
        }
        
        public Builder aiResponse(EnhancedMCPClient.AIResponse aiResponse) {
            result.aiResponse = aiResponse;
            return this;
        }
        
        public Builder toolResults(List<MCPToolResult> toolResults) {
            result.toolResults = toolResults;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            result.metadata = metadata;
            return this;
        }
        
        public Builder timestamp(long timestamp) {
            result.timestamp = timestamp;
            return this;
        }
        
        public EnhancedReviewResult build() {
            result.success = result.error == null;
            if (result.timestamp == 0) {
                result.timestamp = System.currentTimeMillis();
            }
            return result;
        }
    }
}
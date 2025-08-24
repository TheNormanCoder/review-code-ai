package com.reviewcode.ai.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Result returned by MCP tool execution
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MCPToolResult {
    
    private boolean success;
    private Object content;
    private String error;
    private String mimeType;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
    
    public MCPToolResult() {
        this.timestamp = LocalDateTime.now();
    }
    
    public static MCPToolResult success(Object content) {
        MCPToolResult result = new MCPToolResult();
        result.success = true;
        result.content = content;
        return result;
    }
    
    public static MCPToolResult success(Object content, String mimeType) {
        MCPToolResult result = success(content);
        result.mimeType = mimeType;
        return result;
    }
    
    public static MCPToolResult error(String error) {
        MCPToolResult result = new MCPToolResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    public static MCPToolResult withMetadata(Object content, Map<String, Object> metadata) {
        MCPToolResult result = success(content);
        result.metadata = metadata;
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public Object getContent() { return content; }
    public void setContent(Object content) { this.content = content; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
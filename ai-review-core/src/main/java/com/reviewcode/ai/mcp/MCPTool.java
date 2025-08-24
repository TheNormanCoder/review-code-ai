package com.reviewcode.ai.mcp;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Base interface for all MCP tools
 * Follows the Model Context Protocol specification
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = GitTool.class, name = "git"),
    @JsonSubTypes.Type(value = DatabaseTool.class, name = "database"),
    @JsonSubTypes.Type(value = FileSystemTool.class, name = "filesystem"),
    @JsonSubTypes.Type(value = NotificationTool.class, name = "notification")
})
public interface MCPTool {
    
    /**
     * Get tool name/identifier
     */
    String getName();
    
    /**
     * Get tool description for AI model
     */
    String getDescription();
    
    /**
     * Get tool input schema
     */
    Map<String, Object> getInputSchema();
    
    /**
     * Execute the tool with given parameters
     */
    Mono<MCPToolResult> execute(Map<String, Object> parameters);
    
    /**
     * Check if tool is available/enabled
     */
    default boolean isAvailable() {
        return true;
    }
    
    /**
     * Get tool capabilities/permissions required
     */
    default String[] getRequiredCapabilities() {
        return new String[0];
    }
}
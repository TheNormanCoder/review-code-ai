package com.reviewcode.ai.mcp;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP Session for maintaining persistent context with AI models
 * Supports tool chaining and state management
 */
public class MCPSession {
    
    private final String sessionId;
    private final Map<String, Object> context;
    private final List<MCPTool> availableTools;
    private boolean isActive;
    
    public MCPSession(List<MCPTool> tools) {
        this.sessionId = UUID.randomUUID().toString();
        this.context = new java.util.concurrent.ConcurrentHashMap<>();
        this.availableTools = tools;
        this.isActive = true;
    }
    
    /**
     * Execute a single tool
     */
    public Mono<MCPToolResult> executeTool(String toolName, Map<String, Object> parameters) {
        return Mono.fromCallable(() -> findTool(toolName))
                .flatMap(tool -> tool.execute(parameters))
                .doOnNext(result -> updateContext(toolName, result));
    }
    
    /**
     * Execute multiple tools in sequence (tool chaining)
     */
    public Mono<List<MCPToolResult>> executeToolChain(List<MCPToolRequest> requests) {
        return Flux.fromIterable(requests)
                .concatMap(request -> executeTool(request.getToolName(), request.getParameters()))
                .collectList();
    }
    
    /**
     * Execute tools in parallel
     */
    public Mono<List<MCPToolResult>> executeToolsParallel(List<MCPToolRequest> requests) {
        return Flux.fromIterable(requests)
                .flatMap(request -> executeTool(request.getToolName(), request.getParameters()))
                .collectList();
    }
    
    /**
     * Stream tool execution results in real-time
     */
    public Flux<MCPToolResult> streamToolExecution(List<MCPToolRequest> requests) {
        return Flux.fromIterable(requests)
                .concatMap(request -> executeTool(request.getToolName(), request.getParameters()));
    }
    
    /**
     * Get available tools for AI model
     */
    public List<MCPToolSchema> getAvailableToolsSchema() {
        return availableTools.stream()
                .filter(MCPTool::isAvailable)
                .map(tool -> new MCPToolSchema(
                    tool.getName(),
                    tool.getDescription(),
                    tool.getInputSchema()
                ))
                .toList();
    }
    
    /**
     * Add context information for AI model
     */
    public void addContext(String key, Object value) {
        context.put(key, value);
    }
    
    /**
     * Get context information
     */
    @SuppressWarnings("unchecked")
    public <T> T getContext(String key, Class<T> type) {
        return (T) context.get(key);
    }
    
    /**
     * Close session and cleanup resources
     */
    public void close() {
        this.isActive = false;
        context.clear();
    }
    
    private MCPTool findTool(String toolName) {
        return availableTools.stream()
                .filter(tool -> tool.getName().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolName));
    }
    
    private void updateContext(String toolName, MCPToolResult result) {
        if (result.isSuccess()) {
            context.put("last_" + toolName + "_result", result.getContent());
        }
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public Map<String, Object> getContext() { return Map.copyOf(context); }
    public boolean isActive() { return isActive; }
    
    /**
     * Tool request for chaining
     */
    public static class MCPToolRequest {
        private String toolName;
        private Map<String, Object> parameters;
        
        public MCPToolRequest(String toolName, Map<String, Object> parameters) {
            this.toolName = toolName;
            this.parameters = parameters;
        }
        
        public String getToolName() { return toolName; }
        public Map<String, Object> getParameters() { return parameters; }
    }
    
    /**
     * Tool schema for AI model
     */
    public static class MCPToolSchema {
        private String name;
        private String description;
        private Map<String, Object> inputSchema;
        
        public MCPToolSchema(String name, String description, Map<String, Object> inputSchema) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, Object> getInputSchema() { return inputSchema; }
    }
}
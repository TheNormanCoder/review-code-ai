package com.reviewcode.ai.controller;

import com.reviewcode.ai.mcp.EnhancedMCPClient;
import com.reviewcode.ai.mcp.EnhancedReviewResult;
import com.reviewcode.ai.mcp.MCPSession;
import com.reviewcode.ai.model.PullRequest;
import com.reviewcode.ai.service.PullRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for MCP operations
 * Provides HTTP endpoints for MCP tool access and AI review functionality
 */
@RestController
@RequestMapping("/api/mcp")
@CrossOrigin(origins = "*") // Configure properly for production
public class MCPController {
    
    @Autowired
    private EnhancedMCPClient mcpClient;
    
    @Autowired
    private PullRequestService pullRequestService;
    
    /**
     * Create a new MCP session
     */
    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createSession() {
        MCPSession session = mcpClient.createSession(null);
        
        return ResponseEntity.ok(Map.of(
            "session_id", session.getSessionId(),
            "available_tools", session.getAvailableToolsSchema(),
            "status", "created"
        ));
    }
    
    /**
     * Get available MCP tools
     */
    @GetMapping("/tools")
    public ResponseEntity<List<MCPSession.MCPToolSchema>> getAvailableTools() {
        MCPSession tempSession = mcpClient.createSession(null);
        List<MCPSession.MCPToolSchema> tools = tempSession.getAvailableToolsSchema();
        tempSession.close();
        
        return ResponseEntity.ok(tools);
    }
    
    /**
     * Execute AI with specific tools
     */
    @PostMapping("/ai/execute-with-tools")
    public Mono<ResponseEntity<EnhancedMCPClient.AIResponse>> executeWithTools(
            @RequestBody AIExecutionRequest request) {
        
        return mcpClient.executeWithToolChain(
            request.getPrompt(), 
            request.getToolNames(), 
            request.getContext()
        ).map(ResponseEntity::ok);
    }
    
    /**
     * Perform advanced review with MCP tools
     */
    @PostMapping("/reviews/advanced/{pullRequestId}")
    public Mono<ResponseEntity<EnhancedReviewResult>> performAdvancedReview(
            @PathVariable Long pullRequestId,
            @RequestBody(required = false) EnhancedMCPClient.ReviewOptions options) {
        
        if (options == null) {
            options = new EnhancedMCPClient.ReviewOptions();
        }
        
        return pullRequestService.findById(pullRequestId)
            .flatMap(pullRequest -> mcpClient.performAdvancedReview(pullRequest, options))
            .map(ResponseEntity::ok);
    }
    
    /**
     * Stream review results in real-time
     */
    @GetMapping(value = "/reviews/stream/{pullRequestId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<EnhancedMCPClient.ReviewUpdate> streamReview(
            @PathVariable Long pullRequestId,
            @RequestParam(required = false) String focusAreas,
            @RequestParam(required = false, defaultValue = "medium") String severityThreshold) {
        
        EnhancedMCPClient.ReviewOptions options = new EnhancedMCPClient.ReviewOptions();
        if (focusAreas != null && !focusAreas.isEmpty()) {
            options.setFocusAreas(List.of(focusAreas.split(",")));
        }
        options.setSeverityThreshold(severityThreshold);
        
        return pullRequestService.findById(pullRequestId)
            .flatMapMany(pullRequest -> mcpClient.streamReview(pullRequest, options));
    }
    
    /**
     * Execute specific MCP tool
     */
    @PostMapping("/tools/{toolName}/execute")
    public Mono<ResponseEntity<Object>> executeTool(
            @PathVariable String toolName,
            @RequestBody Map<String, Object> parameters) {
        
        MCPSession session = mcpClient.createSession(null);
        
        return session.executeTool(toolName, parameters)
            .map(result -> {
                if (result.isSuccess()) {
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "content", result.getContent(),
                        "metadata", result.getMetadata()
                    ));
                } else {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", result.getError()
                    ));
                }
            })
            .doFinally(signal -> session.close());
    }
    
    /**
     * Execute tool chain
     */
    @PostMapping("/tools/chain")
    public Mono<ResponseEntity<List<Object>>> executeToolChain(
            @RequestBody ToolChainRequest request) {
        
        MCPSession session = mcpClient.createSession(null);
        
        List<MCPSession.MCPToolRequest> toolRequests = request.getTools().stream()
            .map(tool -> new MCPSession.MCPToolRequest(tool.getName(), tool.getParameters()))
            .toList();
        
        return session.executeToolChain(toolRequests)
            .map(results -> results.stream()
                .map(result -> Map.of(
                    "success", result.isSuccess(),
                    "content", result.getContent(),
                    "error", result.getError(),
                    "metadata", result.getMetadata()
                ))
                .toList()
            )
            .map(ResponseEntity::ok)
            .doFinally(signal -> session.close());
    }
    
    /**
     * Get active sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getActiveSessions() {
        Map<String, MCPSession> sessions = mcpClient.getActiveSessions();
        
        Map<String, Object> sessionInfo = sessions.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> Map.of(
                    "session_id", entry.getValue().getSessionId(),
                    "is_active", entry.getValue().isActive(),
                    "context_size", entry.getValue().getContext().size()
                )
            ));
        
        return ResponseEntity.ok(Map.of(
            "active_sessions", sessionInfo.size(),
            "sessions", sessionInfo
        ));
    }
    
    /**
     * Close MCP session
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, String>> closeSession(@PathVariable String sessionId) {
        mcpClient.closeSession(sessionId);
        
        return ResponseEntity.ok(Map.of(
            "status", "closed",
            "session_id", sessionId
        ));
    }
    
    // Request DTOs
    public static class AIExecutionRequest {
        private String prompt;
        private List<String> toolNames;
        private Map<String, Object> context;
        
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        
        public List<String> getToolNames() { return toolNames; }
        public void setToolNames(List<String> toolNames) { this.toolNames = toolNames; }
        
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }
    
    public static class ToolChainRequest {
        private List<ToolRequest> tools;
        
        public List<ToolRequest> getTools() { return tools; }
        public void setTools(List<ToolRequest> tools) { this.tools = tools; }
        
        public static class ToolRequest {
            private String name;
            private Map<String, Object> parameters;
            
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            
            public Map<String, Object> getParameters() { return parameters; }
            public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        }
    }
}
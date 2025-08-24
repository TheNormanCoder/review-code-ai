package com.reviewcode.ai.mcp;

import com.reviewcode.ai.config.AiConfiguration;
import com.reviewcode.ai.model.PullRequest;
import com.reviewcode.ai.model.ReviewSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced MCP Client with tool chaining and advanced AI integration
 * Supports multi-model AI, context management, and real-time streaming
 */
@Service
public class EnhancedMCPClient {
    
    private final WebClient aiWebClient;
    private final AiConfiguration aiConfig;
    private final List<MCPTool> availableTools;
    private final Map<String, MCPSession> activeSessions;
    private final MCPContextManager contextManager;
    
    @Autowired
    public EnhancedMCPClient(WebClient aiWebClient, 
                           AiConfiguration aiConfig, 
                           List<MCPTool> mcpTools,
                           MCPContextManager contextManager) {
        this.aiWebClient = aiWebClient;
        this.aiConfig = aiConfig;
        this.availableTools = mcpTools;
        this.activeSessions = new ConcurrentHashMap<>();
        this.contextManager = contextManager;
    }
    
    /**
     * Create a new MCP session with full tool access
     */
    public MCPSession createSession(String sessionId) {
        MCPSession session = new MCPSession(availableTools);
        activeSessions.put(sessionId != null ? sessionId : session.getSessionId(), session);
        return session;
    }
    
    /**
     * Perform advanced code review with AI tool chaining
     */
    public Mono<EnhancedReviewResult> performAdvancedReview(PullRequest pullRequest, ReviewOptions options) {
        MCPSession session = createSession(null);
        return performReviewWithSession(pullRequest, options, session);
    }
    
    /**
     * Stream code review results in real-time
     */
    public Flux<ReviewUpdate> streamReview(PullRequest pullRequest, ReviewOptions options) {
        MCPSession session = createSession(null);
        
        return prepareReviewContext(pullRequest, session)
            .thenMany(executeReviewPipeline(pullRequest, options, session))
            .map(this::convertToReviewUpdate)
            .doFinally(signal -> session.close());
    }
    
    /**
     * Execute AI with specific tools in sequence
     */
    public Mono<AIResponse> executeWithToolChain(String prompt, List<String> toolNames, Map<String, Object> context) {
        MCPSession session = createSession(null);
        
        // Add context to session
        context.forEach(session::addContext);
        
        // Create AI request with tool schema
        List<MCPSession.MCPToolSchema> toolSchemas = session.getAvailableToolsSchema()
            .stream()
            .filter(schema -> toolNames.isEmpty() || toolNames.contains(schema.getName()))
            .toList();
        
        Map<String, Object> aiRequest = Map.of(
            "prompt", prompt,
            "tools", toolSchemas,
            "context", session.getContext(),
            "session_id", session.getSessionId()
        );
        
        return aiWebClient
            .post()
            .uri("/api/ai/chat-with-tools")
            .bodyValue(aiRequest)
            .retrieve()
            .bodyToMono(AIResponse.class)
            .timeout(Duration.ofMillis(aiConfig.getMcp().getTimeout()))
            .flatMap(response -> processAIResponse(response, session))
            .doFinally(signal -> session.close());
    }
    
    private Mono<EnhancedReviewResult> performReviewWithSession(PullRequest pullRequest, ReviewOptions options, MCPSession session) {
        return prepareReviewContext(pullRequest, session)
            .then(executeComprehensiveReview(pullRequest, options, session))
            .doFinally(signal -> session.close());
    }
    
    private Mono<Void> prepareReviewContext(PullRequest pullRequest, MCPSession session) {
        // Add PR context to session
        session.addContext("pull_request", pullRequest);
        session.addContext("repository_url", pullRequest.getRepositoryUrl());
        session.addContext("branch", pullRequest.getSourceBranch());
        
        // Get Git information
        List<MCPSession.MCPToolRequest> contextRequests = List.of(
            new MCPSession.MCPToolRequest("git", Map.of(
                "command", "diff",
                "repository", pullRequest.getRepositoryUrl(),
                "parameters", Map.of("commit", pullRequest.getSourceBranch())
            )),
            new MCPSession.MCPToolRequest("git", Map.of(
                "command", "log",
                "repository", pullRequest.getRepositoryUrl(),
                "parameters", Map.of("author", pullRequest.getAuthor(), "since", "7 days ago")
            )),
            new MCPSession.MCPToolRequest("database", Map.of(
                "query", "recent_reviews",
                "parameters", Map.of("author", pullRequest.getAuthor())
            ))
        );
        
        return session.executeToolChain(contextRequests)
            .doOnNext(results -> {
                // Store context results
                for (int i = 0; i < results.size(); i++) {
                    session.addContext("context_" + i, results.get(i).getContent());
                }
            })
            .then();
    }
    
    private Mono<EnhancedReviewResult> executeComprehensiveReview(PullRequest pullRequest, ReviewOptions options, MCPSession session) {
        // Create comprehensive review prompt with all context
        String prompt = buildComprehensivePrompt(pullRequest, options, session);
        
        Map<String, Object> aiRequest = Map.of(
            "prompt", prompt,
            "tools", session.getAvailableToolsSchema(),
            "context", session.getContext(),
            "options", Map.of(
                "focus_areas", options.getFocusAreas(),
                "severity_threshold", options.getSeverityThreshold(),
                "include_suggestions", options.isIncludeSuggestions()
            )
        );
        
        return aiWebClient
            .post()
            .uri("/api/ai/comprehensive-review")
            .bodyValue(aiRequest)
            .retrieve()
            .bodyToMono(AIResponse.class)
            .timeout(Duration.ofMillis(aiConfig.getMcp().getTimeout() * 2)) // Longer timeout for comprehensive review
            .flatMap(response -> processComprehensiveResponse(response, session, options))
            .onErrorResume(error -> {
                return Mono.just(EnhancedReviewResult.error(
                    "Comprehensive review failed: " + error.getMessage()
                ));
            });
    }
    
    private Flux<MCPToolResult> executeReviewPipeline(PullRequest pullRequest, ReviewOptions options, MCPSession session) {
        // Define review pipeline stages
        List<List<MCPSession.MCPToolRequest>> pipeline = List.of(
            // Stage 1: Context gathering
            List.of(
                new MCPSession.MCPToolRequest("git", Map.of("command", "status", "repository", pullRequest.getRepositoryUrl())),
                new MCPSession.MCPToolRequest("database", Map.of("query", "recent_reviews"))
            ),
            // Stage 2: File analysis
            List.of(
                new MCPSession.MCPToolRequest("filesystem", Map.of("operation", "analyze_structure", "path", pullRequest.getRepositoryUrl())),
                new MCPSession.MCPToolRequest("git", Map.of("command", "diff", "repository", pullRequest.getRepositoryUrl()))
            ),
            // Stage 3: Security and quality checks (would be done by AI)
            List.of(
                new MCPSession.MCPToolRequest("database", Map.of("query", "security_findings")),
                new MCPSession.MCPToolRequest("database", Map.of("query", "quality_trends"))
            )
        );
        
        return Flux.fromIterable(pipeline)
            .concatMap(stage -> session.executeToolsParallel(stage).flux().flatMap(Flux::fromIterable))
            .delayElements(Duration.ofMillis(100)); // Small delay for streaming effect
    }
    
    private ReviewUpdate convertToReviewUpdate(MCPToolResult result) {
        return new ReviewUpdate(
            result.isSuccess() ? "completed" : "error",
            result.getContent(),
            result.getError(),
            result.getMetadata()
        );
    }
    
    private Mono<AIResponse> processAIResponse(AIResponse response, MCPSession session) {
        // If AI response contains tool calls, execute them
        if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
            List<MCPSession.MCPToolRequest> toolRequests = response.getToolCalls().stream()
                .map(call -> new MCPSession.MCPToolRequest(call.getName(), call.getParameters()))
                .toList();
            
            return session.executeToolChain(toolRequests)
                .map(results -> {
                    // Add tool results to AI response
                    response.setToolResults(results);
                    return response;
                });
        }
        
        return Mono.just(response);
    }
    
    private Mono<EnhancedReviewResult> processComprehensiveResponse(AIResponse aiResponse, MCPSession session, ReviewOptions options) {
        // Process AI response and create comprehensive result
        EnhancedReviewResult.Builder builder = EnhancedReviewResult.builder()
            .sessionId(session.getSessionId())
            .aiResponse(aiResponse)
            .timestamp(System.currentTimeMillis());
        
        // If AI requested tool execution, handle it
        if (aiResponse.getToolCalls() != null && !aiResponse.getToolCalls().isEmpty()) {
            List<MCPSession.MCPToolRequest> toolRequests = aiResponse.getToolCalls().stream()
                .map(call -> new MCPSession.MCPToolRequest(call.getName(), call.getParameters()))
                .toList();
            
            return session.executeToolChain(toolRequests)
                .map(toolResults -> {
                    builder.toolResults(toolResults);
                    
                    // Send notifications if critical issues found
                    if (hasCriticalFindings(toolResults)) {
                        sendCriticalNotification(session, toolResults);
                    }
                    
                    return builder.build();
                });
        }
        
        return Mono.just(builder.build());
    }
    
    private String buildComprehensivePrompt(PullRequest pullRequest, ReviewOptions options, MCPSession session) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Perform a comprehensive code review for the following pull request:\n\n");
        prompt.append("Title: ").append(pullRequest.getTitle()).append("\n");
        prompt.append("Author: ").append(pullRequest.getAuthor()).append("\n");
        prompt.append("Description: ").append(pullRequest.getDescription()).append("\n\n");
        
        prompt.append("Focus Areas: ").append(String.join(", ", options.getFocusAreas())).append("\n");
        prompt.append("Severity Threshold: ").append(options.getSeverityThreshold()).append("\n\n");
        
        prompt.append("Available tools for analysis:\n");
        session.getAvailableToolsSchema().forEach(tool -> {
            prompt.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
        });
        
        prompt.append("\nPlease use the available tools to:\n");
        prompt.append("1. Analyze the code changes and their impact\n");
        prompt.append("2. Check for security vulnerabilities\n");
        prompt.append("3. Assess code quality and maintainability\n");
        prompt.append("4. Review test coverage and documentation\n");
        prompt.append("5. Compare with historical patterns and team standards\n");
        prompt.append("6. Provide actionable suggestions for improvement\n");
        
        return prompt.toString();
    }
    
    private boolean hasCriticalFindings(List<MCPToolResult> toolResults) {
        // Check if any tool results indicate critical issues
        return toolResults.stream()
            .anyMatch(result -> result.getMetadata() != null && 
                "critical".equals(result.getMetadata().get("severity")));
    }
    
    private void sendCriticalNotification(MCPSession session, List<MCPToolResult> toolResults) {
        // Send notification about critical findings
        session.executeTool("notification", Map.of(
            "channel", "slack",
            "message", "Critical security vulnerabilities found in code review",
            "severity", "critical",
            "parameters", Map.of(
                "title", "Critical Code Review Alert",
                "findings", toolResults
            )
        )).subscribe();
    }
    
    public void closeSession(String sessionId) {
        MCPSession session = activeSessions.remove(sessionId);
        if (session != null) {
            session.close();
        }
    }
    
    public Map<String, MCPSession> getActiveSessions() {
        return Map.copyOf(activeSessions);
    }
    
    // Supporting classes
    public static class ReviewOptions {
        private List<String> focusAreas = List.of("security", "performance", "maintainability");
        private String severityThreshold = "medium";
        private boolean includeSuggestions = true;
        
        // Getters and setters
        public List<String> getFocusAreas() { return focusAreas; }
        public void setFocusAreas(List<String> focusAreas) { this.focusAreas = focusAreas; }
        
        public String getSeverityThreshold() { return severityThreshold; }
        public void setSeverityThreshold(String severityThreshold) { this.severityThreshold = severityThreshold; }
        
        public boolean isIncludeSuggestions() { return includeSuggestions; }
        public void setIncludeSuggestions(boolean includeSuggestions) { this.includeSuggestions = includeSuggestions; }
    }
    
    public static class ReviewUpdate {
        private String status;
        private Object content;
        private String error;
        private Map<String, Object> metadata;
        
        public ReviewUpdate(String status, Object content, String error, Map<String, Object> metadata) {
            this.status = status;
            this.content = content;
            this.error = error;
            this.metadata = metadata;
        }
        
        // Getters
        public String getStatus() { return status; }
        public Object getContent() { return content; }
        public String getError() { return error; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    public static class AIResponse {
        private String content;
        private List<ToolCall> toolCalls;
        private List<MCPToolResult> toolResults;
        private Map<String, Object> metadata;
        
        // Getters and setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public List<ToolCall> getToolCalls() { return toolCalls; }
        public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }
        
        public List<MCPToolResult> getToolResults() { return toolResults; }
        public void setToolResults(List<MCPToolResult> toolResults) { this.toolResults = toolResults; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    public static class ToolCall {
        private String name;
        private Map<String, Object> parameters;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
}
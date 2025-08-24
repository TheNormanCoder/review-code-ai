package com.reviewcode.ai.mcp;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context manager for MCP sessions
 * Handles learning, memory, and context sharing across sessions
 */
@Component
public class MCPContextManager {
    
    private final Map<String, ProjectContext> projectContexts;
    private final Map<String, UserContext> userContexts;
    private final LearningPipeline learningPipeline;
    
    public MCPContextManager() {
        this.projectContexts = new ConcurrentHashMap<>();
        this.userContexts = new ConcurrentHashMap<>();
        this.learningPipeline = new LearningPipeline();
    }
    
    /**
     * Get or create project context
     */
    public ProjectContext getProjectContext(String projectId) {
        return projectContexts.computeIfAbsent(projectId, ProjectContext::new);
    }
    
    /**
     * Get or create user context
     */
    public UserContext getUserContext(String userId) {
        return userContexts.computeIfAbsent(userId, UserContext::new);
    }
    
    /**
     * Learn from review feedback
     */
    public Mono<Void> learnFromFeedback(ReviewFeedback feedback) {
        return learningPipeline.processFeedback(feedback);
    }
    
    /**
     * Get learned patterns for a project
     */
    public Map<String, Object> getLearnedPatterns(String projectId) {
        ProjectContext context = getProjectContext(projectId);
        return context.getLearnedPatterns();
    }
    
    public static class ProjectContext {
        private final String projectId;
        private final Map<String, Object> patterns;
        private final Map<String, Integer> codeMetrics;
        private final Map<String, Object> preferences;
        
        public ProjectContext(String projectId) {
            this.projectId = projectId;
            this.patterns = new ConcurrentHashMap<>();
            this.codeMetrics = new ConcurrentHashMap<>();
            this.preferences = new ConcurrentHashMap<>();
        }
        
        public void addPattern(String pattern, Object value) {
            patterns.put(pattern, value);
        }
        
        public void updateMetric(String metric, int value) {
            codeMetrics.put(metric, value);
        }
        
        public void setPreference(String key, Object value) {
            preferences.put(key, value);
        }
        
        public String getProjectId() { return projectId; }
        public Map<String, Object> getLearnedPatterns() { return Map.copyOf(patterns); }
        public Map<String, Integer> getCodeMetrics() { return Map.copyOf(codeMetrics); }
        public Map<String, Object> getPreferences() { return Map.copyOf(preferences); }
    }
    
    public static class UserContext {
        private final String userId;
        private final Map<String, Object> preferences;
        private final Map<String, Integer> reviewHistory;
        
        public UserContext(String userId) {
            this.userId = userId;
            this.preferences = new ConcurrentHashMap<>();
            this.reviewHistory = new ConcurrentHashMap<>();
        }
        
        public String getUserId() { return userId; }
        public Map<String, Object> getPreferences() { return Map.copyOf(preferences); }
        public Map<String, Integer> getReviewHistory() { return Map.copyOf(reviewHistory); }
    }
    
    public static class ReviewFeedback {
        private String reviewId;
        private String projectId;
        private String userId;
        private boolean helpful;
        private Map<String, Object> details;
        
        // Getters and setters
        public String getReviewId() { return reviewId; }
        public void setReviewId(String reviewId) { this.reviewId = reviewId; }
        
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public boolean isHelpful() { return helpful; }
        public void setHelpful(boolean helpful) { this.helpful = helpful; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
    
    private static class LearningPipeline {
        public Mono<Void> processFeedback(ReviewFeedback feedback) {
            return Mono.fromRunnable(() -> {
                // In a real implementation, this would update ML models
                // For now, just log the feedback
                System.out.println("Processing feedback: " + feedback.getReviewId() + 
                                 " - Helpful: " + feedback.isHelpful());
            });
        }
    }
}
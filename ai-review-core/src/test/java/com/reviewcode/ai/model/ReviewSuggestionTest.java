package com.reviewcode.ai.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReviewSuggestionTest {
    
    private ReviewSuggestion reviewSuggestion;
    private PullRequest pullRequest;
    
    @BeforeEach
    void setUp() {
        pullRequest = new PullRequest();
        pullRequest.setId(1L);
        pullRequest.setTitle("Test PR");
        pullRequest.setAuthor("test-author");
        pullRequest.setRepositoryUrl("https://github.com/test/repo");
        pullRequest.setSourceBranch("feature/test");
        pullRequest.setTargetBranch("main");
        
        reviewSuggestion = new ReviewSuggestion();
    }
    
    @Test
    void shouldCreateReviewSuggestionWithDefaults() {
        assertNotNull(reviewSuggestion.getCreatedAt());
        assertEquals(ReviewSuggestion.SuggestionStatus.PENDING, reviewSuggestion.getStatus());
    }
    
    @Test
    void shouldSetAndGetBasicProperties() {
        reviewSuggestion.setPullRequest(pullRequest);
        reviewSuggestion.setFileName("TestFile.java");
        reviewSuggestion.setLineNumber(42);
        reviewSuggestion.setType(ReviewSuggestion.SuggestionType.SECURITY);
        reviewSuggestion.setSeverity(ReviewSuggestion.Severity.HIGH);
        reviewSuggestion.setDescription("Security issue found");
        reviewSuggestion.setSuggestion("Fix the security vulnerability");
        reviewSuggestion.setCodeSnippet("public void vulnerableMethod() { }");
        reviewSuggestion.setProposedCode("public void secureMethod() { }");
        reviewSuggestion.setRuleId("SEC-001");
        
        assertEquals(pullRequest, reviewSuggestion.getPullRequest());
        assertEquals("TestFile.java", reviewSuggestion.getFileName());
        assertEquals(42, reviewSuggestion.getLineNumber());
        assertEquals(ReviewSuggestion.SuggestionType.SECURITY, reviewSuggestion.getType());
        assertEquals(ReviewSuggestion.Severity.HIGH, reviewSuggestion.getSeverity());
        assertEquals("Security issue found", reviewSuggestion.getDescription());
        assertEquals("Fix the security vulnerability", reviewSuggestion.getSuggestion());
        assertEquals("public void vulnerableMethod() { }", reviewSuggestion.getCodeSnippet());
        assertEquals("public void secureMethod() { }", reviewSuggestion.getProposedCode());
        assertEquals("SEC-001", reviewSuggestion.getRuleId());
    }
    
    @Test
    void shouldValidateSuggestionTypes() {
        ReviewSuggestion.SuggestionType[] expectedTypes = {
            ReviewSuggestion.SuggestionType.SECURITY,
            ReviewSuggestion.SuggestionType.PERFORMANCE,
            ReviewSuggestion.SuggestionType.BUG,
            ReviewSuggestion.SuggestionType.MAINTAINABILITY,
            ReviewSuggestion.SuggestionType.DOCUMENTATION,
            ReviewSuggestion.SuggestionType.BEST_PRACTICE,
            ReviewSuggestion.SuggestionType.ARCHITECTURE,
            ReviewSuggestion.SuggestionType.DESIGN_PATTERN,
            ReviewSuggestion.SuggestionType.SOLID_PRINCIPLES,
            ReviewSuggestion.SuggestionType.DEPENDENCY_INJECTION,
            ReviewSuggestion.SuggestionType.SEPARATION_OF_CONCERNS,
            ReviewSuggestion.SuggestionType.CODE_STYLE
        };
        
        assertEquals(12, ReviewSuggestion.SuggestionType.values().length);
        assertArrayEquals(expectedTypes, ReviewSuggestion.SuggestionType.values());
    }
    
    @Test
    void shouldValidateSeverityLevels() {
        ReviewSuggestion.Severity[] expectedSeverities = {
            ReviewSuggestion.Severity.CRITICAL,
            ReviewSuggestion.Severity.HIGH,
            ReviewSuggestion.Severity.MEDIUM,
            ReviewSuggestion.Severity.LOW,
            ReviewSuggestion.Severity.INFO
        };
        
        assertEquals(5, ReviewSuggestion.Severity.values().length);
        assertArrayEquals(expectedSeverities, ReviewSuggestion.Severity.values());
    }
    
    @Test
    void shouldValidateSuggestionStatuses() {
        ReviewSuggestion.SuggestionStatus[] expectedStatuses = {
            ReviewSuggestion.SuggestionStatus.PENDING,
            ReviewSuggestion.SuggestionStatus.APPROVED,
            ReviewSuggestion.SuggestionStatus.REJECTED,
            ReviewSuggestion.SuggestionStatus.APPLIED
        };
        
        assertEquals(4, ReviewSuggestion.SuggestionStatus.values().length);
        assertArrayEquals(expectedStatuses, ReviewSuggestion.SuggestionStatus.values());
    }
    
    @Test
    void shouldHandleApprovalWorkflow() {
        LocalDateTime beforeApproval = LocalDateTime.now();
        
        reviewSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
        reviewSuggestion.setApprovedBy("test-user");
        reviewSuggestion.preUpdate();
        
        assertEquals(ReviewSuggestion.SuggestionStatus.APPROVED, reviewSuggestion.getStatus());
        assertEquals("test-user", reviewSuggestion.getApprovedBy());
        assertNotNull(reviewSuggestion.getApprovedAt());
        assertTrue(reviewSuggestion.getApprovedAt().isAfter(beforeApproval) || 
                   reviewSuggestion.getApprovedAt().isEqual(beforeApproval));
    }
    
    @Test
    void shouldHandleApplicationWorkflow() {
        LocalDateTime beforeApplication = LocalDateTime.now();
        
        reviewSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPLIED);
        reviewSuggestion.preUpdate();
        
        assertEquals(ReviewSuggestion.SuggestionStatus.APPLIED, reviewSuggestion.getStatus());
        assertNotNull(reviewSuggestion.getAppliedAt());
        assertTrue(reviewSuggestion.getAppliedAt().isAfter(beforeApplication) || 
                   reviewSuggestion.getAppliedAt().isEqual(beforeApplication));
    }
    
    @Test
    void shouldNotSetTimestampsIfAlreadySet() {
        LocalDateTime fixedTime = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        
        reviewSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
        reviewSuggestion.setApprovedAt(fixedTime);
        reviewSuggestion.preUpdate();
        
        assertEquals(fixedTime, reviewSuggestion.getApprovedAt());
    }
    
    @Test
    void shouldCreateCompleteReviewSuggestion() {
        LocalDateTime now = LocalDateTime.now();
        
        ReviewSuggestion suggestion = new ReviewSuggestion(
            1L,
            pullRequest,
            "TestFile.java",
            42,
            ReviewSuggestion.SuggestionType.PERFORMANCE,
            ReviewSuggestion.Severity.MEDIUM,
            "Performance issue",
            "Optimize this code",
            "slow code here",
            "fast code here",
            "PERF-001",
            ReviewSuggestion.SuggestionStatus.PENDING,
            now,
            null,
            null,
            null
        );
        
        assertEquals(1L, suggestion.getId());
        assertEquals(pullRequest, suggestion.getPullRequest());
        assertEquals("TestFile.java", suggestion.getFileName());
        assertEquals(42, suggestion.getLineNumber());
        assertEquals(ReviewSuggestion.SuggestionType.PERFORMANCE, suggestion.getType());
        assertEquals(ReviewSuggestion.Severity.MEDIUM, suggestion.getSeverity());
        assertEquals("Performance issue", suggestion.getDescription());
        assertEquals("Optimize this code", suggestion.getSuggestion());
        assertEquals("slow code here", suggestion.getCodeSnippet());
        assertEquals("fast code here", suggestion.getProposedCode());
        assertEquals("PERF-001", suggestion.getRuleId());
        assertEquals(ReviewSuggestion.SuggestionStatus.PENDING, suggestion.getStatus());
        assertEquals(now, suggestion.getCreatedAt());
    }
}
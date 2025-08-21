package com.reviewcode.ai.repository;

import com.reviewcode.ai.model.PullRequest;
import com.reviewcode.ai.model.ReviewSuggestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ReviewSuggestionRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private ReviewSuggestionRepository reviewSuggestionRepository;
    
    private PullRequest testPullRequest;
    private ReviewSuggestion suggestion1;
    private ReviewSuggestion suggestion2;
    private ReviewSuggestion suggestion3;
    
    @BeforeEach
    void setUp() {
        testPullRequest = new PullRequest();
        testPullRequest.setTitle("Test PR");
        testPullRequest.setAuthor("test-author");
        testPullRequest.setRepositoryUrl("https://github.com/test/repo");
        testPullRequest.setSourceBranch("feature/test");
        testPullRequest.setTargetBranch("main");
        testPullRequest.setStatus(PullRequest.PullRequestStatus.OPEN);
        testPullRequest.setReviewStatus(PullRequest.ReviewStatus.SUGGESTIONS_PENDING);
        testPullRequest = entityManager.persistAndFlush(testPullRequest);
        
        suggestion1 = createSuggestion(
            "File1.java", 10, 
            ReviewSuggestion.SuggestionType.SECURITY, 
            ReviewSuggestion.Severity.HIGH,
            ReviewSuggestion.SuggestionStatus.PENDING
        );
        
        suggestion2 = createSuggestion(
            "File2.java", 20,
            ReviewSuggestion.SuggestionType.PERFORMANCE,
            ReviewSuggestion.Severity.MEDIUM,
            ReviewSuggestion.SuggestionStatus.APPROVED
        );
        
        suggestion3 = createSuggestion(
            "File1.java", 30,
            ReviewSuggestion.SuggestionType.BUG,
            ReviewSuggestion.Severity.CRITICAL,
            ReviewSuggestion.SuggestionStatus.APPLIED
        );
        
        entityManager.persistAndFlush(suggestion1);
        entityManager.persistAndFlush(suggestion2);
        entityManager.persistAndFlush(suggestion3);
    }
    
    private ReviewSuggestion createSuggestion(String fileName, Integer lineNumber,
                                            ReviewSuggestion.SuggestionType type,
                                            ReviewSuggestion.Severity severity,
                                            ReviewSuggestion.SuggestionStatus status) {
        ReviewSuggestion suggestion = new ReviewSuggestion();
        suggestion.setPullRequest(testPullRequest);
        suggestion.setFileName(fileName);
        suggestion.setLineNumber(lineNumber);
        suggestion.setType(type);
        suggestion.setSeverity(severity);
        suggestion.setDescription("Test description");
        suggestion.setSuggestion("Test suggestion");
        suggestion.setStatus(status);
        suggestion.setCreatedAt(LocalDateTime.now());
        return suggestion;
    }
    
    @Test
    void shouldFindByPullRequestId() {
        List<ReviewSuggestion> suggestions = reviewSuggestionRepository.findByPullRequestId(testPullRequest.getId());
        
        assertEquals(3, suggestions.size());
        assertTrue(suggestions.contains(suggestion1));
        assertTrue(suggestions.contains(suggestion2));
        assertTrue(suggestions.contains(suggestion3));
    }
    
    @Test
    void shouldFindByPullRequestIdAndStatus() {
        List<ReviewSuggestion> pendingSuggestions = reviewSuggestionRepository
            .findByPullRequestIdAndStatus(testPullRequest.getId(), ReviewSuggestion.SuggestionStatus.PENDING);
        
        assertEquals(1, pendingSuggestions.size());
        assertEquals(suggestion1, pendingSuggestions.get(0));
        
        List<ReviewSuggestion> approvedSuggestions = reviewSuggestionRepository
            .findByPullRequestIdAndStatus(testPullRequest.getId(), ReviewSuggestion.SuggestionStatus.APPROVED);
        
        assertEquals(1, approvedSuggestions.size());
        assertEquals(suggestion2, approvedSuggestions.get(0));
    }
    
    @Test
    void shouldFindByPullRequestIdAndStatusIn() {
        List<ReviewSuggestion.SuggestionStatus> statuses = Arrays.asList(
            ReviewSuggestion.SuggestionStatus.PENDING,
            ReviewSuggestion.SuggestionStatus.APPROVED
        );
        
        List<ReviewSuggestion> suggestions = reviewSuggestionRepository
            .findByPullRequestIdAndStatusIn(testPullRequest.getId(), statuses);
        
        assertEquals(2, suggestions.size());
        assertTrue(suggestions.contains(suggestion1));
        assertTrue(suggestions.contains(suggestion2));
        assertFalse(suggestions.contains(suggestion3));
    }
    
    @Test
    void shouldFindByPullRequestIdAndSeverity() {
        List<ReviewSuggestion> highSeveritySuggestions = reviewSuggestionRepository
            .findByPullRequestIdAndSeverity(testPullRequest.getId(), ReviewSuggestion.Severity.HIGH);
        
        assertEquals(1, highSeveritySuggestions.size());
        assertEquals(suggestion1, highSeveritySuggestions.get(0));
        
        List<ReviewSuggestion> criticalSeveritySuggestions = reviewSuggestionRepository
            .findByPullRequestIdAndSeverity(testPullRequest.getId(), ReviewSuggestion.Severity.CRITICAL);
        
        assertEquals(1, criticalSeveritySuggestions.size());
        assertEquals(suggestion3, criticalSeveritySuggestions.get(0));
    }
    
    @Test
    void shouldFindByPullRequestIdAndType() {
        List<ReviewSuggestion> securitySuggestions = reviewSuggestionRepository
            .findByPullRequestIdAndType(testPullRequest.getId(), ReviewSuggestion.SuggestionType.SECURITY);
        
        assertEquals(1, securitySuggestions.size());
        assertEquals(suggestion1, securitySuggestions.get(0));
        
        List<ReviewSuggestion> performanceSuggestions = reviewSuggestionRepository
            .findByPullRequestIdAndType(testPullRequest.getId(), ReviewSuggestion.SuggestionType.PERFORMANCE);
        
        assertEquals(1, performanceSuggestions.size());
        assertEquals(suggestion2, performanceSuggestions.get(0));
    }
    
    @Test
    void shouldCountByPullRequestIdAndStatus() {
        long pendingCount = reviewSuggestionRepository
            .countByPullRequestIdAndStatus(testPullRequest.getId(), ReviewSuggestion.SuggestionStatus.PENDING);
        assertEquals(1, pendingCount);
        
        long approvedCount = reviewSuggestionRepository
            .countByPullRequestIdAndStatus(testPullRequest.getId(), ReviewSuggestion.SuggestionStatus.APPROVED);
        assertEquals(1, approvedCount);
        
        long appliedCount = reviewSuggestionRepository
            .countByPullRequestIdAndStatus(testPullRequest.getId(), ReviewSuggestion.SuggestionStatus.APPLIED);
        assertEquals(1, appliedCount);
        
        long rejectedCount = reviewSuggestionRepository
            .countByPullRequestIdAndStatus(testPullRequest.getId(), ReviewSuggestion.SuggestionStatus.REJECTED);
        assertEquals(0, rejectedCount);
    }
    
    @Test
    void shouldDeleteByPullRequestId() {
        List<ReviewSuggestion> beforeDelete = reviewSuggestionRepository.findByPullRequestId(testPullRequest.getId());
        assertEquals(3, beforeDelete.size());
        
        reviewSuggestionRepository.deleteByPullRequestId(testPullRequest.getId());
        entityManager.flush();
        
        List<ReviewSuggestion> afterDelete = reviewSuggestionRepository.findByPullRequestId(testPullRequest.getId());
        assertEquals(0, afterDelete.size());
    }
    
    @Test
    void shouldReturnEmptyListForNonExistentPullRequest() {
        List<ReviewSuggestion> suggestions = reviewSuggestionRepository.findByPullRequestId(999L);
        assertTrue(suggestions.isEmpty());
    }
    
    @Test
    void shouldSaveAndRetrieveSuggestion() {
        ReviewSuggestion newSuggestion = new ReviewSuggestion();
        newSuggestion.setPullRequest(testPullRequest);
        newSuggestion.setFileName("NewFile.java");
        newSuggestion.setLineNumber(100);
        newSuggestion.setType(ReviewSuggestion.SuggestionType.CODE_STYLE);
        newSuggestion.setSeverity(ReviewSuggestion.Severity.LOW);
        newSuggestion.setDescription("Code style issue");
        newSuggestion.setSuggestion("Fix formatting");
        newSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.PENDING);
        newSuggestion.setCreatedAt(LocalDateTime.now());
        
        ReviewSuggestion saved = reviewSuggestionRepository.save(newSuggestion);
        assertNotNull(saved.getId());
        
        ReviewSuggestion retrieved = reviewSuggestionRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals("NewFile.java", retrieved.getFileName());
        assertEquals(100, retrieved.getLineNumber());
        assertEquals(ReviewSuggestion.SuggestionType.CODE_STYLE, retrieved.getType());
        assertEquals(ReviewSuggestion.Severity.LOW, retrieved.getSeverity());
    }
}
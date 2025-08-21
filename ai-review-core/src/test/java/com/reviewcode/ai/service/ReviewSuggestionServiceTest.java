package com.reviewcode.ai.service;

import com.reviewcode.ai.model.PullRequest;
import com.reviewcode.ai.model.ReviewSuggestion;
import com.reviewcode.ai.repository.PullRequestRepository;
import com.reviewcode.ai.repository.ReviewSuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewSuggestionServiceTest {
    
    @Mock
    private ReviewSuggestionRepository reviewSuggestionRepository;
    
    @Mock
    private PullRequestRepository pullRequestRepository;
    
    @InjectMocks
    private ReviewSuggestionService reviewSuggestionService;
    
    private PullRequest testPullRequest;
    private ReviewSuggestion testSuggestion;
    
    @BeforeEach
    void setUp() {
        testPullRequest = new PullRequest();
        testPullRequest.setId(1L);
        testPullRequest.setTitle("Test PR");
        testPullRequest.setAuthor("test-author");
        testPullRequest.setRepositoryUrl("https://github.com/test/repo");
        testPullRequest.setSourceBranch("feature/test");
        testPullRequest.setTargetBranch("main");
        testPullRequest.setStatus(PullRequest.PullRequestStatus.OPEN);
        testPullRequest.setReviewStatus(PullRequest.ReviewStatus.SUGGESTIONS_PENDING);
        
        testSuggestion = new ReviewSuggestion();
        testSuggestion.setId(1L);
        testSuggestion.setPullRequest(testPullRequest);
        testSuggestion.setFileName("TestFile.java");
        testSuggestion.setLineNumber(42);
        testSuggestion.setType(ReviewSuggestion.SuggestionType.SECURITY);
        testSuggestion.setSeverity(ReviewSuggestion.Severity.HIGH);
        testSuggestion.setDescription("Security issue");
        testSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.PENDING);
        testSuggestion.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void shouldGetAllSuggestions() {
        List<ReviewSuggestion> expectedSuggestions = Arrays.asList(testSuggestion);
        when(reviewSuggestionRepository.findByPullRequestId(1L)).thenReturn(expectedSuggestions);
        
        List<ReviewSuggestion> result = reviewSuggestionService.getAllSuggestions(1L);
        
        assertEquals(1, result.size());
        assertEquals(testSuggestion, result.get(0));
        verify(reviewSuggestionRepository).findByPullRequestId(1L);
    }
    
    @Test
    void shouldGetSuggestionsByStatus() {
        List<ReviewSuggestion> expectedSuggestions = Arrays.asList(testSuggestion);
        when(reviewSuggestionRepository.findByPullRequestIdAndStatus(1L, ReviewSuggestion.SuggestionStatus.PENDING))
            .thenReturn(expectedSuggestions);
        
        List<ReviewSuggestion> result = reviewSuggestionService.getSuggestionsByStatus(1L, ReviewSuggestion.SuggestionStatus.PENDING);
        
        assertEquals(1, result.size());
        assertEquals(testSuggestion, result.get(0));
    }
    
    @Test
    void shouldGetSuggestionsBySeverity() {
        List<ReviewSuggestion> expectedSuggestions = Arrays.asList(testSuggestion);
        when(reviewSuggestionRepository.findByPullRequestIdAndSeverity(1L, ReviewSuggestion.Severity.HIGH))
            .thenReturn(expectedSuggestions);
        
        List<ReviewSuggestion> result = reviewSuggestionService.getSuggestionsBySeverity(1L, ReviewSuggestion.Severity.HIGH);
        
        assertEquals(1, result.size());
        assertEquals(testSuggestion, result.get(0));
    }
    
    @Test
    void shouldGetSuggestionsByType() {
        List<ReviewSuggestion> expectedSuggestions = Arrays.asList(testSuggestion);
        when(reviewSuggestionRepository.findByPullRequestIdAndType(1L, ReviewSuggestion.SuggestionType.SECURITY))
            .thenReturn(expectedSuggestions);
        
        List<ReviewSuggestion> result = reviewSuggestionService.getSuggestionsByType(1L, ReviewSuggestion.SuggestionType.SECURITY);
        
        assertEquals(1, result.size());
        assertEquals(testSuggestion, result.get(0));
    }
    
    @Test
    void shouldGetSingleSuggestion() {
        when(reviewSuggestionRepository.findById(1L)).thenReturn(Optional.of(testSuggestion));
        
        Optional<ReviewSuggestion> result = reviewSuggestionService.getSuggestion(1L, 1L);
        
        assertTrue(result.isPresent());
        assertEquals(testSuggestion, result.get());
    }
    
    @Test
    void shouldReturnEmptyWhenSuggestionNotFound() {
        when(reviewSuggestionRepository.findById(1L)).thenReturn(Optional.empty());
        
        Optional<ReviewSuggestion> result = reviewSuggestionService.getSuggestion(1L, 1L);
        
        assertFalse(result.isPresent());
    }
    
    @Test
    void shouldReturnEmptyWhenSuggestionBelongsToDifferentPR() {
        PullRequest otherPR = new PullRequest();
        otherPR.setId(2L);
        testSuggestion.setPullRequest(otherPR);
        
        when(reviewSuggestionRepository.findById(1L)).thenReturn(Optional.of(testSuggestion));
        
        Optional<ReviewSuggestion> result = reviewSuggestionService.getSuggestion(1L, 1L);
        
        assertFalse(result.isPresent());
    }
    
    @Test
    void shouldApproveSuggestion() {
        when(reviewSuggestionRepository.findById(1L)).thenReturn(Optional.of(testSuggestion));
        when(reviewSuggestionRepository.save(any(ReviewSuggestion.class))).thenReturn(testSuggestion);
        
        ReviewSuggestion result = reviewSuggestionService.approveSuggestion(1L, 1L, "test-user");
        
        assertEquals(ReviewSuggestion.SuggestionStatus.APPROVED, result.getStatus());
        assertEquals("test-user", result.getApprovedBy());
        assertNotNull(result.getApprovedAt());
        verify(reviewSuggestionRepository).save(testSuggestion);
    }
    
    @Test
    void shouldThrowExceptionWhenApprovingNonPendingSuggestion() {
        testSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
        when(reviewSuggestionRepository.findById(1L)).thenReturn(Optional.of(testSuggestion));
        
        assertThrows(IllegalArgumentException.class, 
            () -> reviewSuggestionService.approveSuggestion(1L, 1L, "test-user"));
    }
    
    @Test
    void shouldRejectSuggestion() {
        when(reviewSuggestionRepository.findById(1L)).thenReturn(Optional.of(testSuggestion));
        when(reviewSuggestionRepository.save(any(ReviewSuggestion.class))).thenReturn(testSuggestion);
        
        ReviewSuggestion result = reviewSuggestionService.rejectSuggestion(1L, 1L);
        
        assertEquals(ReviewSuggestion.SuggestionStatus.REJECTED, result.getStatus());
        verify(reviewSuggestionRepository).save(testSuggestion);
    }
    
    @Test
    void shouldBulkApproveSuggestions() {
        ReviewSuggestion suggestion2 = new ReviewSuggestion();
        suggestion2.setId(2L);
        suggestion2.setPullRequest(testPullRequest);
        suggestion2.setStatus(ReviewSuggestion.SuggestionStatus.PENDING);
        
        List<Long> suggestionIds = Arrays.asList(1L, 2L);
        List<ReviewSuggestion> suggestions = Arrays.asList(testSuggestion, suggestion2);
        
        when(reviewSuggestionRepository.findAllById(suggestionIds)).thenReturn(suggestions);
        when(reviewSuggestionRepository.saveAll(anyList())).thenReturn(suggestions);
        
        List<ReviewSuggestion> result = reviewSuggestionService.bulkApproveSuggestions(1L, suggestionIds, "test-user");
        
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(s -> s.getStatus() == ReviewSuggestion.SuggestionStatus.APPROVED));
        assertTrue(result.stream().allMatch(s -> "test-user".equals(s.getApprovedBy())));
        assertTrue(result.stream().allMatch(s -> s.getApprovedAt() != null));
    }
    
    @Test
    void shouldThrowExceptionWhenBulkApprovingWithInvalidSuggestions() {
        testSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
        List<Long> suggestionIds = Arrays.asList(1L);
        List<ReviewSuggestion> suggestions = Arrays.asList(testSuggestion);
        
        when(reviewSuggestionRepository.findAllById(suggestionIds)).thenReturn(suggestions);
        
        assertThrows(IllegalArgumentException.class, 
            () -> reviewSuggestionService.bulkApproveSuggestions(1L, suggestionIds, "test-user"));
    }
    
    @Test
    void shouldApplyApprovedSuggestions() {
        testSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
        testSuggestion.setProposedCode("fixed code");
        
        when(reviewSuggestionRepository.findByPullRequestIdAndStatus(1L, ReviewSuggestion.SuggestionStatus.APPROVED))
            .thenReturn(Arrays.asList(testSuggestion));
        when(pullRequestRepository.findById(1L)).thenReturn(Optional.of(testPullRequest));
        when(reviewSuggestionRepository.countByPullRequestIdAndStatus(1L, ReviewSuggestion.SuggestionStatus.PENDING))
            .thenReturn(0L);
        
        Mono<ReviewSuggestionService.ApplicationResult> resultMono = 
            reviewSuggestionService.applyApprovedSuggestions(1L);
        
        StepVerifier.create(resultMono)
            .assertNext(result -> {
                assertEquals(1, result.getAppliedCount());
                assertEquals(0, result.getFailedCount());
                assertTrue(result.getErrors().isEmpty());
            })
            .verifyComplete();
            
        verify(reviewSuggestionRepository).save(testSuggestion);
        verify(pullRequestRepository).save(testPullRequest);
    }
    
    @Test
    void shouldReturnEmptyResultWhenNoApprovedSuggestions() {
        when(reviewSuggestionRepository.findByPullRequestIdAndStatus(1L, ReviewSuggestion.SuggestionStatus.APPROVED))
            .thenReturn(Collections.emptyList());
        
        Mono<ReviewSuggestionService.ApplicationResult> resultMono = 
            reviewSuggestionService.applyApprovedSuggestions(1L);
        
        StepVerifier.create(resultMono)
            .assertNext(result -> {
                assertEquals(0, result.getAppliedCount());
                assertEquals(0, result.getFailedCount());
                assertTrue(result.getErrors().isEmpty());
            })
            .verifyComplete();
    }
    
    @Test
    void shouldGenerateSuggestionsSummary() {
        ReviewSuggestion approvedSuggestion = new ReviewSuggestion();
        approvedSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
        approvedSuggestion.setSeverity(ReviewSuggestion.Severity.HIGH);
        approvedSuggestion.setType(ReviewSuggestion.SuggestionType.SECURITY);
        
        List<ReviewSuggestion> suggestions = Arrays.asList(testSuggestion, approvedSuggestion);
        when(reviewSuggestionRepository.findByPullRequestId(1L)).thenReturn(suggestions);
        
        Map<String, Object> summary = reviewSuggestionService.getSuggestionsSummary(1L);
        
        assertEquals(2, summary.get("total"));
        assertEquals(1L, summary.get("pendingCount"));
        assertEquals(1L, summary.get("approvedCount"));
        assertEquals(0L, summary.get("appliedCount"));
        assertEquals(0L, summary.get("rejectedCount"));
        
        @SuppressWarnings("unchecked")
        Map<ReviewSuggestion.SuggestionStatus, Long> statusCounts = 
            (Map<ReviewSuggestion.SuggestionStatus, Long>) summary.get("statusCounts");
        assertEquals(1L, statusCounts.get(ReviewSuggestion.SuggestionStatus.PENDING));
        assertEquals(1L, statusCounts.get(ReviewSuggestion.SuggestionStatus.APPROVED));
    }
    
    @Test
    void shouldCreateSuggestionsFromAiResponse() {
        AiReviewResponse.Finding finding = new AiReviewResponse.Finding();
        finding.setFileName("TestFile.java");
        finding.setLineNumber(42);
        finding.setType("SECURITY");
        finding.setSeverity("HIGH");
        finding.setDescription("Security vulnerability");
        finding.setSuggestion("Fix the vulnerability");
        finding.setCodeSnippet("vulnerable code");
        finding.setProposedCode("secure code");
        finding.setRuleId("SEC-001");
        
        AiReviewResponse response = new AiReviewResponse();
        response.setFindings(Arrays.asList(finding));
        
        when(reviewSuggestionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        List<ReviewSuggestion> result = reviewSuggestionService.createSuggestionsFromAiResponse(testPullRequest, response);
        
        assertEquals(1, result.size());
        ReviewSuggestion suggestion = result.get(0);
        assertEquals("TestFile.java", suggestion.getFileName());
        assertEquals(42, suggestion.getLineNumber());
        assertEquals(ReviewSuggestion.SuggestionType.SECURITY, suggestion.getType());
        assertEquals(ReviewSuggestion.Severity.HIGH, suggestion.getSeverity());
        assertEquals("Security vulnerability", suggestion.getDescription());
        assertEquals("Fix the vulnerability", suggestion.getSuggestion());
        assertEquals("vulnerable code", suggestion.getCodeSnippet());
        assertEquals("secure code", suggestion.getProposedCode());
        assertEquals("SEC-001", suggestion.getRuleId());
        assertEquals(ReviewSuggestion.SuggestionStatus.PENDING, suggestion.getStatus());
    }
}
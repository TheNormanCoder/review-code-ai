package com.reviewcode.ai.service;

import com.reviewcode.ai.model.*;
import com.reviewcode.ai.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private ReviewMetricsRepository metricsRepository;

    @Mock
    private PullRequestRepository pullRequestRepository;

    @Mock
    private CodeReviewRepository codeReviewRepository;

    @InjectMocks
    private MetricsService metricsService;

    private PullRequest testPullRequest;
    private CodeReview testCodeReview;
    private ReviewFinding testFinding;

    @BeforeEach
    void setUp() {
        testPullRequest = new PullRequest();
        testPullRequest.setId(1L);
        testPullRequest.setTitle("Test PR");
        testPullRequest.setAuthor("testuser");
        testPullRequest.setRepositoryUrl("https://github.com/test/repo");
        testPullRequest.setSourceBranch("feature/test");
        testPullRequest.setTargetBranch("main");
        testPullRequest.setStatus(PullRequest.PullRequestStatus.MERGED);
        testPullRequest.setReviewStatus(PullRequest.ReviewStatus.APPROVED);
        testPullRequest.setCreatedAt(LocalDateTime.now().minusDays(1));
        testPullRequest.setUpdatedAt(LocalDateTime.now());

        testCodeReview = new CodeReview();
        testCodeReview.setId(1L);
        testCodeReview.setPullRequest(testPullRequest);
        testCodeReview.setReviewer("AI-MCP");
        testCodeReview.setReviewerType(CodeReview.ReviewerType.AI_MCP);
        testCodeReview.setDecision(CodeReview.ReviewDecision.APPROVED);
        testCodeReview.setCreatedAt(LocalDateTime.now().minusDays(1));

        testFinding = new ReviewFinding();
        testFinding.setId(1L);
        testFinding.setCodeReview(testCodeReview);
        testFinding.setFileName("TestClass.java");
        testFinding.setLineNumber(42);
        testFinding.setType(ReviewFinding.FindingType.SOLID_PRINCIPLES);
        testFinding.setSeverity(ReviewFinding.Severity.MEDIUM);
        testFinding.setDescription("SRP violation detected");

        testCodeReview.setFindings(List.of(testFinding));
    }

    @Test
    void shouldGenerateMetricsForPeriod() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        when(pullRequestRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(List.of(testPullRequest));
        when(codeReviewRepository.findByPullRequestId(1L))
            .thenReturn(List.of(testCodeReview));
        when(metricsRepository.save(any(ReviewMetrics.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ReviewMetrics result = metricsService.generateMetricsForPeriod(startDate, endDate, "WEEKLY");

        // Then
        assertNotNull(result);
        assertEquals("WEEKLY", result.getPeriod());
        assertEquals(startDate, result.getReportDate());
        assertEquals(1, result.getTotalPullRequests());
        assertEquals(0, result.getOpenPullRequests());
        assertEquals(0, result.getClosedPullRequests());
        assertEquals(1, result.getMergedPullRequests());
        assertEquals(1, result.getTotalReviews());
        assertEquals(1, result.getAiReviews());
        assertEquals(0, result.getHumanReviews());
        assertEquals(1, result.getApprovedReviews());

        verify(metricsRepository).save(any(ReviewMetrics.class));
    }

    @Test
    void shouldCalculatePullRequestMetricsCorrectly() {
        // Given
        PullRequest openPr = createPullRequest(PullRequest.PullRequestStatus.OPEN, PullRequest.ReviewStatus.PENDING);
        PullRequest closedPr = createPullRequest(PullRequest.PullRequestStatus.CLOSED, PullRequest.ReviewStatus.REJECTED);
        PullRequest mergedPr = createPullRequest(PullRequest.PullRequestStatus.MERGED, PullRequest.ReviewStatus.APPROVED);
        
        List<PullRequest> pullRequests = List.of(openPr, closedPr, mergedPr);
        
        when(pullRequestRepository.findByCreatedAtBetween(any(), any())).thenReturn(pullRequests);
        when(codeReviewRepository.findByPullRequestId(any())).thenReturn(List.of());
        when(metricsRepository.save(any(ReviewMetrics.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ReviewMetrics result = metricsService.generateMetricsForPeriod(
            LocalDateTime.now().minusDays(7), LocalDateTime.now(), "WEEKLY");

        // Then
        assertEquals(3, result.getTotalPullRequests());
        assertEquals(1, result.getOpenPullRequests());
        assertEquals(1, result.getClosedPullRequests());
        assertEquals(1, result.getMergedPullRequests());
    }

    @Test
    void shouldCalculateReviewMetricsCorrectly() {
        // Given
        CodeReview aiReview = createCodeReview(CodeReview.ReviewerType.AI_MCP, CodeReview.ReviewDecision.APPROVED);
        CodeReview humanReview1 = createCodeReview(CodeReview.ReviewerType.HUMAN, CodeReview.ReviewDecision.CHANGES_REQUESTED);
        CodeReview humanReview2 = createCodeReview(CodeReview.ReviewerType.HUMAN, CodeReview.ReviewDecision.REJECTED);
        
        when(pullRequestRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(List.of(testPullRequest));
        when(codeReviewRepository.findByPullRequestId(1L))
            .thenReturn(List.of(aiReview, humanReview1, humanReview2));
        when(metricsRepository.save(any(ReviewMetrics.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ReviewMetrics result = metricsService.generateMetricsForPeriod(
            LocalDateTime.now().minusDays(7), LocalDateTime.now(), "WEEKLY");

        // Then
        assertEquals(3, result.getTotalReviews());
        assertEquals(1, result.getAiReviews());
        assertEquals(2, result.getHumanReviews());
        assertEquals(1, result.getApprovedReviews());
        assertEquals(1, result.getChangesRequestedReviews());
        assertEquals(1, result.getRejectedReviews());
    }

    @Test
    void shouldCalculateQualityMetricsCorrectly() {
        // Given
        ReviewFinding criticalFinding = createFinding(ReviewFinding.FindingType.SECURITY, ReviewFinding.Severity.CRITICAL);
        ReviewFinding highFinding = createFinding(ReviewFinding.FindingType.BUG, ReviewFinding.Severity.HIGH);
        ReviewFinding mediumFinding = createFinding(ReviewFinding.FindingType.PERFORMANCE, ReviewFinding.Severity.MEDIUM);
        ReviewFinding lowFinding = createFinding(ReviewFinding.FindingType.CODE_STYLE, ReviewFinding.Severity.LOW);
        ReviewFinding infoFinding = createFinding(ReviewFinding.FindingType.DOCUMENTATION, ReviewFinding.Severity.INFO);
        
        CodeReview reviewWithFindings = createCodeReview(CodeReview.ReviewerType.AI_MCP, CodeReview.ReviewDecision.CHANGES_REQUESTED);
        reviewWithFindings.setFindings(List.of(criticalFinding, highFinding, mediumFinding, lowFinding, infoFinding));
        
        when(pullRequestRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(List.of(testPullRequest));
        when(codeReviewRepository.findByPullRequestId(1L))
            .thenReturn(List.of(reviewWithFindings));
        when(metricsRepository.save(any(ReviewMetrics.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ReviewMetrics result = metricsService.generateMetricsForPeriod(
            LocalDateTime.now().minusDays(7), LocalDateTime.now(), "WEEKLY");

        // Then
        assertEquals(5, result.getTotalFindings());
        assertEquals(1, result.getCriticalFindings());
        assertEquals(1, result.getHighFindings());
        assertEquals(1, result.getMediumFindings());
        assertEquals(1, result.getLowFindings());
        assertEquals(1, result.getInfoFindings());
        assertEquals(1, result.getSecurityFindings());
        assertEquals(1, result.getBugFindings());
        assertEquals(1, result.getPerformanceFindings());
    }

    @Test
    void shouldCalculateDeveloperMetricsCorrectly() {
        // Given
        PullRequest pr1 = createPullRequest("author1", PullRequest.ReviewStatus.APPROVED);
        PullRequest pr2 = createPullRequest("author2", PullRequest.ReviewStatus.CHANGES_REQUESTED);
        PullRequest pr3 = createPullRequest("author1", PullRequest.ReviewStatus.APPROVED);
        
        CodeReview humanReview1 = createCodeReview("reviewer1", CodeReview.ReviewerType.HUMAN, CodeReview.ReviewDecision.APPROVED);
        CodeReview humanReview2 = createCodeReview("reviewer2", CodeReview.ReviewerType.HUMAN, CodeReview.ReviewDecision.CHANGES_REQUESTED);
        
        when(pullRequestRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(List.of(pr1, pr2, pr3));
        when(codeReviewRepository.findByPullRequestId(any()))
            .thenReturn(List.of());
        when(codeReviewRepository.findByReviewer("reviewer1"))
            .thenReturn(List.of(humanReview1));
        when(codeReviewRepository.findByReviewer("reviewer2"))
            .thenReturn(List.of(humanReview2));
        when(metricsRepository.save(any(ReviewMetrics.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ReviewMetrics result = metricsService.generateMetricsForPeriod(
            LocalDateTime.now().minusDays(7), LocalDateTime.now(), "WEEKLY");

        // Then
        assertEquals(2, result.getActiveAuthors()); // author1, author2
        assertTrue(result.getReworkRate() > 0); // One PR needed changes
        assertTrue(result.getApprovalRate() >= 0); // Calculated based on reviews
    }

    @Test
    void shouldGetMetricsForPeriod() {
        // Given
        List<ReviewMetrics> mockMetrics = List.of(new ReviewMetrics(), new ReviewMetrics());
        when(metricsRepository.findByPeriodOrderByReportDateDesc("WEEKLY"))
            .thenReturn(mockMetrics);

        // When
        List<ReviewMetrics> result = metricsService.getMetricsForPeriod("WEEKLY", 10);

        // Then
        assertEquals(2, result.size());
        verify(metricsRepository).findByPeriodOrderByReportDateDesc("WEEKLY");
    }

    @Test
    void shouldGetMetricsSince() {
        // Given
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(30);
        List<ReviewMetrics> mockMetrics = List.of(new ReviewMetrics());
        when(metricsRepository.findMetricsSince(sinceDate)).thenReturn(mockMetrics);

        // When
        List<ReviewMetrics> result = metricsService.getMetricsSince(sinceDate);

        // Then
        assertEquals(1, result.size());
        verify(metricsRepository).findMetricsSince(sinceDate);
    }

    private PullRequest createPullRequest(PullRequest.PullRequestStatus status, PullRequest.ReviewStatus reviewStatus) {
        PullRequest pr = new PullRequest();
        pr.setId((long) (Math.random() * 1000));
        pr.setTitle("Test PR");
        pr.setAuthor("testuser");
        pr.setRepositoryUrl("https://github.com/test/repo");
        pr.setSourceBranch("feature/test");
        pr.setTargetBranch("main");
        pr.setStatus(status);
        pr.setReviewStatus(reviewStatus);
        pr.setCreatedAt(LocalDateTime.now().minusDays(1));
        pr.setUpdatedAt(LocalDateTime.now());
        return pr;
    }

    private PullRequest createPullRequest(String author, PullRequest.ReviewStatus reviewStatus) {
        PullRequest pr = createPullRequest(PullRequest.PullRequestStatus.MERGED, reviewStatus);
        pr.setAuthor(author);
        return pr;
    }

    private CodeReview createCodeReview(CodeReview.ReviewerType reviewerType, CodeReview.ReviewDecision decision) {
        return createCodeReview("test-reviewer", reviewerType, decision);
    }

    private CodeReview createCodeReview(String reviewer, CodeReview.ReviewerType reviewerType, CodeReview.ReviewDecision decision) {
        CodeReview review = new CodeReview();
        review.setId((long) (Math.random() * 1000));
        review.setPullRequest(testPullRequest);
        review.setReviewer(reviewer);
        review.setReviewerType(reviewerType);
        review.setDecision(decision);
        review.setCreatedAt(LocalDateTime.now().minusDays(1));
        return review;
    }

    private ReviewFinding createFinding(ReviewFinding.FindingType type, ReviewFinding.Severity severity) {
        ReviewFinding finding = new ReviewFinding();
        finding.setId((long) (Math.random() * 1000));
        finding.setCodeReview(testCodeReview);
        finding.setFileName("TestFile.java");
        finding.setType(type);
        finding.setSeverity(severity);
        finding.setDescription("Test finding");
        return finding;
    }
}
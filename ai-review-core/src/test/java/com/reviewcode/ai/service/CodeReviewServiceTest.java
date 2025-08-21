package com.reviewcode.ai.service;

import com.reviewcode.ai.model.CodeReview;
import com.reviewcode.ai.model.PullRequest;
import com.reviewcode.ai.repository.CodeReviewRepository;
import com.reviewcode.ai.repository.PullRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeReviewServiceTest {

    @Mock
    private PullRequestRepository pullRequestRepository;

    @Mock
    private CodeReviewRepository codeReviewRepository;

    @Mock
    private AiReviewService aiReviewService;

    @InjectMocks
    private CodeReviewService codeReviewService;

    private PullRequest testPullRequest;
    private CodeReview testCodeReview;

    @BeforeEach
    void setUp() {
        testPullRequest = new PullRequest();
        testPullRequest.setId(1L);
        testPullRequest.setTitle("Test PR");
        testPullRequest.setAuthor("testuser");
        testPullRequest.setRepositoryUrl("https://github.com/test/repo");
        testPullRequest.setSourceBranch("feature/test");
        testPullRequest.setTargetBranch("main");

        testCodeReview = new CodeReview();
        testCodeReview.setId(1L);
        testCodeReview.setPullRequest(testPullRequest);
        testCodeReview.setReviewer("AI-MCP");
        testCodeReview.setReviewerType(CodeReview.ReviewerType.AI_MCP);
        testCodeReview.setDecision(CodeReview.ReviewDecision.APPROVED);
    }

    @Test
    void shouldCreatePullRequestSuccessfully() {
        // Given
        when(pullRequestRepository.save(any(PullRequest.class))).thenReturn(testPullRequest);

        // When
        PullRequest result = codeReviewService.createPullRequest(testPullRequest);

        // Then
        assertNotNull(result);
        assertEquals("Test PR", result.getTitle());
        assertEquals(PullRequest.PullRequestStatus.OPEN, result.getStatus());
        assertEquals(PullRequest.ReviewStatus.PENDING, result.getReviewStatus());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        verify(pullRequestRepository).save(testPullRequest);
    }

    @Test
    void shouldGetPullRequestById() {
        // Given
        when(pullRequestRepository.findById(1L)).thenReturn(Optional.of(testPullRequest));

        // When
        Optional<PullRequest> result = codeReviewService.getPullRequest(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testPullRequest, result.get());
        verify(pullRequestRepository).findById(1L);
    }

    @Test
    void shouldReturnEmptyWhenPullRequestNotFound() {
        // Given
        when(pullRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<PullRequest> result = codeReviewService.getPullRequest(999L);

        // Then
        assertTrue(result.isEmpty());
        verify(pullRequestRepository).findById(999L);
    }

    @Test
    void shouldGetAllPullRequests() {
        // Given
        List<PullRequest> pullRequests = List.of(testPullRequest);
        when(pullRequestRepository.findAll()).thenReturn(pullRequests);

        // When
        List<PullRequest> result = codeReviewService.getAllPullRequests();

        // Then
        assertEquals(1, result.size());
        assertEquals(testPullRequest, result.get(0));
        verify(pullRequestRepository).findAll();
    }

    @Test
    void shouldGetPullRequestsByAuthor() {
        // Given
        List<PullRequest> pullRequests = List.of(testPullRequest);
        when(pullRequestRepository.findByAuthor("testuser")).thenReturn(pullRequests);

        // When
        List<PullRequest> result = codeReviewService.getPullRequestsByAuthor("testuser");

        // Then
        assertEquals(1, result.size());
        assertEquals(testPullRequest, result.get(0));
        verify(pullRequestRepository).findByAuthor("testuser");
    }

    @Test
    void shouldGetPullRequestsByStatus() {
        // Given
        List<PullRequest> pullRequests = List.of(testPullRequest);
        when(pullRequestRepository.findByStatus(PullRequest.PullRequestStatus.OPEN))
            .thenReturn(pullRequests);

        // When
        List<PullRequest> result = codeReviewService.getPullRequestsByStatus(PullRequest.PullRequestStatus.OPEN);

        // Then
        assertEquals(1, result.size());
        assertEquals(testPullRequest, result.get(0));
        verify(pullRequestRepository).findByStatus(PullRequest.PullRequestStatus.OPEN);
    }

    @Test
    void shouldTriggerAiReviewSuccessfully() {
        // Given
        List<String> filesToReview = List.of("src/main/java/TestClass.java");
        when(pullRequestRepository.findById(1L)).thenReturn(Optional.of(testPullRequest));
        when(pullRequestRepository.save(any(PullRequest.class))).thenReturn(testPullRequest);
        when(aiReviewService.performAiReview(any(PullRequest.class), any(List.class)))
            .thenReturn(Mono.just(testCodeReview));
        when(codeReviewRepository.save(any(CodeReview.class))).thenReturn(testCodeReview);
        when(codeReviewRepository.findByPullRequestId(1L)).thenReturn(List.of(testCodeReview));

        // When
        Mono<CodeReview> result = codeReviewService.triggerAiReview(1L, filesToReview);

        // Then
        StepVerifier.create(result)
            .expectNext(testCodeReview)
            .verifyComplete();

        verify(pullRequestRepository).findById(1L);
        verify(pullRequestRepository, times(2)).save(any(PullRequest.class));
        verify(aiReviewService).performAiReview(testPullRequest, filesToReview);
        verify(codeReviewRepository).save(testCodeReview);
    }

    @Test
    void shouldFailWhenPullRequestNotFoundForAiReview() {
        // Given
        List<String> filesToReview = List.of("src/main/java/TestClass.java");
        when(pullRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Mono<CodeReview> result = codeReviewService.triggerAiReview(999L, filesToReview);

        // Then
        StepVerifier.create(result)
            .expectError(IllegalArgumentException.class)
            .verify();

        verify(pullRequestRepository).findById(999L);
        verify(aiReviewService, never()).performAiReview(any(), any());
    }

    @Test
    void shouldAddHumanReviewSuccessfully() {
        // Given
        CodeReview humanReview = new CodeReview();
        humanReview.setReviewer("human-reviewer");
        humanReview.setDecision(CodeReview.ReviewDecision.APPROVED);
        humanReview.setSummary("Looks good to me!");

        when(pullRequestRepository.findById(1L)).thenReturn(Optional.of(testPullRequest));
        when(codeReviewRepository.save(any(CodeReview.class))).thenReturn(humanReview);
        when(codeReviewRepository.findByPullRequestId(1L)).thenReturn(List.of(humanReview));
        when(pullRequestRepository.save(any(PullRequest.class))).thenReturn(testPullRequest);

        // When
        CodeReview result = codeReviewService.addHumanReview(1L, humanReview);

        // Then
        assertNotNull(result);
        assertEquals(testPullRequest, result.getPullRequest());
        assertEquals(CodeReview.ReviewerType.HUMAN, result.getReviewerType());
        assertNotNull(result.getCreatedAt());

        verify(pullRequestRepository).findById(1L);
        verify(codeReviewRepository).save(humanReview);
        verify(pullRequestRepository).save(testPullRequest);
    }

    @Test
    void shouldThrowExceptionWhenPullRequestNotFoundForHumanReview() {
        // Given
        CodeReview humanReview = new CodeReview();
        when(pullRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            codeReviewService.addHumanReview(999L, humanReview);
        });

        verify(pullRequestRepository).findById(999L);
        verify(codeReviewRepository, never()).save(any());
    }

    @Test
    void shouldGetReviewsForPullRequest() {
        // Given
        List<CodeReview> reviews = List.of(testCodeReview);
        when(codeReviewRepository.findByPullRequestId(1L)).thenReturn(reviews);

        // When
        List<CodeReview> result = codeReviewService.getReviewsForPullRequest(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals(testCodeReview, result.get(0));
        verify(codeReviewRepository).findByPullRequestId(1L);
    }

    @Test
    void shouldUpdatePullRequestStatusAfterApproval() {
        // Given
        CodeReview approvedReview = new CodeReview();
        approvedReview.setPullRequest(testPullRequest);
        approvedReview.setDecision(CodeReview.ReviewDecision.APPROVED);

        when(pullRequestRepository.findById(1L)).thenReturn(Optional.of(testPullRequest));
        when(codeReviewRepository.save(any(CodeReview.class))).thenReturn(approvedReview);
        when(codeReviewRepository.findByPullRequestId(1L)).thenReturn(List.of(approvedReview));
        when(pullRequestRepository.save(any(PullRequest.class))).thenReturn(testPullRequest);

        // When
        codeReviewService.addHumanReview(1L, approvedReview);

        // Then
        verify(pullRequestRepository).save(argThat(pr -> 
            pr.getReviewStatus() == PullRequest.ReviewStatus.APPROVED));
    }

    @Test
    void shouldUpdatePullRequestStatusAfterRejection() {
        // Given
        CodeReview rejectedReview = new CodeReview();
        rejectedReview.setPullRequest(testPullRequest);
        rejectedReview.setDecision(CodeReview.ReviewDecision.REJECTED);

        when(pullRequestRepository.findById(1L)).thenReturn(Optional.of(testPullRequest));
        when(codeReviewRepository.save(any(CodeReview.class))).thenReturn(rejectedReview);
        when(codeReviewRepository.findByPullRequestId(1L)).thenReturn(List.of(rejectedReview));
        when(pullRequestRepository.save(any(PullRequest.class))).thenReturn(testPullRequest);

        // When
        codeReviewService.addHumanReview(1L, rejectedReview);

        // Then
        verify(pullRequestRepository).save(argThat(pr -> 
            pr.getReviewStatus() == PullRequest.ReviewStatus.REJECTED));
    }

    @Test
    void shouldUpdatePullRequestStatusForChangesRequested() {
        // Given
        CodeReview changesRequestedReview = new CodeReview();
        changesRequestedReview.setPullRequest(testPullRequest);
        changesRequestedReview.setDecision(CodeReview.ReviewDecision.CHANGES_REQUESTED);

        when(pullRequestRepository.findById(1L)).thenReturn(Optional.of(testPullRequest));
        when(codeReviewRepository.save(any(CodeReview.class))).thenReturn(changesRequestedReview);
        when(codeReviewRepository.findByPullRequestId(1L)).thenReturn(List.of(changesRequestedReview));
        when(pullRequestRepository.save(any(PullRequest.class))).thenReturn(testPullRequest);

        // When
        codeReviewService.addHumanReview(1L, changesRequestedReview);

        // Then
        verify(pullRequestRepository).save(argThat(pr -> 
            pr.getReviewStatus() == PullRequest.ReviewStatus.CHANGES_REQUESTED));
    }
}
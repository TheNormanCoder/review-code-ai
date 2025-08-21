package com.reviewcode.ai.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PullRequestTest {

    private PullRequest pullRequest;

    @BeforeEach
    void setUp() {
        pullRequest = new PullRequest();
    }

    @Test
    void shouldCreatePullRequestWithDefaults() {
        // Given
        pullRequest.setTitle("Test PR");
        pullRequest.setAuthor("testuser");
        pullRequest.setRepositoryUrl("https://github.com/test/repo");
        pullRequest.setSourceBranch("feature/test");
        pullRequest.setTargetBranch("main");

        // Then
        assertEquals("Test PR", pullRequest.getTitle());
        assertEquals("testuser", pullRequest.getAuthor());
        assertEquals(PullRequest.PullRequestStatus.OPEN, pullRequest.getStatus());
        assertEquals(PullRequest.ReviewStatus.PENDING, pullRequest.getReviewStatus());
        assertNotNull(pullRequest.getCreatedAt());
        assertNotNull(pullRequest.getUpdatedAt());
    }

    @Test
    void shouldUpdateTimestampOnPreUpdate() {
        // Given
        LocalDateTime originalTime = pullRequest.getUpdatedAt();
        
        // When
        pullRequest.preUpdate();
        
        // Then
        assertNotEquals(originalTime, pullRequest.getUpdatedAt());
    }

    @Test
    void shouldHandleReviewsAndComments() {
        // Given
        pullRequest.setReviews(new ArrayList<>());
        pullRequest.setComments(new ArrayList<>());

        // Then
        assertNotNull(pullRequest.getReviews());
        assertNotNull(pullRequest.getComments());
        assertTrue(pullRequest.getReviews().isEmpty());
        assertTrue(pullRequest.getComments().isEmpty());
    }

    @Test
    void shouldValidatePullRequestStatus() {
        // Test all enum values
        pullRequest.setStatus(PullRequest.PullRequestStatus.OPEN);
        assertEquals(PullRequest.PullRequestStatus.OPEN, pullRequest.getStatus());

        pullRequest.setStatus(PullRequest.PullRequestStatus.CLOSED);
        assertEquals(PullRequest.PullRequestStatus.CLOSED, pullRequest.getStatus());

        pullRequest.setStatus(PullRequest.PullRequestStatus.MERGED);
        assertEquals(PullRequest.PullRequestStatus.MERGED, pullRequest.getStatus());

        pullRequest.setStatus(PullRequest.PullRequestStatus.DRAFT);
        assertEquals(PullRequest.PullRequestStatus.DRAFT, pullRequest.getStatus());
    }

    @Test
    void shouldValidateReviewStatus() {
        // Test all enum values
        pullRequest.setReviewStatus(PullRequest.ReviewStatus.PENDING);
        assertEquals(PullRequest.ReviewStatus.PENDING, pullRequest.getReviewStatus());

        pullRequest.setReviewStatus(PullRequest.ReviewStatus.IN_PROGRESS);
        assertEquals(PullRequest.ReviewStatus.IN_PROGRESS, pullRequest.getReviewStatus());

        pullRequest.setReviewStatus(PullRequest.ReviewStatus.APPROVED);
        assertEquals(PullRequest.ReviewStatus.APPROVED, pullRequest.getReviewStatus());

        pullRequest.setReviewStatus(PullRequest.ReviewStatus.CHANGES_REQUESTED);
        assertEquals(PullRequest.ReviewStatus.CHANGES_REQUESTED, pullRequest.getReviewStatus());

        pullRequest.setReviewStatus(PullRequest.ReviewStatus.REJECTED);
        assertEquals(PullRequest.ReviewStatus.REJECTED, pullRequest.getReviewStatus());
    }

    @Test
    void shouldCreatePullRequestWithAllFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // When
        PullRequest pr = new PullRequest(
            1L,
            "Feature Implementation",
            "This PR implements a new feature",
            "developer1",
            "https://github.com/company/repo",
            "feature/new-feature",
            "main",
            PullRequest.PullRequestStatus.OPEN,
            PullRequest.ReviewStatus.PENDING,
            now,
            now,
            new ArrayList<>(),
            new ArrayList<>()
        );

        // Then
        assertEquals(1L, pr.getId());
        assertEquals("Feature Implementation", pr.getTitle());
        assertEquals("This PR implements a new feature", pr.getDescription());
        assertEquals("developer1", pr.getAuthor());
        assertEquals("https://github.com/company/repo", pr.getRepositoryUrl());
        assertEquals("feature/new-feature", pr.getSourceBranch());
        assertEquals("main", pr.getTargetBranch());
        assertEquals(PullRequest.PullRequestStatus.OPEN, pr.getStatus());
        assertEquals(PullRequest.ReviewStatus.PENDING, pr.getReviewStatus());
        assertEquals(now, pr.getCreatedAt());
        assertEquals(now, pr.getUpdatedAt());
    }
}
package com.reviewcode.ai.repository;

import com.reviewcode.ai.model.PullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PullRequestRepositoryTest {

    @Autowired
    private PullRequestRepository pullRequestRepository;

    private PullRequest testPr1;
    private PullRequest testPr2;
    private PullRequest testPr3;

    @BeforeEach
    void setUp() {
        pullRequestRepository.deleteAll();

        LocalDateTime baseTime = LocalDateTime.now().minusDays(7);

        testPr1 = new PullRequest();
        testPr1.setTitle("Feature A");
        testPr1.setAuthor("alice");
        testPr1.setRepositoryUrl("https://github.com/test/repo1");
        testPr1.setSourceBranch("feature/a");
        testPr1.setTargetBranch("main");
        testPr1.setStatus(PullRequest.PullRequestStatus.OPEN);
        testPr1.setReviewStatus(PullRequest.ReviewStatus.PENDING);
        testPr1.setCreatedAt(baseTime);
        testPr1.setUpdatedAt(baseTime);

        testPr2 = new PullRequest();
        testPr2.setTitle("Feature B");
        testPr2.setAuthor("bob");
        testPr2.setRepositoryUrl("https://github.com/test/repo1");
        testPr2.setSourceBranch("feature/b");
        testPr2.setTargetBranch("main");
        testPr2.setStatus(PullRequest.PullRequestStatus.MERGED);
        testPr2.setReviewStatus(PullRequest.ReviewStatus.APPROVED);
        testPr2.setCreatedAt(baseTime.plusDays(1));
        testPr2.setUpdatedAt(baseTime.plusDays(1));

        testPr3 = new PullRequest();
        testPr3.setTitle("Feature C");
        testPr3.setAuthor("alice");
        testPr3.setRepositoryUrl("https://github.com/test/repo2");
        testPr3.setSourceBranch("feature/c");
        testPr3.setTargetBranch("develop");
        testPr3.setStatus(PullRequest.PullRequestStatus.CLOSED);
        testPr3.setReviewStatus(PullRequest.ReviewStatus.REJECTED);
        testPr3.setCreatedAt(baseTime.plusDays(2));
        testPr3.setUpdatedAt(baseTime.plusDays(2));

        pullRequestRepository.saveAll(List.of(testPr1, testPr2, testPr3));
    }

    @Test
    void shouldFindByAuthor() {
        // When
        List<PullRequest> alicePrs = pullRequestRepository.findByAuthor("alice");
        List<PullRequest> bobPrs = pullRequestRepository.findByAuthor("bob");
        List<PullRequest> nonExistentPrs = pullRequestRepository.findByAuthor("charlie");

        // Then
        assertEquals(2, alicePrs.size());
        assertTrue(alicePrs.stream().allMatch(pr -> "alice".equals(pr.getAuthor())));

        assertEquals(1, bobPrs.size());
        assertEquals("bob", bobPrs.get(0).getAuthor());

        assertTrue(nonExistentPrs.isEmpty());
    }

    @Test
    void shouldFindByStatus() {
        // When
        List<PullRequest> openPrs = pullRequestRepository.findByStatus(PullRequest.PullRequestStatus.OPEN);
        List<PullRequest> mergedPrs = pullRequestRepository.findByStatus(PullRequest.PullRequestStatus.MERGED);
        List<PullRequest> closedPrs = pullRequestRepository.findByStatus(PullRequest.PullRequestStatus.CLOSED);
        List<PullRequest> draftPrs = pullRequestRepository.findByStatus(PullRequest.PullRequestStatus.DRAFT);

        // Then
        assertEquals(1, openPrs.size());
        assertEquals("Feature A", openPrs.get(0).getTitle());

        assertEquals(1, mergedPrs.size());
        assertEquals("Feature B", mergedPrs.get(0).getTitle());

        assertEquals(1, closedPrs.size());
        assertEquals("Feature C", closedPrs.get(0).getTitle());

        assertTrue(draftPrs.isEmpty());
    }

    @Test
    void shouldFindByReviewStatus() {
        // When
        List<PullRequest> pendingPrs = pullRequestRepository.findByReviewStatus(PullRequest.ReviewStatus.PENDING);
        List<PullRequest> approvedPrs = pullRequestRepository.findByReviewStatus(PullRequest.ReviewStatus.APPROVED);
        List<PullRequest> rejectedPrs = pullRequestRepository.findByReviewStatus(PullRequest.ReviewStatus.REJECTED);

        // Then
        assertEquals(1, pendingPrs.size());
        assertEquals("Feature A", pendingPrs.get(0).getTitle());

        assertEquals(1, approvedPrs.size());
        assertEquals("Feature B", approvedPrs.get(0).getTitle());

        assertEquals(1, rejectedPrs.size());
        assertEquals("Feature C", rejectedPrs.get(0).getTitle());
    }

    @Test
    void shouldFindByRepositoryUrl() {
        // When
        List<PullRequest> repo1Prs = pullRequestRepository.findByRepositoryUrl("https://github.com/test/repo1");
        List<PullRequest> repo2Prs = pullRequestRepository.findByRepositoryUrl("https://github.com/test/repo2");
        List<PullRequest> nonExistentRepoPrs = pullRequestRepository.findByRepositoryUrl("https://github.com/test/repo3");

        // Then
        assertEquals(2, repo1Prs.size());
        assertTrue(repo1Prs.stream().allMatch(pr -> "https://github.com/test/repo1".equals(pr.getRepositoryUrl())));

        assertEquals(1, repo2Prs.size());
        assertEquals("https://github.com/test/repo2", repo2Prs.get(0).getRepositoryUrl());

        assertTrue(nonExistentRepoPrs.isEmpty());
    }

    @Test
    void shouldFindByCreatedAtBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(8);
        LocalDateTime endDate = LocalDateTime.now().minusDays(6);

        // When
        List<PullRequest> prsInRange = pullRequestRepository.findByCreatedAtBetween(startDate, endDate);

        // Then
        assertEquals(1, prsInRange.size());
        assertEquals("Feature A", prsInRange.get(0).getTitle());
    }

    @Test
    void shouldFindByAuthorAndStatus() {
        // When
        List<PullRequest> aliceOpenPrs = pullRequestRepository.findByAuthorAndStatus("alice", PullRequest.PullRequestStatus.OPEN);
        List<PullRequest> aliceClosedPrs = pullRequestRepository.findByAuthorAndStatus("alice", PullRequest.PullRequestStatus.CLOSED);
        List<PullRequest> bobOpenPrs = pullRequestRepository.findByAuthorAndStatus("bob", PullRequest.PullRequestStatus.OPEN);

        // Then
        assertEquals(1, aliceOpenPrs.size());
        assertEquals("Feature A", aliceOpenPrs.get(0).getTitle());

        assertEquals(1, aliceClosedPrs.size());
        assertEquals("Feature C", aliceClosedPrs.get(0).getTitle());

        assertTrue(bobOpenPrs.isEmpty());
    }

    @Test
    void shouldPersistAndRetrievePullRequestWithAllFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        PullRequest newPr = new PullRequest();
        newPr.setTitle("Comprehensive PR");
        newPr.setDescription("This PR has all fields filled");
        newPr.setAuthor("comprehensive-user");
        newPr.setRepositoryUrl("https://github.com/test/comprehensive");
        newPr.setSourceBranch("feature/comprehensive");
        newPr.setTargetBranch("main");
        newPr.setStatus(PullRequest.PullRequestStatus.DRAFT);
        newPr.setReviewStatus(PullRequest.ReviewStatus.IN_PROGRESS);
        newPr.setCreatedAt(now);
        newPr.setUpdatedAt(now);

        // When
        PullRequest saved = pullRequestRepository.save(newPr);
        PullRequest retrieved = pullRequestRepository.findById(saved.getId()).orElse(null);

        // Then
        assertNotNull(retrieved);
        assertEquals("Comprehensive PR", retrieved.getTitle());
        assertEquals("This PR has all fields filled", retrieved.getDescription());
        assertEquals("comprehensive-user", retrieved.getAuthor());
        assertEquals("https://github.com/test/comprehensive", retrieved.getRepositoryUrl());
        assertEquals("feature/comprehensive", retrieved.getSourceBranch());
        assertEquals("main", retrieved.getTargetBranch());
        assertEquals(PullRequest.PullRequestStatus.DRAFT, retrieved.getStatus());
        assertEquals(PullRequest.ReviewStatus.IN_PROGRESS, retrieved.getReviewStatus());
        assertNotNull(retrieved.getCreatedAt());
        assertNotNull(retrieved.getUpdatedAt());
        assertNotNull(retrieved.getId());
    }

    @Test
    void shouldHandleNullAndEmptyQueries() {
        // When & Then
        List<PullRequest> nullAuthorPrs = pullRequestRepository.findByAuthor(null);
        assertTrue(nullAuthorPrs.isEmpty());

        List<PullRequest> emptyAuthorPrs = pullRequestRepository.findByAuthor("");
        assertTrue(emptyAuthorPrs.isEmpty());

        List<PullRequest> nullRepoUrlPrs = pullRequestRepository.findByRepositoryUrl(null);
        assertTrue(nullRepoUrlPrs.isEmpty());
    }

    @Test
    void shouldOrderResultsConsistently() {
        // When
        List<PullRequest> allPrs = pullRequestRepository.findAll();

        // Then
        assertEquals(3, allPrs.size());
        // Results should be ordered by ID (insertion order in this case)
        assertTrue(allPrs.get(0).getId() < allPrs.get(1).getId());
        assertTrue(allPrs.get(1).getId() < allPrs.get(2).getId());
    }
}
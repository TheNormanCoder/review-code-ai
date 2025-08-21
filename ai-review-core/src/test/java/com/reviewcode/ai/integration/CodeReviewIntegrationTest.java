package com.reviewcode.ai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewcode.ai.model.CodeReview;
import com.reviewcode.ai.model.PullRequest;
import com.reviewcode.ai.repository.CodeReviewRepository;
import com.reviewcode.ai.repository.PullRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class CodeReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PullRequestRepository pullRequestRepository;

    @Autowired
    private CodeReviewRepository codeReviewRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private PullRequest testPullRequest;

    @BeforeEach
    void setUp() {
        codeReviewRepository.deleteAll();
        pullRequestRepository.deleteAll();

        testPullRequest = new PullRequest();
        testPullRequest.setTitle("Integration Test PR");
        testPullRequest.setDescription("Test description for integration");
        testPullRequest.setAuthor("integration-test-user");
        testPullRequest.setRepositoryUrl("https://github.com/test/integration-repo");
        testPullRequest.setSourceBranch("feature/integration-test");
        testPullRequest.setTargetBranch("main");
        testPullRequest.setStatus(PullRequest.PullRequestStatus.OPEN);
        testPullRequest.setReviewStatus(PullRequest.ReviewStatus.PENDING);
        testPullRequest.setCreatedAt(LocalDateTime.now());
        testPullRequest.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void shouldCreatePullRequestAndPersistToDatabase() throws Exception {
        // When
        mockMvc.perform(post("/reviews/pull-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPullRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration Test PR"))
                .andExpect(jsonPath("$.author").value("integration-test-user"));

        // Then
        assertEquals(1, pullRequestRepository.count());
        PullRequest savedPr = pullRequestRepository.findAll().get(0);
        assertEquals("Integration Test PR", savedPr.getTitle());
        assertEquals("integration-test-user", savedPr.getAuthor());
        assertNotNull(savedPr.getCreatedAt());
        assertNotNull(savedPr.getUpdatedAt());
    }

    @Test
    void shouldRetrievePullRequestsFromDatabase() throws Exception {
        // Given
        PullRequest savedPr = pullRequestRepository.save(testPullRequest);

        // When & Then
        mockMvc.perform(get("/reviews/pull-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(savedPr.getId()))
                .andExpect(jsonPath("$[0].title").value("Integration Test PR"));
    }

    @Test
    void shouldFilterPullRequestsByAuthor() throws Exception {
        // Given
        pullRequestRepository.save(testPullRequest);
        
        PullRequest anotherPr = new PullRequest();
        anotherPr.setTitle("Another PR");
        anotherPr.setAuthor("another-user");
        anotherPr.setRepositoryUrl("https://github.com/test/repo");
        anotherPr.setSourceBranch("feature/another");
        anotherPr.setTargetBranch("main");
        pullRequestRepository.save(anotherPr);

        // When & Then
        mockMvc.perform(get("/reviews/pull-requests")
                .param("author", "integration-test-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].author").value("integration-test-user"));
    }

    @Test
    void shouldAddHumanReviewAndUpdatePullRequestStatus() throws Exception {
        // Given
        PullRequest savedPr = pullRequestRepository.save(testPullRequest);

        CodeReview humanReview = new CodeReview();
        humanReview.setReviewer("human-reviewer");
        humanReview.setDecision(CodeReview.ReviewDecision.APPROVED);
        humanReview.setSummary("Code looks great!");

        // When
        mockMvc.perform(post("/reviews/pull-requests/" + savedPr.getId() + "/human-review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(humanReview)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewer").value("human-reviewer"))
                .andExpect(jsonPath("$.decision").value("APPROVED"));

        // Then
        assertEquals(1, codeReviewRepository.count());
        CodeReview savedReview = codeReviewRepository.findAll().get(0);
        assertEquals("human-reviewer", savedReview.getReviewer());
        assertEquals(CodeReview.ReviewerType.HUMAN, savedReview.getReviewerType());
        assertEquals(CodeReview.ReviewDecision.APPROVED, savedReview.getDecision());
        assertNotNull(savedReview.getCreatedAt());

        // Verify PR status was updated
        PullRequest updatedPr = pullRequestRepository.findById(savedPr.getId()).orElse(null);
        assertNotNull(updatedPr);
        assertEquals(PullRequest.ReviewStatus.APPROVED, updatedPr.getReviewStatus());
    }

    @Test
    void shouldGetReviewsForSpecificPullRequest() throws Exception {
        // Given
        PullRequest savedPr = pullRequestRepository.save(testPullRequest);

        CodeReview review1 = new CodeReview();
        review1.setPullRequest(savedPr);
        review1.setReviewer("reviewer1");
        review1.setReviewerType(CodeReview.ReviewerType.HUMAN);
        review1.setDecision(CodeReview.ReviewDecision.APPROVED);
        review1.setCreatedAt(LocalDateTime.now());
        codeReviewRepository.save(review1);

        CodeReview review2 = new CodeReview();
        review2.setPullRequest(savedPr);
        review2.setReviewer("AI-MCP");
        review2.setReviewerType(CodeReview.ReviewerType.AI_MCP);
        review2.setDecision(CodeReview.ReviewDecision.APPROVED);
        review2.setCreatedAt(LocalDateTime.now());
        codeReviewRepository.save(review2);

        // When & Then
        mockMvc.perform(get("/reviews/pull-requests/" + savedPr.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].reviewer").value("reviewer1"))
                .andExpect(jsonPath("$[1].reviewer").value("AI-MCP"));
    }

    @Test
    void shouldHandleMultipleReviewsAndStatusUpdate() throws Exception {
        // Given
        PullRequest savedPr = pullRequestRepository.save(testPullRequest);

        // Add first review - changes requested
        CodeReview review1 = new CodeReview();
        review1.setReviewer("reviewer1");
        review1.setDecision(CodeReview.ReviewDecision.CHANGES_REQUESTED);
        review1.setSummary("Please fix the naming conventions");

        mockMvc.perform(post("/reviews/pull-requests/" + savedPr.getId() + "/human-review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(review1)))
                .andExpect(status().isCreated());

        // Verify status is CHANGES_REQUESTED
        PullRequest prAfterFirstReview = pullRequestRepository.findById(savedPr.getId()).orElse(null);
        assertEquals(PullRequest.ReviewStatus.CHANGES_REQUESTED, prAfterFirstReview.getReviewStatus());

        // Add second review - approved
        CodeReview review2 = new CodeReview();
        review2.setReviewer("reviewer2");
        review2.setDecision(CodeReview.ReviewDecision.APPROVED);
        review2.setSummary("Looks good now!");

        mockMvc.perform(post("/reviews/pull-requests/" + savedPr.getId() + "/human-review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(review2)))
                .andExpect(status().isCreated());

        // Verify final status (should still be CHANGES_REQUESTED due to first review)
        PullRequest finalPr = pullRequestRepository.findById(savedPr.getId()).orElse(null);
        assertEquals(PullRequest.ReviewStatus.CHANGES_REQUESTED, finalPr.getReviewStatus());

        // Verify both reviews exist
        assertEquals(2, codeReviewRepository.findByPullRequestId(savedPr.getId()).size());
    }

    @Test
    void shouldReturnNotFoundForNonExistentPullRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/reviews/pull-requests/999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/reviews/pull-requests/999/human-review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CodeReview())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldValidateRequiredFieldsAndReturnBadRequest() throws Exception {
        // Given - PR with missing required fields
        PullRequest invalidPr = new PullRequest();
        invalidPr.setTitle(""); // Empty title should fail validation

        // When & Then
        mockMvc.perform(post("/reviews/pull-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPr)))
                .andExpect(status().isBadRequest());

        // Verify no PR was created
        assertEquals(0, pullRequestRepository.count());
    }
}
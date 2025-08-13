package com.reviewcode.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewcode.ai.model.CodeReview;
import com.reviewcode.ai.model.PullRequest;
import com.reviewcode.ai.service.CodeReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PullRequestController.class)
@ActiveProfiles("test")
class PullRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CodeReviewService codeReviewService;

    @Autowired
    private ObjectMapper objectMapper;

    private PullRequest testPullRequest;
    private CodeReview testCodeReview;

    @BeforeEach
    void setUp() {
        testPullRequest = new PullRequest();
        testPullRequest.setId(1L);
        testPullRequest.setTitle("Test PR");
        testPullRequest.setDescription("Test description");
        testPullRequest.setAuthor("testuser");
        testPullRequest.setRepositoryUrl("https://github.com/test/repo");
        testPullRequest.setSourceBranch("feature/test");
        testPullRequest.setTargetBranch("main");
        testPullRequest.setStatus(PullRequest.PullRequestStatus.OPEN);
        testPullRequest.setReviewStatus(PullRequest.ReviewStatus.PENDING);
        testPullRequest.setCreatedAt(LocalDateTime.now());

        testCodeReview = new CodeReview();
        testCodeReview.setId(1L);
        testCodeReview.setPullRequest(testPullRequest);
        testCodeReview.setReviewer("AI-MCP");
        testCodeReview.setReviewerType(CodeReview.ReviewerType.AI_MCP);
        testCodeReview.setDecision(CodeReview.ReviewDecision.APPROVED);
        testCodeReview.setSummary("Code looks good");
    }

    @Test
    void shouldCreatePullRequestSuccessfully() throws Exception {
        // Given
        when(codeReviewService.createPullRequest(any(PullRequest.class))).thenReturn(testPullRequest);

        // When & Then
        mockMvc.perform(post("/reviews/pull-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPullRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test PR"))
                .andExpected(jsonPath("$.author").value("testuser"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.reviewStatus").value("PENDING"));
    }

    @Test
    void shouldGetAllPullRequests() throws Exception {
        // Given
        List<PullRequest> pullRequests = List.of(testPullRequest);
        when(codeReviewService.getAllPullRequests()).thenReturn(pullRequests);

        // When & Then
        mockMvc.perform(get("/reviews/pull-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test PR"));
    }

    @Test
    void shouldGetPullRequestsByAuthor() throws Exception {
        // Given
        List<PullRequest> pullRequests = List.of(testPullRequest);
        when(codeReviewService.getPullRequestsByAuthor("testuser")).thenReturn(pullRequests);

        // When & Then
        mockMvc.perform(get("/reviews/pull-requests")
                .param("author", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].author").value("testuser"));
    }

    @Test
    void shouldGetPullRequestsByStatus() throws Exception {
        // Given
        List<PullRequest> pullRequests = List.of(testPullRequest);
        when(codeReviewService.getPullRequestsByStatus(PullRequest.PullRequestStatus.OPEN))
                .thenReturn(pullRequests);

        // When & Then
        mockMvc.perform(get("/reviews/pull-requests")
                .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void shouldGetPullRequestById() throws Exception {
        // Given
        when(codeReviewService.getPullRequest(1L)).thenReturn(Optional.of(testPullRequest));

        // When & Then
        mockMvc.perform(get("/reviews/pull-requests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test PR"));
    }

    @Test
    void shouldReturnNotFoundWhenPullRequestDoesNotExist() throws Exception {
        // Given
        when(codeReviewService.getPullRequest(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/reviews/pull-requests/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldTriggerAiReviewSuccessfully() throws Exception {
        // Given
        List<String> filesToReview = List.of("src/main/java/TestClass.java");
        when(codeReviewService.triggerAiReview(eq(1L), any(List.class)))
                .thenReturn(Mono.just(testCodeReview));

        // When & Then
        mockMvc.perform(post("/reviews/pull-requests/1/ai-review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filesToReview)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reviewer").value("AI-MCP"))
                .andExpect(jsonPath("$.reviewerType").value("AI_MCP"))
                .andExpect(jsonPath("$.decision").value("APPROVED"));
    }

    @Test
    void shouldHandleAiReviewError() throws Exception {
        // Given
        List<String> filesToReview = List.of("src/main/java/TestClass.java");
        when(codeReviewService.triggerAiReview(eq(999L), any(List.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Pull request not found")));

        // When & Then
        mockMvc.perform(post("/reviews/pull-requests/999/ai-review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filesToReview)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAddHumanReviewSuccessfully() throws Exception {
        // Given
        CodeReview humanReview = new CodeReview();
        humanReview.setReviewer("human-reviewer");
        humanReview.setDecision(CodeReview.ReviewDecision.APPROVED);
        humanReview.setSummary("Looks good to me!");

        when(codeReviewService.addHumanReview(eq(1L), any(CodeReview.class)))
                .thenReturn(humanReview);

        // When & Then
        mockMvc.perform(post("/reviews/pull-requests/1/human-review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(humanReview)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewer").value("human-reviewer"))
                .andExpect(jsonPath("$.decision").value("APPROVED"));
    }

    @Test
    void shouldHandleHumanReviewErrorWhenPullRequestNotFound() throws Exception {
        // Given
        CodeReview humanReview = new CodeReview();
        humanReview.setReviewer("human-reviewer");

        when(codeReviewService.addHumanReview(eq(999L), any(CodeReview.class)))
                .thenThrow(new IllegalArgumentException("Pull request not found"));

        // When & Then
        mockMvc.perform(post("/reviews/pull-requests/999/human-review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(humanReview)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetReviewsForPullRequest() throws Exception {
        // Given
        List<CodeReview> reviews = List.of(testCodeReview);
        when(codeReviewService.getReviewsForPullRequest(1L)).thenReturn(reviews);

        // When & Then
        mockMvc.perform(get("/reviews/pull-requests/1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].reviewer").value("AI-MCP"));
    }

    @Test
    void shouldValidateRequiredFieldsInCreatePullRequest() throws Exception {
        // Given
        PullRequest invalidPr = new PullRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/reviews/pull-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPr)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldValidateRequiredFieldsInHumanReview() throws Exception {
        // Given
        CodeReview invalidReview = new CodeReview();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/reviews/pull-requests/1/human-review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidReview)))
                .andExpect(status().isBadRequest());
    }
}
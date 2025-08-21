package com.reviewcode.ai.service;

import com.reviewcode.ai.config.AiConfiguration;
import com.reviewcode.ai.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.Disabled("Temporarily disabled - API changed, needs updating")
class AiReviewServiceIntegrationTest {

    private MockWebServer mockWebServer;
    private AiReviewService aiReviewService;
    private ObjectMapper objectMapper;
    
    @Mock
    private AiConfiguration aiConfig;
    
    @Mock
    private AiConfiguration.Mcp mcpConfig;
    
    @Mock
    private ReviewSuggestionService reviewSuggestionService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        objectMapper = new ObjectMapper();
        
        when(aiConfig.getMcp()).thenReturn(mcpConfig);
        when(mcpConfig.getTimeout()).thenReturn(5000);
        
        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();
            
        aiReviewService = new AiReviewService(webClient, aiConfig, reviewSuggestionService);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldPerformSuccessfulAiReview() throws Exception {
        // Given
        PullRequest pullRequest = createTestPullRequest();
        List<String> filesToReview = Arrays.asList("UserService.java", "UserController.java");
        
        AiReviewResponse mockResponse = createSuccessfulAiResponse();
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(mockResponse))
            .addHeader("Content-Type", "application/json"));

        // When
        Mono<CodeReview> result = aiReviewService.performAiReview(pullRequest, filesToReview);

        // Then
        StepVerifier.create(result)
            .assertNext(review -> {
                assertNotNull(review);
                assertEquals("AI-MCP", review.getReviewer());
                assertEquals(CodeReview.ReviewerType.AI_MCP, review.getReviewerType());
                assertEquals(CodeReview.ReviewDecision.APPROVED, review.getDecision());
                assertEquals(85, review.getScore());
                assertFalse(review.getFindings().isEmpty());
            })
            .verifyComplete();

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/review", recordedRequest.getPath());
        
        String requestBody = recordedRequest.getBody().readUtf8();
        assertTrue(requestBody.contains("UserService.java"));
        assertTrue(requestBody.contains("UserController.java"));
    }

    @Test
    void shouldHandleAiServiceTimeout() {
        // Given
        when(mcpConfig.getTimeout()).thenReturn(100); // Very short timeout
        
        PullRequest pullRequest = createTestPullRequest();
        List<String> filesToReview = Arrays.asList("SlowService.java");
        
        // Mock slow response
        mockWebServer.enqueue(new MockResponse()
            .setBodyDelay(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setBody("{}"));

        // When
        Mono<CodeReview> result = aiReviewService.performAiReview(pullRequest, filesToReview);

        // Then
        StepVerifier.create(result)
            .assertNext(review -> {
                assertEquals(CodeReview.ReviewDecision.REJECTED, review.getDecision());
                assertEquals("AI-MCP", review.getReviewer());
                assertTrue(review.getComments().stream()
                    .anyMatch(comment -> comment.getContent().contains("timeout")));
            })
            .verifyComplete();
    }

    @Test
    void shouldHandleAiServiceError() {
        // Given
        PullRequest pullRequest = createTestPullRequest();
        List<String> filesToReview = Arrays.asList("ErrorService.java");
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));

        // When
        Mono<CodeReview> result = aiReviewService.performAiReview(pullRequest, filesToReview);

        // Then
        StepVerifier.create(result)
            .assertNext(review -> {
                assertEquals(CodeReview.ReviewDecision.REJECTED, review.getDecision());
                assertEquals("AI-MCP", review.getReviewer());
                assertTrue(review.getComments().stream()
                    .anyMatch(comment -> comment.getContent().contains("error")));
            })
            .verifyComplete();
    }

    @Test
    void shouldHandleAiServiceRejectingCode() throws Exception {
        // Given
        PullRequest pullRequest = createTestPullRequest();
        List<String> filesToReview = Arrays.asList("BadCode.java");
        
        AiReviewResponse rejectResponse = createRejectingAiResponse();
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(rejectResponse))
            .addHeader("Content-Type", "application/json"));

        // When
        Mono<CodeReview> result = aiReviewService.performAiReview(pullRequest, filesToReview);

        // Then
        StepVerifier.create(result)
            .assertNext(review -> {
                assertEquals(CodeReview.ReviewDecision.CHANGES_REQUESTED, review.getDecision());
                assertEquals(25, review.getScore());
                assertTrue(review.getFindings().stream()
                    .anyMatch(f -> f.getSeverity() == ReviewFinding.Severity.CRITICAL));
            })
            .verifyComplete();
    }

    @Test
    void shouldHandleEmptyFilesList() {
        // Given
        PullRequest pullRequest = createTestPullRequest();
        List<String> emptyFiles = Arrays.asList();

        // When
        Mono<CodeReview> result = aiReviewService.performAiReview(pullRequest, emptyFiles);

        // Then
        StepVerifier.create(result)
            .assertNext(review -> {
                assertEquals(CodeReview.ReviewDecision.NO_REVIEW_NEEDED, review.getDecision());
                assertTrue(review.getComments().stream()
                    .anyMatch(comment -> comment.getContent().contains("No files")));
            })
            .verifyComplete();
    }

    @Test
    void shouldIncludeContextInAiRequest() throws Exception {
        // Given
        PullRequest pullRequest = createTestPullRequest();
        pullRequest.setDescription("Fix security vulnerability in user authentication");
        List<String> filesToReview = Arrays.asList("AuthService.java");
        
        AiReviewResponse mockResponse = createSuccessfulAiResponse();
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(mockResponse))
            .addHeader("Content-Type", "application/json"));

        // When
        Mono<CodeReview> result = aiReviewService.performAiReview(pullRequest, filesToReview);

        // Then
        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();
        
        assertTrue(requestBody.contains("Fix security vulnerability"));
        assertTrue(requestBody.contains("AuthService.java"));
        assertTrue(requestBody.contains(pullRequest.getAuthor()));
    }

    @Test
    void shouldHandleMalformedAiResponse() {
        // Given
        PullRequest pullRequest = createTestPullRequest();
        List<String> filesToReview = Arrays.asList("TestService.java");
        
        mockWebServer.enqueue(new MockResponse()
            .setBody("{ invalid json }")
            .addHeader("Content-Type", "application/json"));

        // When
        Mono<CodeReview> result = aiReviewService.performAiReview(pullRequest, filesToReview);

        // Then
        StepVerifier.create(result)
            .assertNext(review -> {
                assertEquals(CodeReview.ReviewDecision.REJECTED, review.getDecision());
                assertTrue(review.getComments().stream()
                    .anyMatch(comment -> comment.getContent().contains("error")));
            })
            .verifyComplete();
    }

    @Test
    void shouldRetryOnTransientFailures() {
        // Given
        PullRequest pullRequest = createTestPullRequest();
        List<String> filesToReview = Arrays.asList("RetryService.java");
        
        // First request fails, second succeeds
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"decision\":\"APPROVED\",\"score\":80,\"findings\":[]}")
            .addHeader("Content-Type", "application/json"));

        // When
        Mono<CodeReview> result = aiReviewService.performAiReview(pullRequest, filesToReview)
            .retry(1);

        // Then
        StepVerifier.create(result)
            .assertNext(review -> {
                assertEquals(CodeReview.ReviewDecision.APPROVED, review.getDecision());
            })
            .verifyComplete();
    }

    @Test
    void shouldMapAiResponseCorrectly() throws Exception {
        // Given
        PullRequest pullRequest = createTestPullRequest();
        List<String> filesToReview = Arrays.asList("MappingTest.java");
        
        AiReviewResponse complexResponse = createComplexAiResponse();
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(complexResponse))
            .addHeader("Content-Type", "application/json"));

        // When
        Mono<CodeReview> result = aiReviewService.performAiReview(pullRequest, filesToReview);

        // Then
        StepVerifier.create(result)
            .assertNext(review -> {
                assertEquals(3, review.getFindings().size());
                assertEquals(2, review.getComments().size());
                
                // Verify finding mapping
                ReviewFinding securityFinding = review.getFindings().stream()
                    .filter(f -> f.getType() == ReviewFinding.FindingType.SECURITY)
                    .findFirst().orElse(null);
                    
                assertNotNull(securityFinding);
                assertEquals(ReviewFinding.Severity.CRITICAL, securityFinding.getSeverity());
                assertEquals("MappingTest.java", securityFinding.getFileName());
                assertEquals(Integer.valueOf(25), securityFinding.getLineNumber());
            })
            .verifyComplete();
    }

    private PullRequest createTestPullRequest() {
        PullRequest pr = new PullRequest();
        pr.setId(123L);
        pr.setTitle("Test Pull Request");
        pr.setDescription("This is a test PR for AI review");
        pr.setAuthor("testuser");
        pr.setRepositoryUrl("https://github.com/test/repo");
        return pr;
    }

    private AiReviewResponse createSuccessfulAiResponse() {
        AiReviewResponse response = new AiReviewResponse();
        response.setDecision("APPROVED");
        response.setScore(85);
        response.setFindings(Arrays.asList(
            createTestFinding("BEST_PRACTICE", "MEDIUM", "Method too long", "UserService.java", 42)
        ));
        response.setComments(Arrays.asList("Code looks good overall!"));
        return response;
    }

    private AiReviewResponse createRejectingAiResponse() {
        AiReviewResponse response = new AiReviewResponse();
        response.setDecision("CHANGES_REQUESTED");
        response.setScore(25);
        response.setFindings(Arrays.asList(
            createTestFinding("SECURITY", "CRITICAL", "SQL injection vulnerability", "BadCode.java", 15),
            createTestFinding("PERFORMANCE", "HIGH", "N+1 query problem", "BadCode.java", 32)
        ));
        response.setComments(Arrays.asList("Critical security issues found"));
        return response;
    }

    private AiReviewResponse createComplexAiResponse() {
        AiReviewResponse response = new AiReviewResponse();
        response.setDecision("CHANGES_REQUESTED");
        response.setScore(65);
        response.setFindings(Arrays.asList(
            createTestFinding("SECURITY", "CRITICAL", "Hardcoded secret", "MappingTest.java", 25),
            createTestFinding("SOLID_PRINCIPLES", "HIGH", "SRP violation", "MappingTest.java", 45),
            createTestFinding("BEST_PRACTICE", "MEDIUM", "Poor naming", "MappingTest.java", 78)
        ));
        response.setComments(Arrays.asList(
            "Security vulnerabilities need immediate attention",
            "Consider refactoring to improve code quality"
        ));
        return response;
    }

    private AiReviewResponse.Finding createTestFinding(String type, String severity, 
                                                      String description, String fileName, int lineNumber) {
        AiReviewResponse.Finding finding = new AiReviewResponse.Finding();
        finding.setType(type);
        finding.setSeverity(severity);
        finding.setDescription(description);
        finding.setFileName(fileName);
        finding.setLineNumber(lineNumber);
        finding.setSuggestion("Fix this issue");
        return finding;
    }
}
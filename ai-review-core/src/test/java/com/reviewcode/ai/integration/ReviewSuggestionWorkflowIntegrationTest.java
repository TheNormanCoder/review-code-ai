package com.reviewcode.ai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewcode.ai.model.PullRequest;
import com.reviewcode.ai.model.ReviewSuggestion;
import com.reviewcode.ai.repository.PullRequestRepository;
import com.reviewcode.ai.repository.ReviewSuggestionRepository;
import com.reviewcode.ai.service.AiReviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@org.junit.jupiter.api.Disabled("Temporarily disabled - WebClient mocking issue")
class ReviewSuggestionWorkflowIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private PullRequestRepository pullRequestRepository;
    
    @Autowired
    private ReviewSuggestionRepository reviewSuggestionRepository;
    
    @MockBean
    private WebClient aiWebClient;
    
    @MockBean
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @MockBean
    private WebClient.RequestBodySpec requestBodySpec;
    
    @MockBean
    private WebClient.ResponseSpec responseSpec;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private PullRequest testPullRequest;
    
    @BeforeEach
    void setUp() {
        testPullRequest = new PullRequest();
        testPullRequest.setTitle("Test PR for Workflow");
        testPullRequest.setDescription("Testing new suggestion workflow");
        testPullRequest.setAuthor("test-user");
        testPullRequest.setRepositoryUrl("https://github.com/test/repo");
        testPullRequest.setSourceBranch("feature/suggestions");
        testPullRequest.setTargetBranch("main");
        testPullRequest.setStatus(PullRequest.PullRequestStatus.OPEN);
        testPullRequest.setReviewStatus(PullRequest.ReviewStatus.PENDING);
        testPullRequest.setCreatedAt(LocalDateTime.now());
        testPullRequest.setUpdatedAt(LocalDateTime.now());
        
        testPullRequest = pullRequestRepository.save(testPullRequest);
    }
    
    @Test
    void shouldCompleteFullWorkflowFromSuggestionsToFinalReview() throws Exception {
        // Step 1: Generate AI suggestions
        AiReviewResponse.Finding finding1 = new AiReviewResponse.Finding(
            "TestFile.java", 42, "SECURITY", "HIGH", 
            "SQL injection vulnerability", "Use parameterized queries", 
            "SELECT * FROM users WHERE id = " + "id", "SELECT * FROM users WHERE id = ?", "SEC-001"
        );
        
        AiReviewResponse.Finding finding2 = new AiReviewResponse.Finding(
            "TestFile.java", 55, "PERFORMANCE", "MEDIUM",
            "Inefficient loop", "Use stream API", 
            "for(int i=0; i<list.size(); i++)", "list.stream().forEach(...)", "PERF-001"
        );
        
        AiReviewResponse suggestionResponse = new AiReviewResponse(
            "SUGGESTIONS", "Generated 2 suggestions", 85,
            Arrays.asList(finding1, finding2)
        );
        
        // Mock AI service for suggestions
        when(aiWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/review/suggestions")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AiReviewResponse.class)).thenReturn(Mono.just(suggestionResponse));
        
        // Generate suggestions
        mockMvc.perform(post("/reviews/pull-requests/" + testPullRequest.getId() + "/ai-suggestions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"TestFile.java\"]"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
        
        // Verify PR status changed to SUGGESTIONS_PENDING
        PullRequest updatedPR = pullRequestRepository.findById(testPullRequest.getId()).orElse(null);
        assertNotNull(updatedPR);
        assertEquals(PullRequest.ReviewStatus.SUGGESTIONS_PENDING, updatedPR.getReviewStatus());
        
        // Step 2: Get suggestions
        List<ReviewSuggestion> suggestions = reviewSuggestionRepository.findByPullRequestId(testPullRequest.getId());
        assertEquals(2, suggestions.size());
        
        mockMvc.perform(get("/reviews/pull-requests/" + testPullRequest.getId() + "/suggestions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[1].status").value("PENDING"));
        
        // Step 3: Approve one suggestion and reject another
        Long suggestionId1 = suggestions.get(0).getId();
        Long suggestionId2 = suggestions.get(1).getId();
        
        mockMvc.perform(post("/reviews/pull-requests/" + testPullRequest.getId() + "/suggestions/" + suggestionId1 + "/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approvedBy\": \"reviewer1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.approvedBy").value("reviewer1"));
        
        mockMvc.perform(post("/reviews/pull-requests/" + testPullRequest.getId() + "/suggestions/" + suggestionId2 + "/reject"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));
        
        // Step 4: Get suggestions summary
        mockMvc.perform(get("/reviews/pull-requests/" + testPullRequest.getId() + "/suggestions/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.pendingCount").value(0))
            .andExpect(jsonPath("$.approvedCount").value(1))
            .andExpect(jsonPath("$.rejectedCount").value(1));
        
        // Step 5: Apply approved suggestions
        mockMvc.perform(post("/reviews/pull-requests/" + testPullRequest.getId() + "/suggestions/apply-approved"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Suggestions applied successfully"))
            .andExpect(jsonPath("$.appliedCount").value(1))
            .andExpect(jsonPath("$.failedCount").value(0));
        
        // Verify applied suggestion status
        ReviewSuggestion appliedSuggestion = reviewSuggestionRepository.findById(suggestionId1).orElse(null);
        assertNotNull(appliedSuggestion);
        assertEquals(ReviewSuggestion.SuggestionStatus.APPLIED, appliedSuggestion.getStatus());
        assertNotNull(appliedSuggestion.getAppliedAt());
        
        // Step 6: Final AI review
        AiReviewResponse finalResponse = new AiReviewResponse(
            "APPROVED", "Code looks good after applying suggestions", 95,
            Arrays.asList()
        );
        
        // Mock AI service for final review
        when(requestBodyUriSpec.uri("/api/review/final")).thenReturn(requestBodySpec);
        when(responseSpec.bodyToMono(AiReviewResponse.class)).thenReturn(Mono.just(finalResponse));
        
        mockMvc.perform(post("/reviews/pull-requests/" + testPullRequest.getId() + "/ai-final-review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"TestFile.java\"]"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.decision").value("APPROVED"))
            .andExpect(jsonPath("$.summary").value("Code looks good after applying suggestions"));
        
        // Verify final PR status
        PullRequest finalPR = pullRequestRepository.findById(testPullRequest.getId()).orElse(null);
        assertNotNull(finalPR);
        assertEquals(PullRequest.ReviewStatus.APPROVED, finalPR.getReviewStatus());
    }
    
    @Test
    void shouldHandleBulkApprovalWorkflow() throws Exception {
        // Create test suggestions manually
        ReviewSuggestion suggestion1 = createTestSuggestion("File1.java", 10, 
            ReviewSuggestion.SuggestionType.SECURITY, ReviewSuggestion.Severity.HIGH);
        ReviewSuggestion suggestion2 = createTestSuggestion("File2.java", 20,
            ReviewSuggestion.SuggestionType.PERFORMANCE, ReviewSuggestion.Severity.MEDIUM);
        ReviewSuggestion suggestion3 = createTestSuggestion("File3.java", 30,
            ReviewSuggestion.SuggestionType.BUG, ReviewSuggestion.Severity.LOW);
        
        suggestion1 = reviewSuggestionRepository.save(suggestion1);
        suggestion2 = reviewSuggestionRepository.save(suggestion2);
        suggestion3 = reviewSuggestionRepository.save(suggestion3);
        
        // Update PR status
        testPullRequest.setReviewStatus(PullRequest.ReviewStatus.SUGGESTIONS_PENDING);
        pullRequestRepository.save(testPullRequest);
        
        // Bulk approve first two suggestions
        Map<String, Object> bulkApprovalData = Map.of(
            "suggestionIds", Arrays.asList(suggestion1.getId(), suggestion2.getId()),
            "approvedBy", "bulk-reviewer"
        );
        
        mockMvc.perform(post("/reviews/pull-requests/" + testPullRequest.getId() + "/suggestions/bulk-approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkApprovalData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].status").value("APPROVED"))
            .andExpect(jsonPath("$[1].status").value("APPROVED"));
        
        // Verify individual suggestions were approved
        ReviewSuggestion updated1 = reviewSuggestionRepository.findById(suggestion1.getId()).orElse(null);
        ReviewSuggestion updated2 = reviewSuggestionRepository.findById(suggestion2.getId()).orElse(null);
        ReviewSuggestion updated3 = reviewSuggestionRepository.findById(suggestion3.getId()).orElse(null);
        
        assertNotNull(updated1);
        assertNotNull(updated2);
        assertNotNull(updated3);
        
        assertEquals(ReviewSuggestion.SuggestionStatus.APPROVED, updated1.getStatus());
        assertEquals(ReviewSuggestion.SuggestionStatus.APPROVED, updated2.getStatus());
        assertEquals(ReviewSuggestion.SuggestionStatus.PENDING, updated3.getStatus());
        
        assertEquals("bulk-reviewer", updated1.getApprovedBy());
        assertEquals("bulk-reviewer", updated2.getApprovedBy());
        assertNull(updated3.getApprovedBy());
    }
    
    @Test
    void shouldFilterSuggestionsByParameters() throws Exception {
        // Create diverse test suggestions
        ReviewSuggestion securityHigh = createTestSuggestion("File1.java", 10,
            ReviewSuggestion.SuggestionType.SECURITY, ReviewSuggestion.Severity.HIGH);
        ReviewSuggestion performanceMed = createTestSuggestion("File2.java", 20,
            ReviewSuggestion.SuggestionType.PERFORMANCE, ReviewSuggestion.Severity.MEDIUM);
        ReviewSuggestion bugLow = createTestSuggestion("File3.java", 30,
            ReviewSuggestion.SuggestionType.BUG, ReviewSuggestion.Severity.LOW);
        
        securityHigh.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
        performanceMed.setStatus(ReviewSuggestion.SuggestionStatus.PENDING);
        bugLow.setStatus(ReviewSuggestion.SuggestionStatus.REJECTED);
        
        reviewSuggestionRepository.saveAll(Arrays.asList(securityHigh, performanceMed, bugLow));
        
        // Filter by status
        mockMvc.perform(get("/reviews/pull-requests/" + testPullRequest.getId() + "/suggestions")
                .param("status", "APPROVED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("APPROVED"));
        
        // Filter by severity
        mockMvc.perform(get("/reviews/pull-requests/" + testPullRequest.getId() + "/suggestions")
                .param("severity", "HIGH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].severity").value("HIGH"));
        
        // Filter by type
        mockMvc.perform(get("/reviews/pull-requests/" + testPullRequest.getId() + "/suggestions")
                .param("type", "PERFORMANCE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("PERFORMANCE"));
    }
    
    private ReviewSuggestion createTestSuggestion(String fileName, Integer lineNumber,
                                                 ReviewSuggestion.SuggestionType type,
                                                 ReviewSuggestion.Severity severity) {
        ReviewSuggestion suggestion = new ReviewSuggestion();
        suggestion.setPullRequest(testPullRequest);
        suggestion.setFileName(fileName);
        suggestion.setLineNumber(lineNumber);
        suggestion.setType(type);
        suggestion.setSeverity(severity);
        suggestion.setDescription("Test " + type + " issue");
        suggestion.setSuggestion("Fix the " + type + " issue");
        suggestion.setCodeSnippet("problematic code");
        suggestion.setProposedCode("fixed code");
        suggestion.setRuleId(type + "-001");
        suggestion.setStatus(ReviewSuggestion.SuggestionStatus.PENDING);
        suggestion.setCreatedAt(LocalDateTime.now());
        return suggestion;
    }
}
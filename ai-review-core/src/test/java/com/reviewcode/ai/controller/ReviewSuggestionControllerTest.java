package com.reviewcode.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewcode.ai.model.ReviewSuggestion;
import com.reviewcode.ai.service.ReviewSuggestionService;
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
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(ReviewSuggestionController.class)
@ActiveProfiles("test")
class ReviewSuggestionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ReviewSuggestionService reviewSuggestionService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private ReviewSuggestion testSuggestion;
    
    @BeforeEach
    void setUp() {
        testSuggestion = new ReviewSuggestion();
        testSuggestion.setId(1L);
        testSuggestion.setFileName("TestFile.java");
        testSuggestion.setLineNumber(42);
        testSuggestion.setType(ReviewSuggestion.SuggestionType.SECURITY);
        testSuggestion.setSeverity(ReviewSuggestion.Severity.HIGH);
        testSuggestion.setDescription("Security issue");
        testSuggestion.setSuggestion("Fix the security issue");
        testSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.PENDING);
        testSuggestion.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void shouldGetAllSuggestions() throws Exception {
        List<ReviewSuggestion> suggestions = Arrays.asList(testSuggestion);
        when(reviewSuggestionService.getAllSuggestions(1L)).thenReturn(suggestions);
        
        mockMvc.perform(get("/reviews/pull-requests/1/suggestions"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].fileName").value("TestFile.java"))
            .andExpect(jsonPath("$[0].lineNumber").value(42))
            .andExpect(jsonPath("$[0].type").value("SECURITY"))
            .andExpect(jsonPath("$[0].severity").value("HIGH"))
            .andExpect(jsonPath("$[0].status").value("PENDING"));
        
        verify(reviewSuggestionService).getAllSuggestions(1L);
    }
    
    @Test
    void shouldGetSuggestionsByStatus() throws Exception {
        List<ReviewSuggestion> suggestions = Arrays.asList(testSuggestion);
        when(reviewSuggestionService.getSuggestionsByStatus(1L, ReviewSuggestion.SuggestionStatus.PENDING))
            .thenReturn(suggestions);
        
        mockMvc.perform(get("/reviews/pull-requests/1/suggestions")
                .param("status", "PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].status").value("PENDING"));
        
        verify(reviewSuggestionService).getSuggestionsByStatus(1L, ReviewSuggestion.SuggestionStatus.PENDING);
    }
    
    @Test
    void shouldGetSuggestionsBySeverity() throws Exception {
        List<ReviewSuggestion> suggestions = Arrays.asList(testSuggestion);
        when(reviewSuggestionService.getSuggestionsBySeverity(1L, ReviewSuggestion.Severity.HIGH))
            .thenReturn(suggestions);
        
        mockMvc.perform(get("/reviews/pull-requests/1/suggestions")
                .param("severity", "HIGH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].severity").value("HIGH"));
        
        verify(reviewSuggestionService).getSuggestionsBySeverity(1L, ReviewSuggestion.Severity.HIGH);
    }
    
    @Test
    void shouldGetSuggestionsByType() throws Exception {
        List<ReviewSuggestion> suggestions = Arrays.asList(testSuggestion);
        when(reviewSuggestionService.getSuggestionsByType(1L, ReviewSuggestion.SuggestionType.SECURITY))
            .thenReturn(suggestions);
        
        mockMvc.perform(get("/reviews/pull-requests/1/suggestions")
                .param("type", "SECURITY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].type").value("SECURITY"));
        
        verify(reviewSuggestionService).getSuggestionsByType(1L, ReviewSuggestion.SuggestionType.SECURITY);
    }
    
    @Test
    void shouldGetSingleSuggestion() throws Exception {
        when(reviewSuggestionService.getSuggestion(1L, 1L)).thenReturn(Optional.of(testSuggestion));
        
        mockMvc.perform(get("/reviews/pull-requests/1/suggestions/1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.fileName").value("TestFile.java"));
        
        verify(reviewSuggestionService).getSuggestion(1L, 1L);
    }
    
    @Test
    void shouldReturnNotFoundWhenSuggestionDoesNotExist() throws Exception {
        when(reviewSuggestionService.getSuggestion(1L, 999L)).thenReturn(Optional.empty());
        
        mockMvc.perform(get("/reviews/pull-requests/1/suggestions/999"))
            .andExpect(status().isNotFound());
        
        verify(reviewSuggestionService).getSuggestion(1L, 999L);
    }
    
    @Test
    void shouldApproveSuggestion() throws Exception {
        testSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
        testSuggestion.setApprovedBy("test-user");
        testSuggestion.setApprovedAt(LocalDateTime.now());
        
        when(reviewSuggestionService.approveSuggestion(1L, 1L, "test-user"))
            .thenReturn(testSuggestion);
        
        Map<String, String> approvalData = Map.of("approvedBy", "test-user");
        
        mockMvc.perform(post("/reviews/pull-requests/1/suggestions/1/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.approvedBy").value("test-user"));
        
        verify(reviewSuggestionService).approveSuggestion(1L, 1L, "test-user");
    }
    
    @Test
    void shouldApproveSuggestionWithDefaultUser() throws Exception {
        testSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
        testSuggestion.setApprovedBy("system");
        
        when(reviewSuggestionService.approveSuggestion(1L, 1L, "system"))
            .thenReturn(testSuggestion);
        
        mockMvc.perform(post("/reviews/pull-requests/1/suggestions/1/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
        
        verify(reviewSuggestionService).approveSuggestion(1L, 1L, "system");
    }
    
    @Test
    void shouldReturnNotFoundWhenApprovingNonExistentSuggestion() throws Exception {
        when(reviewSuggestionService.approveSuggestion(1L, 999L, "test-user"))
            .thenThrow(new IllegalArgumentException("Suggestion not found"));
        
        Map<String, String> approvalData = Map.of("approvedBy", "test-user");
        
        mockMvc.perform(post("/reviews/pull-requests/1/suggestions/999/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalData)))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldRejectSuggestion() throws Exception {
        testSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.REJECTED);
        
        when(reviewSuggestionService.rejectSuggestion(1L, 1L))
            .thenReturn(testSuggestion);
        
        mockMvc.perform(post("/reviews/pull-requests/1/suggestions/1/reject"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));
        
        verify(reviewSuggestionService).rejectSuggestion(1L, 1L);
    }
    
    @Test
    void shouldBulkApproveSuggestions() throws Exception {
        ReviewSuggestion suggestion2 = new ReviewSuggestion();
        suggestion2.setId(2L);
        suggestion2.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
        
        testSuggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
        
        List<ReviewSuggestion> approvedSuggestions = Arrays.asList(testSuggestion, suggestion2);
        
        when(reviewSuggestionService.bulkApproveSuggestions(eq(1L), eq(Arrays.asList(1L, 2L)), eq("test-user")))
            .thenReturn(approvedSuggestions);
        
        Map<String, Object> requestData = Map.of(
            "suggestionIds", Arrays.asList(1L, 2L),
            "approvedBy", "test-user"
        );
        
        mockMvc.perform(post("/reviews/pull-requests/1/suggestions/bulk-approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].status").value("APPROVED"))
            .andExpect(jsonPath("$[1].status").value("APPROVED"));
        
        verify(reviewSuggestionService).bulkApproveSuggestions(1L, Arrays.asList(1L, 2L), "test-user");
    }
    
    @Test
    void shouldApplyApprovedSuggestions() throws Exception {
        ReviewSuggestionService.ApplicationResult result = 
            new ReviewSuggestionService.ApplicationResult(2, 0, Collections.emptyList());
        
        when(reviewSuggestionService.applyApprovedSuggestions(1L))
            .thenReturn(Mono.just(result));
        
        mockMvc.perform(post("/reviews/pull-requests/1/suggestions/apply-approved"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Suggestions applied successfully"))
            .andExpect(jsonPath("$.appliedCount").value(2))
            .andExpect(jsonPath("$.failedCount").value(0))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors", hasSize(0)));
        
        verify(reviewSuggestionService).applyApprovedSuggestions(1L);
    }
    
    @Test
    void shouldHandleErrorWhenApplyingApprovedSuggestions() throws Exception {
        when(reviewSuggestionService.applyApprovedSuggestions(1L))
            .thenReturn(Mono.error(new RuntimeException("Application failed")));
        
        mockMvc.perform(post("/reviews/pull-requests/1/suggestions/apply-approved"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Failed to apply suggestions"));
        
        verify(reviewSuggestionService).applyApprovedSuggestions(1L);
    }
    
    @Test
    void shouldGetSuggestionsSummary() throws Exception {
        Map<String, Object> summary = Map.of(
            "total", 5,
            "pendingCount", 2L,
            "approvedCount", 2L,
            "appliedCount", 1L,
            "rejectedCount", 0L,
            "statusCounts", Map.of(
                ReviewSuggestion.SuggestionStatus.PENDING, 2L,
                ReviewSuggestion.SuggestionStatus.APPROVED, 2L,
                ReviewSuggestion.SuggestionStatus.APPLIED, 1L
            ),
            "severityCounts", Map.of(
                ReviewSuggestion.Severity.HIGH, 3L,
                ReviewSuggestion.Severity.MEDIUM, 2L
            ),
            "typeCounts", Map.of(
                ReviewSuggestion.SuggestionType.SECURITY, 2L,
                ReviewSuggestion.SuggestionType.PERFORMANCE, 3L
            )
        );
        
        when(reviewSuggestionService.getSuggestionsSummary(1L)).thenReturn(summary);
        
        mockMvc.perform(get("/reviews/pull-requests/1/suggestions/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(5))
            .andExpect(jsonPath("$.pendingCount").value(2))
            .andExpect(jsonPath("$.approvedCount").value(2))
            .andExpect(jsonPath("$.appliedCount").value(1))
            .andExpect(jsonPath("$.rejectedCount").value(0));
        
        verify(reviewSuggestionService).getSuggestionsSummary(1L);
    }
}
package com.reviewcode.ai.controller;

import com.reviewcode.ai.model.ReviewSuggestion;
import com.reviewcode.ai.service.ReviewSuggestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews/pull-requests/{pullRequestId}/suggestions")
@CrossOrigin(origins = "*")
public class ReviewSuggestionController {
    
    private final ReviewSuggestionService reviewSuggestionService;
    
    @Autowired
    public ReviewSuggestionController(ReviewSuggestionService reviewSuggestionService) {
        this.reviewSuggestionService = reviewSuggestionService;
    }
    
    @GetMapping
    public ResponseEntity<List<ReviewSuggestion>> getSuggestions(
            @PathVariable Long pullRequestId,
            @RequestParam(required = false) ReviewSuggestion.SuggestionStatus status,
            @RequestParam(required = false) ReviewSuggestion.Severity severity,
            @RequestParam(required = false) ReviewSuggestion.SuggestionType type) {
        
        List<ReviewSuggestion> suggestions;
        
        if (status != null) {
            suggestions = reviewSuggestionService.getSuggestionsByStatus(pullRequestId, status);
        } else if (severity != null) {
            suggestions = reviewSuggestionService.getSuggestionsBySeverity(pullRequestId, severity);
        } else if (type != null) {
            suggestions = reviewSuggestionService.getSuggestionsByType(pullRequestId, type);
        } else {
            suggestions = reviewSuggestionService.getAllSuggestions(pullRequestId);
        }
        
        return ResponseEntity.ok(suggestions);
    }
    
    @GetMapping("/{suggestionId}")
    public ResponseEntity<ReviewSuggestion> getSuggestion(
            @PathVariable Long pullRequestId,
            @PathVariable Long suggestionId) {
        
        return reviewSuggestionService.getSuggestion(pullRequestId, suggestionId)
            .map(suggestion -> ResponseEntity.ok(suggestion))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{suggestionId}/approve")
    public ResponseEntity<ReviewSuggestion> approveSuggestion(
            @PathVariable Long pullRequestId,
            @PathVariable Long suggestionId,
            @RequestBody(required = false) Map<String, String> approvalData) {
        
        String approvedBy = approvalData != null ? approvalData.get("approvedBy") : "system";
        
        try {
            ReviewSuggestion approved = reviewSuggestionService.approveSuggestion(
                pullRequestId, suggestionId, approvedBy);
            return ResponseEntity.ok(approved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{suggestionId}/reject")
    public ResponseEntity<ReviewSuggestion> rejectSuggestion(
            @PathVariable Long pullRequestId,
            @PathVariable Long suggestionId) {
        
        try {
            ReviewSuggestion rejected = reviewSuggestionService.rejectSuggestion(pullRequestId, suggestionId);
            return ResponseEntity.ok(rejected);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/bulk-approve")
    public ResponseEntity<List<ReviewSuggestion>> bulkApproveSuggestions(
            @PathVariable Long pullRequestId,
            @RequestBody Map<String, Object> requestData) {
        
        @SuppressWarnings("unchecked")
        List<Long> suggestionIds = (List<Long>) requestData.get("suggestionIds");
        String approvedBy = (String) requestData.getOrDefault("approvedBy", "system");
        
        try {
            List<ReviewSuggestion> approved = reviewSuggestionService.bulkApproveSuggestions(
                pullRequestId, suggestionIds, approvedBy);
            return ResponseEntity.ok(approved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/apply-approved")
    public Mono<ResponseEntity<Map<String, Object>>> applyApprovedSuggestions(
            @PathVariable Long pullRequestId) {
        
        return reviewSuggestionService.applyApprovedSuggestions(pullRequestId)
            .map(result -> ResponseEntity.ok(Map.of(
                "message", "Suggestions applied successfully",
                "appliedCount", result.getAppliedCount(),
                "failedCount", result.getFailedCount(),
                "errors", result.getErrors()
            )))
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to apply suggestions")));
    }
    
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSuggestionsSummary(@PathVariable Long pullRequestId) {
        Map<String, Object> summary = reviewSuggestionService.getSuggestionsSummary(pullRequestId);
        return ResponseEntity.ok(summary);
    }
}
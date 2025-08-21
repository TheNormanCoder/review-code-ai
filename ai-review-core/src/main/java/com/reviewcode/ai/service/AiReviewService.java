package com.reviewcode.ai.service;

import com.reviewcode.ai.config.AiConfiguration;
import com.reviewcode.ai.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AiReviewService {
    
    private final WebClient aiWebClient;
    private final AiConfiguration aiConfig;
    private final ReviewSuggestionService reviewSuggestionService;
    
    @Autowired
    public AiReviewService(WebClient aiWebClient, 
                          AiConfiguration aiConfig,
                          ReviewSuggestionService reviewSuggestionService) {
        this.aiWebClient = aiWebClient;
        this.aiConfig = aiConfig;
        this.reviewSuggestionService = reviewSuggestionService;
    }
    
    public Mono<List<ReviewSuggestion>> generateSuggestions(PullRequest pullRequest, List<String> filesToReview) {
        Map<String, Object> reviewRequest = Map.of(
            "pullRequestId", pullRequest.getId(),
            "title", pullRequest.getTitle(),
            "description", pullRequest.getDescription(),
            "files", filesToReview,
            "author", pullRequest.getAuthor(),
            "repositoryUrl", pullRequest.getRepositoryUrl(),
            "mode", "suggestions"
        );
        
        return aiWebClient
            .post()
            .uri("/api/review/suggestions")
            .bodyValue(reviewRequest)
            .retrieve()
            .bodyToMono(AiReviewResponse.class)
            .timeout(Duration.ofMillis(aiConfig.getMcp().getTimeout()))
            .map(response -> reviewSuggestionService.createSuggestionsFromAiResponse(pullRequest, response))
            .onErrorResume(error -> {
                System.err.println("AI Suggestions generation failed: " + error.getMessage());
                return Mono.just(List.of());
            });
    }
    
    public Mono<CodeReview> performFinalReview(PullRequest pullRequest, List<String> filesToReview) {
        Map<String, Object> reviewRequest = Map.of(
            "pullRequestId", pullRequest.getId(),
            "title", pullRequest.getTitle(),
            "description", pullRequest.getDescription(),
            "files", filesToReview,
            "author", pullRequest.getAuthor(),
            "repositoryUrl", pullRequest.getRepositoryUrl(),
            "mode", "final-review"
        );
        
        return aiWebClient
            .post()
            .uri("/api/review/final")
            .bodyValue(reviewRequest)
            .retrieve()
            .bodyToMono(AiReviewResponse.class)
            .timeout(Duration.ofMillis(aiConfig.getMcp().getTimeout()))
            .map(response -> mapToCodeReview(pullRequest, response))
            .onErrorResume(error -> {
                CodeReview errorReview = new CodeReview();
                errorReview.setPullRequest(pullRequest);
                errorReview.setReviewer("AI-MCP");
                errorReview.setReviewerType(CodeReview.ReviewerType.AI_MCP);
                errorReview.setDecision(CodeReview.ReviewDecision.REJECTED);
                errorReview.setSummary("AI Final Review failed: " + error.getMessage());
                errorReview.setCreatedAt(LocalDateTime.now());
                return Mono.just(errorReview);
            });
    }
    
    @Deprecated
    public Mono<CodeReview> performAiReview(PullRequest pullRequest, List<String> filesToReview) {
        return performFinalReview(pullRequest, filesToReview);
    }
    
    private CodeReview mapToCodeReview(PullRequest pullRequest, AiReviewResponse response) {
        CodeReview review = new CodeReview();
        review.setPullRequest(pullRequest);
        review.setReviewer("AI-MCP");
        review.setReviewerType(CodeReview.ReviewerType.AI_MCP);
        review.setDecision(mapDecision(response.getDecision()));
        review.setSummary(response.getSummary());
        review.setOverallScore(response.getScore());
        review.setCreatedAt(LocalDateTime.now());
        review.setCompletedAt(LocalDateTime.now());
        
        List<ReviewFinding> findings = response.getFindings().stream()
            .map(finding -> mapToReviewFinding(review, finding))
            .toList();
        
        review.setFindings(findings);
        return review;
    }
    
    private ReviewFinding mapToReviewFinding(CodeReview review, AiReviewResponse.Finding finding) {
        ReviewFinding reviewFinding = new ReviewFinding();
        reviewFinding.setCodeReview(review);
        reviewFinding.setFileName(finding.getFileName());
        reviewFinding.setLineNumber(finding.getLineNumber());
        reviewFinding.setType(mapFindingType(finding.getType()));
        reviewFinding.setSeverity(mapSeverity(finding.getSeverity()));
        reviewFinding.setDescription(finding.getDescription());
        reviewFinding.setSuggestion(finding.getSuggestion());
        reviewFinding.setCodeSnippet(finding.getCodeSnippet());
        reviewFinding.setRuleId(finding.getRuleId());
        return reviewFinding;
    }
    
    private CodeReview.ReviewDecision mapDecision(String decision) {
        return switch (decision.toUpperCase()) {
            case "APPROVED" -> CodeReview.ReviewDecision.APPROVED;
            case "CHANGES_REQUESTED" -> CodeReview.ReviewDecision.CHANGES_REQUESTED;
            case "REJECTED" -> CodeReview.ReviewDecision.REJECTED;
            default -> CodeReview.ReviewDecision.PENDING;
        };
    }
    
    private ReviewFinding.FindingType mapFindingType(String type) {
        return switch (type.toUpperCase()) {
            case "SECURITY" -> ReviewFinding.FindingType.SECURITY;
            case "PERFORMANCE" -> ReviewFinding.FindingType.PERFORMANCE;
            case "BUG" -> ReviewFinding.FindingType.BUG;
            case "MAINTAINABILITY" -> ReviewFinding.FindingType.MAINTAINABILITY;
            case "DOCUMENTATION" -> ReviewFinding.FindingType.DOCUMENTATION;
            case "BEST_PRACTICE" -> ReviewFinding.FindingType.BEST_PRACTICE;
            case "ARCHITECTURE" -> ReviewFinding.FindingType.ARCHITECTURE;
            case "DESIGN_PATTERN" -> ReviewFinding.FindingType.DESIGN_PATTERN;
            case "SOLID_PRINCIPLES" -> ReviewFinding.FindingType.SOLID_PRINCIPLES;
            case "DEPENDENCY_INJECTION" -> ReviewFinding.FindingType.DEPENDENCY_INJECTION;
            case "SEPARATION_OF_CONCERNS" -> ReviewFinding.FindingType.SEPARATION_OF_CONCERNS;
            default -> ReviewFinding.FindingType.CODE_STYLE;
        };
    }
    
    private ReviewFinding.Severity mapSeverity(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> ReviewFinding.Severity.CRITICAL;
            case "HIGH" -> ReviewFinding.Severity.HIGH;
            case "MEDIUM" -> ReviewFinding.Severity.MEDIUM;
            case "LOW" -> ReviewFinding.Severity.LOW;
            default -> ReviewFinding.Severity.INFO;
        };
    }
}
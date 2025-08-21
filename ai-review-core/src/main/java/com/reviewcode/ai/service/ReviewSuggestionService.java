package com.reviewcode.ai.service;

import com.reviewcode.ai.model.PullRequest;
import com.reviewcode.ai.model.ReviewSuggestion;
import com.reviewcode.ai.repository.PullRequestRepository;
import com.reviewcode.ai.repository.ReviewSuggestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewSuggestionService {
    
    private final ReviewSuggestionRepository reviewSuggestionRepository;
    private final PullRequestRepository pullRequestRepository;
    
    @Autowired
    public ReviewSuggestionService(ReviewSuggestionRepository reviewSuggestionRepository,
                                  PullRequestRepository pullRequestRepository) {
        this.reviewSuggestionRepository = reviewSuggestionRepository;
        this.pullRequestRepository = pullRequestRepository;
    }
    
    public List<ReviewSuggestion> getAllSuggestions(Long pullRequestId) {
        return reviewSuggestionRepository.findByPullRequestId(pullRequestId);
    }
    
    public List<ReviewSuggestion> getSuggestionsByStatus(Long pullRequestId, ReviewSuggestion.SuggestionStatus status) {
        return reviewSuggestionRepository.findByPullRequestIdAndStatus(pullRequestId, status);
    }
    
    public List<ReviewSuggestion> getSuggestionsBySeverity(Long pullRequestId, ReviewSuggestion.Severity severity) {
        return reviewSuggestionRepository.findByPullRequestIdAndSeverity(pullRequestId, severity);
    }
    
    public List<ReviewSuggestion> getSuggestionsByType(Long pullRequestId, ReviewSuggestion.SuggestionType type) {
        return reviewSuggestionRepository.findByPullRequestIdAndType(pullRequestId, type);
    }
    
    public Optional<ReviewSuggestion> getSuggestion(Long pullRequestId, Long suggestionId) {
        return reviewSuggestionRepository.findById(suggestionId)
            .filter(suggestion -> suggestion.getPullRequest().getId().equals(pullRequestId));
    }
    
    public ReviewSuggestion approveSuggestion(Long pullRequestId, Long suggestionId, String approvedBy) {
        ReviewSuggestion suggestion = reviewSuggestionRepository.findById(suggestionId)
            .filter(s -> s.getPullRequest().getId().equals(pullRequestId))
            .orElseThrow(() -> new IllegalArgumentException("Suggestion not found"));
        
        if (suggestion.getStatus() != ReviewSuggestion.SuggestionStatus.PENDING) {
            throw new IllegalArgumentException("Suggestion is not in pending state");
        }
        
        suggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
        suggestion.setApprovedBy(approvedBy);
        suggestion.setApprovedAt(LocalDateTime.now());
        
        return reviewSuggestionRepository.save(suggestion);
    }
    
    public ReviewSuggestion rejectSuggestion(Long pullRequestId, Long suggestionId) {
        ReviewSuggestion suggestion = reviewSuggestionRepository.findById(suggestionId)
            .filter(s -> s.getPullRequest().getId().equals(pullRequestId))
            .orElseThrow(() -> new IllegalArgumentException("Suggestion not found"));
        
        if (suggestion.getStatus() != ReviewSuggestion.SuggestionStatus.PENDING) {
            throw new IllegalArgumentException("Suggestion is not in pending state");
        }
        
        suggestion.setStatus(ReviewSuggestion.SuggestionStatus.REJECTED);
        
        return reviewSuggestionRepository.save(suggestion);
    }
    
    public List<ReviewSuggestion> bulkApproveSuggestions(Long pullRequestId, List<Long> suggestionIds, String approvedBy) {
        List<ReviewSuggestion> suggestions = reviewSuggestionRepository.findAllById(suggestionIds);
        
        List<ReviewSuggestion> validSuggestions = suggestions.stream()
            .filter(s -> s.getPullRequest().getId().equals(pullRequestId))
            .filter(s -> s.getStatus() == ReviewSuggestion.SuggestionStatus.PENDING)
            .collect(Collectors.toList());
        
        if (validSuggestions.size() != suggestionIds.size()) {
            throw new IllegalArgumentException("Some suggestions are not found or not in pending state");
        }
        
        LocalDateTime now = LocalDateTime.now();
        validSuggestions.forEach(suggestion -> {
            suggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPROVED);
            suggestion.setApprovedBy(approvedBy);
            suggestion.setApprovedAt(now);
        });
        
        return reviewSuggestionRepository.saveAll(validSuggestions);
    }
    
    public Mono<ApplicationResult> applyApprovedSuggestions(Long pullRequestId) {
        List<ReviewSuggestion> approvedSuggestions = reviewSuggestionRepository.findByPullRequestIdAndStatus(
            pullRequestId, ReviewSuggestion.SuggestionStatus.APPROVED);
        
        if (approvedSuggestions.isEmpty()) {
            return Mono.just(new ApplicationResult(0, 0, Collections.emptyList()));
        }
        
        return Mono.fromCallable(() -> {
            int appliedCount = 0;
            int failedCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (ReviewSuggestion suggestion : approvedSuggestions) {
                try {
                    applySuggestion(suggestion);
                    suggestion.setStatus(ReviewSuggestion.SuggestionStatus.APPLIED);
                    suggestion.setAppliedAt(LocalDateTime.now());
                    reviewSuggestionRepository.save(suggestion);
                    appliedCount++;
                } catch (Exception e) {
                    failedCount++;
                    errors.add("Failed to apply suggestion " + suggestion.getId() + ": " + e.getMessage());
                }
            }
            
            updatePullRequestAfterSuggestionApplication(pullRequestId);
            
            return new ApplicationResult(appliedCount, failedCount, errors);
        });
    }
    
    private void applySuggestion(ReviewSuggestion suggestion) {
        if (suggestion.getProposedCode() != null && !suggestion.getProposedCode().isEmpty()) {
            String fileName = suggestion.getFileName();
            Integer lineNumber = suggestion.getLineNumber();
            String proposedCode = suggestion.getProposedCode();
            
            System.out.println("Applying suggestion to " + fileName + " at line " + lineNumber);
            System.out.println("Proposed code: " + proposedCode);
        }
    }
    
    private void updatePullRequestAfterSuggestionApplication(Long pullRequestId) {
        Optional<PullRequest> pullRequestOpt = pullRequestRepository.findById(pullRequestId);
        
        if (pullRequestOpt.isPresent()) {
            PullRequest pullRequest = pullRequestOpt.get();
            
            long pendingCount = reviewSuggestionRepository.countByPullRequestIdAndStatus(
                pullRequestId, ReviewSuggestion.SuggestionStatus.PENDING);
            
            if (pendingCount == 0) {
                pullRequest.setReviewStatus(PullRequest.ReviewStatus.IN_PROGRESS);
                pullRequest.setUpdatedAt(LocalDateTime.now());
                pullRequestRepository.save(pullRequest);
            }
        }
    }
    
    public Map<String, Object> getSuggestionsSummary(Long pullRequestId) {
        List<ReviewSuggestion> allSuggestions = reviewSuggestionRepository.findByPullRequestId(pullRequestId);
        
        Map<ReviewSuggestion.SuggestionStatus, Long> statusCounts = allSuggestions.stream()
            .collect(Collectors.groupingBy(ReviewSuggestion::getStatus, Collectors.counting()));
        
        Map<ReviewSuggestion.Severity, Long> severityCounts = allSuggestions.stream()
            .collect(Collectors.groupingBy(ReviewSuggestion::getSeverity, Collectors.counting()));
        
        Map<ReviewSuggestion.SuggestionType, Long> typeCounts = allSuggestions.stream()
            .collect(Collectors.groupingBy(ReviewSuggestion::getType, Collectors.counting()));
        
        return Map.of(
            "total", allSuggestions.size(),
            "statusCounts", statusCounts,
            "severityCounts", severityCounts,
            "typeCounts", typeCounts,
            "pendingCount", statusCounts.getOrDefault(ReviewSuggestion.SuggestionStatus.PENDING, 0L),
            "approvedCount", statusCounts.getOrDefault(ReviewSuggestion.SuggestionStatus.APPROVED, 0L),
            "appliedCount", statusCounts.getOrDefault(ReviewSuggestion.SuggestionStatus.APPLIED, 0L),
            "rejectedCount", statusCounts.getOrDefault(ReviewSuggestion.SuggestionStatus.REJECTED, 0L)
        );
    }
    
    public List<ReviewSuggestion> createSuggestionsFromAiResponse(PullRequest pullRequest, AiReviewResponse aiResponse) {
        List<ReviewSuggestion> suggestions = new ArrayList<>();
        
        for (AiReviewResponse.Finding finding : aiResponse.getFindings()) {
            ReviewSuggestion suggestion = new ReviewSuggestion();
            suggestion.setPullRequest(pullRequest);
            suggestion.setFileName(finding.getFileName());
            suggestion.setLineNumber(finding.getLineNumber());
            suggestion.setType(mapFindingTypeToSuggestionType(finding.getType()));
            suggestion.setSeverity(mapSeverityToSuggestionSeverity(finding.getSeverity()));
            suggestion.setDescription(finding.getDescription());
            suggestion.setSuggestion(finding.getSuggestion());
            suggestion.setCodeSnippet(finding.getCodeSnippet());
            suggestion.setProposedCode(finding.getProposedCode());
            suggestion.setRuleId(finding.getRuleId());
            suggestion.setStatus(ReviewSuggestion.SuggestionStatus.PENDING);
            suggestion.setCreatedAt(LocalDateTime.now());
            
            suggestions.add(suggestion);
        }
        
        return reviewSuggestionRepository.saveAll(suggestions);
    }
    
    private ReviewSuggestion.SuggestionType mapFindingTypeToSuggestionType(String type) {
        return switch (type.toUpperCase()) {
            case "SECURITY" -> ReviewSuggestion.SuggestionType.SECURITY;
            case "PERFORMANCE" -> ReviewSuggestion.SuggestionType.PERFORMANCE;
            case "BUG" -> ReviewSuggestion.SuggestionType.BUG;
            case "MAINTAINABILITY" -> ReviewSuggestion.SuggestionType.MAINTAINABILITY;
            case "DOCUMENTATION" -> ReviewSuggestion.SuggestionType.DOCUMENTATION;
            case "BEST_PRACTICE" -> ReviewSuggestion.SuggestionType.BEST_PRACTICE;
            case "ARCHITECTURE" -> ReviewSuggestion.SuggestionType.ARCHITECTURE;
            case "DESIGN_PATTERN" -> ReviewSuggestion.SuggestionType.DESIGN_PATTERN;
            case "SOLID_PRINCIPLES" -> ReviewSuggestion.SuggestionType.SOLID_PRINCIPLES;
            case "DEPENDENCY_INJECTION" -> ReviewSuggestion.SuggestionType.DEPENDENCY_INJECTION;
            case "SEPARATION_OF_CONCERNS" -> ReviewSuggestion.SuggestionType.SEPARATION_OF_CONCERNS;
            default -> ReviewSuggestion.SuggestionType.CODE_STYLE;
        };
    }
    
    private ReviewSuggestion.Severity mapSeverityToSuggestionSeverity(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> ReviewSuggestion.Severity.CRITICAL;
            case "HIGH" -> ReviewSuggestion.Severity.HIGH;
            case "MEDIUM" -> ReviewSuggestion.Severity.MEDIUM;
            case "LOW" -> ReviewSuggestion.Severity.LOW;
            default -> ReviewSuggestion.Severity.INFO;
        };
    }
    
    public static class ApplicationResult {
        private final int appliedCount;
        private final int failedCount;
        private final List<String> errors;
        
        public ApplicationResult(int appliedCount, int failedCount, List<String> errors) {
            this.appliedCount = appliedCount;
            this.failedCount = failedCount;
            this.errors = errors;
        }
        
        public int getAppliedCount() { return appliedCount; }
        public int getFailedCount() { return failedCount; }
        public List<String> getErrors() { return errors; }
    }
}
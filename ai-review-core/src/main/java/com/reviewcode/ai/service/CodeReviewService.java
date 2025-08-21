package com.reviewcode.ai.service;

import com.reviewcode.ai.model.CodeReview;
import com.reviewcode.ai.model.PullRequest;
import com.reviewcode.ai.model.ReviewSuggestion;
import com.reviewcode.ai.repository.CodeReviewRepository;
import com.reviewcode.ai.repository.PullRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CodeReviewService {
    
    private final PullRequestRepository pullRequestRepository;
    private final CodeReviewRepository codeReviewRepository;
    private final AiReviewService aiReviewService;
    private final ReviewSuggestionService reviewSuggestionService;
    
    @Autowired
    public CodeReviewService(PullRequestRepository pullRequestRepository,
                           CodeReviewRepository codeReviewRepository,
                           AiReviewService aiReviewService,
                           ReviewSuggestionService reviewSuggestionService) {
        this.pullRequestRepository = pullRequestRepository;
        this.codeReviewRepository = codeReviewRepository;
        this.aiReviewService = aiReviewService;
        this.reviewSuggestionService = reviewSuggestionService;
    }
    
    public PullRequest createPullRequest(PullRequest pullRequest) {
        pullRequest.setCreatedAt(LocalDateTime.now());
        pullRequest.setUpdatedAt(LocalDateTime.now());
        pullRequest.setStatus(PullRequest.PullRequestStatus.OPEN);
        pullRequest.setReviewStatus(PullRequest.ReviewStatus.PENDING);
        
        return pullRequestRepository.save(pullRequest);
    }
    
    public Optional<PullRequest> getPullRequest(Long id) {
        return pullRequestRepository.findById(id);
    }
    
    public List<PullRequest> getAllPullRequests() {
        return pullRequestRepository.findAll();
    }
    
    public List<PullRequest> getPullRequestsByAuthor(String author) {
        return pullRequestRepository.findByAuthor(author);
    }
    
    public List<PullRequest> getPullRequestsByStatus(PullRequest.PullRequestStatus status) {
        return pullRequestRepository.findByStatus(status);
    }
    
    public Mono<List<ReviewSuggestion>> triggerAiSuggestions(Long pullRequestId, List<String> filesToReview) {
        Optional<PullRequest> pullRequestOpt = pullRequestRepository.findById(pullRequestId);
        
        if (pullRequestOpt.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Pull request not found"));
        }
        
        PullRequest pullRequest = pullRequestOpt.get();
        pullRequest.setReviewStatus(PullRequest.ReviewStatus.IN_PROGRESS);
        pullRequestRepository.save(pullRequest);
        
        return aiReviewService.generateSuggestions(pullRequest, filesToReview)
            .map(suggestions -> {
                if (!suggestions.isEmpty()) {
                    pullRequest.setReviewStatus(PullRequest.ReviewStatus.SUGGESTIONS_PENDING);
                    pullRequestRepository.save(pullRequest);
                }
                return suggestions;
            });
    }
    
    public Mono<CodeReview> triggerFinalReview(Long pullRequestId, List<String> filesToReview) {
        Optional<PullRequest> pullRequestOpt = pullRequestRepository.findById(pullRequestId);
        
        if (pullRequestOpt.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Pull request not found"));
        }
        
        PullRequest pullRequest = pullRequestOpt.get();
        pullRequest.setReviewStatus(PullRequest.ReviewStatus.IN_PROGRESS);
        pullRequestRepository.save(pullRequest);
        
        return aiReviewService.performFinalReview(pullRequest, filesToReview)
            .map(review -> {
                CodeReview savedReview = codeReviewRepository.save(review);
                updatePullRequestAfterReview(pullRequest, savedReview);
                return savedReview;
            });
    }
    
    @Deprecated
    public Mono<CodeReview> triggerAiReview(Long pullRequestId, List<String> filesToReview) {
        return triggerFinalReview(pullRequestId, filesToReview);
    }
    
    public CodeReview addHumanReview(Long pullRequestId, CodeReview review) {
        Optional<PullRequest> pullRequestOpt = pullRequestRepository.findById(pullRequestId);
        
        if (pullRequestOpt.isEmpty()) {
            throw new IllegalArgumentException("Pull request not found");
        }
        
        PullRequest pullRequest = pullRequestOpt.get();
        review.setPullRequest(pullRequest);
        review.setReviewerType(CodeReview.ReviewerType.HUMAN);
        review.setCreatedAt(LocalDateTime.now());
        
        CodeReview savedReview = codeReviewRepository.save(review);
        updatePullRequestAfterReview(pullRequest, savedReview);
        
        return savedReview;
    }
    
    public List<CodeReview> getReviewsForPullRequest(Long pullRequestId) {
        return codeReviewRepository.findByPullRequestId(pullRequestId);
    }
    
    private void updatePullRequestAfterReview(PullRequest pullRequest, CodeReview review) {
        List<CodeReview> allReviews = codeReviewRepository.findByPullRequestId(pullRequest.getId());
        
        boolean hasApproval = allReviews.stream()
            .anyMatch(r -> r.getDecision() == CodeReview.ReviewDecision.APPROVED);
        
        boolean hasRejection = allReviews.stream()
            .anyMatch(r -> r.getDecision() == CodeReview.ReviewDecision.REJECTED);
        
        boolean hasChangesRequested = allReviews.stream()
            .anyMatch(r -> r.getDecision() == CodeReview.ReviewDecision.CHANGES_REQUESTED);
        
        if (hasRejection) {
            pullRequest.setReviewStatus(PullRequest.ReviewStatus.REJECTED);
        } else if (hasChangesRequested) {
            pullRequest.setReviewStatus(PullRequest.ReviewStatus.CHANGES_REQUESTED);
        } else if (hasApproval) {
            pullRequest.setReviewStatus(PullRequest.ReviewStatus.APPROVED);
        }
        
        pullRequest.setUpdatedAt(LocalDateTime.now());
        pullRequestRepository.save(pullRequest);
    }
}
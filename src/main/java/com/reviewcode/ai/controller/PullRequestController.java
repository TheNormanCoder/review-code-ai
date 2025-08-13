package com.reviewcode.ai.controller;

import com.reviewcode.ai.model.CodeReview;
import com.reviewcode.ai.model.PullRequest;
import com.reviewcode.ai.service.CodeReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/reviews/pull-requests")
@CrossOrigin(origins = "*")
public class PullRequestController {
    
    private final CodeReviewService codeReviewService;
    
    @Autowired
    public PullRequestController(CodeReviewService codeReviewService) {
        this.codeReviewService = codeReviewService;
    }
    
    @PostMapping
    public ResponseEntity<PullRequest> createPullRequest(@Valid @RequestBody PullRequest pullRequest) {
        PullRequest created = codeReviewService.createPullRequest(pullRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping
    public ResponseEntity<List<PullRequest>> getAllPullRequests(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) PullRequest.PullRequestStatus status) {
        
        List<PullRequest> pullRequests;
        
        if (author != null) {
            pullRequests = codeReviewService.getPullRequestsByAuthor(author);
        } else if (status != null) {
            pullRequests = codeReviewService.getPullRequestsByStatus(status);
        } else {
            pullRequests = codeReviewService.getAllPullRequests();
        }
        
        return ResponseEntity.ok(pullRequests);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PullRequest> getPullRequest(@PathVariable Long id) {
        return codeReviewService.getPullRequest(id)
            .map(pr -> ResponseEntity.ok(pr))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/ai-review")
    public Mono<ResponseEntity<CodeReview>> triggerAiReview(
            @PathVariable Long id,
            @RequestBody List<String> filesToReview) {
        
        return codeReviewService.triggerAiReview(id, filesToReview)
            .map(review -> ResponseEntity.ok(review))
            .onErrorReturn(ResponseEntity.badRequest().build());
    }
    
    @PostMapping("/{id}/human-review")
    public ResponseEntity<CodeReview> addHumanReview(
            @PathVariable Long id,
            @Valid @RequestBody CodeReview review) {
        
        try {
            CodeReview created = codeReviewService.addHumanReview(id, review);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<CodeReview>> getReviews(@PathVariable Long id) {
        List<CodeReview> reviews = codeReviewService.getReviewsForPullRequest(id);
        return ResponseEntity.ok(reviews);
    }
}
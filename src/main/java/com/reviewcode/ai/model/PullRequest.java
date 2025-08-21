package com.reviewcode.ai.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pull_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PullRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @NotBlank
    @Column(nullable = false)
    private String author;
    
    @NotBlank
    @Column(nullable = false)
    private String repositoryUrl;
    
    @NotBlank
    @Column(nullable = false)
    private String sourceBranch;
    
    @NotBlank
    @Column(nullable = false)
    private String targetBranch;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PullRequestStatus status = PullRequestStatus.OPEN;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "pullRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CodeReview> reviews;
    
    @OneToMany(mappedBy = "pullRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReviewComment> comments;
    
    public enum PullRequestStatus {
        OPEN, CLOSED, MERGED, DRAFT
    }
    
    public enum ReviewStatus {
        PENDING, IN_PROGRESS, SUGGESTIONS_PENDING, APPROVED, CHANGES_REQUESTED, REJECTED
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
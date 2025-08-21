package com.reviewcode.ai.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "code_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeReview {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequest pullRequest;
    
    @NotBlank
    @Column(nullable = false)
    private String reviewer;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewerType reviewerType = ReviewerType.HUMAN;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewDecision decision = ReviewDecision.PENDING;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime completedAt;
    
    @OneToMany(mappedBy = "codeReview", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReviewFinding> findings;
    
    private Integer overallScore;
    
    public enum ReviewerType {
        HUMAN, AI_MCP, AUTOMATED
    }
    
    public enum ReviewDecision {
        PENDING, APPROVED, CHANGES_REQUESTED, REJECTED
    }
}
package com.reviewcode.ai.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMetrics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDateTime reportDate;
    
    @Column(nullable = false)
    private String period; // DAILY, WEEKLY, MONTHLY
    
    // PR Metrics
    @Column(nullable = false)
    private Integer totalPullRequests = 0;
    
    @Column(nullable = false)
    private Integer openPullRequests = 0;
    
    @Column(nullable = false)
    private Integer closedPullRequests = 0;
    
    @Column(nullable = false)
    private Integer mergedPullRequests = 0;
    
    @Column(nullable = false)
    private Double averageReviewTime = 0.0; // in hours
    
    @Column(nullable = false)
    private Double averagePullRequestSize = 0.0; // in LOC
    
    // Review Metrics
    @Column(nullable = false)
    private Integer totalReviews = 0;
    
    @Column(nullable = false)
    private Integer aiReviews = 0;
    
    @Column(nullable = false)
    private Integer humanReviews = 0;
    
    @Column(nullable = false)
    private Integer approvedReviews = 0;
    
    @Column(nullable = false)
    private Integer changesRequestedReviews = 0;
    
    @Column(nullable = false)
    private Integer rejectedReviews = 0;
    
    // Quality Metrics
    @Column(nullable = false)
    private Integer totalFindings = 0;
    
    @Column(nullable = false)
    private Integer criticalFindings = 0;
    
    @Column(nullable = false)
    private Integer highFindings = 0;
    
    @Column(nullable = false)
    private Integer mediumFindings = 0;
    
    @Column(nullable = false)
    private Integer lowFindings = 0;
    
    @Column(nullable = false)
    private Integer infoFindings = 0;
    
    // Security Metrics
    @Column(nullable = false)
    private Integer securityFindings = 0;
    
    @Column(nullable = false)
    private Integer bugFindings = 0;
    
    @Column(nullable = false)
    private Integer performanceFindings = 0;
    
    @Column(nullable = false)
    private Integer maintainabilityFindings = 0;
    
    // Architecture Metrics
    @Column(nullable = false)
    private Integer architectureFindings = 0;
    
    @Column(nullable = false)
    private Integer designPatternFindings = 0;
    
    @Column(nullable = false)
    private Integer solidPrincipleFindings = 0;
    
    @Column(nullable = false)
    private Integer dependencyInjectionFindings = 0;
    
    // Developer Metrics
    @Column(nullable = false)
    private Integer activeAuthors = 0;
    
    @Column(nullable = false)
    private Integer activeReviewers = 0;
    
    @Column(nullable = false)
    private Double reworkRate = 0.0; // percentage
    
    @Column(nullable = false)
    private Double approvalRate = 0.0; // percentage
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
package com.reviewcode.ai.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "team_adoption_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamAdoptionMetrics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String teamMember;
    
    @Column(nullable = false)
    private String period; // WEEKLY, MONTHLY, QUARTERLY
    
    @Column(nullable = false)
    private LocalDateTime reportDate;
    
    // SOLID Principles Adoption
    @Column(nullable = false)
    private Integer solidViolationsCount = 0;
    
    @Column(nullable = false)
    private Integer solidComplianceRate = 0; // percentage
    
    @Column(nullable = false)
    private Boolean solidPrinciplesUnderstood = false;
    
    // Clean Code Adoption
    @Column(nullable = false)
    private Integer dryViolations = 0;
    
    @Column(nullable = false)
    private Integer kissViolations = 0;
    
    @Column(nullable = false)
    private Integer yagniViolations = 0;
    
    @Column(nullable = false)
    private Double averageMethodLength = 0.0;
    
    @Column(nullable = false)
    private Double averageClassLength = 0.0;
    
    // DDD Adoption
    @Column(nullable = false)
    private Integer dddPatternUsage = 0; // count of correct DDD patterns used
    
    @Column(nullable = false)
    private Integer anemicModelCount = 0;
    
    @Column(nullable = false)
    private Boolean boundedContextRespected = true;
    
    // Architecture Compliance
    @Column(nullable = false)
    private Integer layerViolations = 0;
    
    @Column(nullable = false)
    private Integer dependencyInjectionViolations = 0;
    
    @Column(nullable = false)
    private Boolean architecturePatternFollowed = true;
    
    // Testing Adoption
    @Column(nullable = false)
    private Double testCoverage = 0.0;
    
    @Column(nullable = false)
    private Integer unitTestsWritten = 0;
    
    @Column(nullable = false)
    private Integer integrationTestsWritten = 0;
    
    @Column(nullable = false)
    private Boolean tddPracticed = false;
    
    // Security Awareness
    @Column(nullable = false)
    private Integer securityViolations = 0;
    
    @Column(nullable = false)
    private Boolean securityBestPracticesFollowed = true;
    
    // Performance Awareness
    @Column(nullable = false)
    private Integer performanceIssues = 0;
    
    @Column(nullable = false)
    private Boolean performanceConsiderations = true;
    
    // Knowledge & Training
    @Column(nullable = false)
    private Integer trainingSessionsAttended = 0;
    
    @Column(nullable = false)
    private Integer architectureReviewsParticipated = 0;
    
    @Column(nullable = false)
    private Boolean mentorshipProvided = false;
    
    @Column(nullable = false)
    private Boolean mentorshipReceived = false;
    
    // Code Review Participation
    @Column(nullable = false)
    private Integer reviewsGiven = 0;
    
    @Column(nullable = false)
    private Integer reviewsReceived = 0;
    
    @Column(nullable = false)
    private Double averageReviewQuality = 0.0; // 1-5 scale
    
    @Column(nullable = false)
    private Boolean architecturalFeedbackProvided = false;
    
    // Improvement Trends
    @Column(nullable = false)
    private Double improvementTrend = 0.0; // percentage change from previous period
    
    @Column(nullable = false)
    private String strengths; // JSON array of strong areas
    
    @Column(nullable = false)
    private String weaknesses; // JSON array of areas needing improvement
    
    @Column(nullable = false)
    private String recommendedActions; // JSON array of recommended training/actions
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
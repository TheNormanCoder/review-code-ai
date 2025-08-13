package com.reviewcode.ai.service;

import com.reviewcode.ai.model.*;
import com.reviewcode.ai.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PrincipleAdoptionService {
    
    private final TeamAdoptionMetricsRepository adoptionMetricsRepository;
    private final PrincipleAdoptionAssessmentRepository assessmentRepository;
    private final PullRequestRepository pullRequestRepository;
    private final CodeReviewRepository codeReviewRepository;
    
    @Autowired
    public PrincipleAdoptionService(TeamAdoptionMetricsRepository adoptionMetricsRepository,
                                  PrincipleAdoptionAssessmentRepository assessmentRepository,
                                  PullRequestRepository pullRequestRepository,
                                  CodeReviewRepository codeReviewRepository) {
        this.adoptionMetricsRepository = adoptionMetricsRepository;
        this.assessmentRepository = assessmentRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.codeReviewRepository = codeReviewRepository;
    }
    
    @Scheduled(cron = "0 0 10 ? * MON") // Every Monday at 10 AM
    public void generateWeeklyAdoptionMetrics() {
        generateAdoptionMetricsForPeriod("WEEKLY");
    }
    
    @Scheduled(cron = "0 0 10 1 * ?") // First day of month at 10 AM
    public void generateMonthlyAdoptionMetrics() {
        generateAdoptionMetricsForPeriod("MONTHLY");
    }
    
    public void generateAdoptionMetricsForPeriod(String period) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = switch (period) {
            case "WEEKLY" -> endDate.minusWeeks(1);
            case "MONTHLY" -> endDate.minusMonths(1);
            case "QUARTERLY" -> endDate.minusMonths(3);
            default -> endDate.minusWeeks(1);
        };
        
        // Get all team members from recent PRs
        List<String> teamMembers = pullRequestRepository.findByCreatedAtBetween(startDate, endDate)
            .stream()
            .map(PullRequest::getAuthor)
            .distinct()
            .collect(Collectors.toList());
        
        for (String teamMember : teamMembers) {
            TeamAdoptionMetrics metrics = calculateAdoptionMetrics(teamMember, period, startDate, endDate);
            adoptionMetricsRepository.save(metrics);
        }
    }
    
    private TeamAdoptionMetrics calculateAdoptionMetrics(String teamMember, String period, 
                                                        LocalDateTime startDate, LocalDateTime endDate) {
        TeamAdoptionMetrics metrics = new TeamAdoptionMetrics();
        metrics.setTeamMember(teamMember);
        metrics.setPeriod(period);
        metrics.setReportDate(startDate);
        
        // Get team member's PRs and reviews for the period
        List<PullRequest> memberPRs = pullRequestRepository.findByAuthorAndStatus(teamMember, PullRequest.PullRequestStatus.MERGED)
            .stream()
            .filter(pr -> pr.getCreatedAt().isAfter(startDate) && pr.getCreatedAt().isBefore(endDate))
            .collect(Collectors.toList());
        
        List<CodeReview> reviewsGiven = codeReviewRepository.findByReviewer(teamMember)
            .stream()
            .filter(review -> review.getCreatedAt().isAfter(startDate) && review.getCreatedAt().isBefore(endDate))
            .collect(Collectors.toList());
        
        // Calculate SOLID compliance
        calculateSOLIDMetrics(metrics, memberPRs);
        
        // Calculate Clean Code metrics
        calculateCleanCodeMetrics(metrics, memberPRs);
        
        // Calculate DDD metrics
        calculateDDDMetrics(metrics, memberPRs);
        
        // Calculate Architecture metrics
        calculateArchitectureMetrics(metrics, memberPRs);
        
        // Calculate Testing metrics
        calculateTestingMetrics(metrics, memberPRs);
        
        // Calculate Security metrics
        calculateSecurityMetrics(metrics, memberPRs);
        
        // Calculate Performance metrics
        calculatePerformanceMetrics(metrics, memberPRs);
        
        // Calculate Code Review metrics
        calculateCodeReviewMetrics(metrics, reviewsGiven);
        
        // Calculate improvement trends
        calculateImprovementTrends(metrics, teamMember, period);
        
        // Generate recommendations
        generateRecommendations(metrics);
        
        return metrics;
    }
    
    private void calculateSOLIDMetrics(TeamAdoptionMetrics metrics, List<PullRequest> prs) {
        int totalViolations = 0;
        int totalReviews = 0;
        
        for (PullRequest pr : prs) {
            List<CodeReview> reviews = codeReviewRepository.findByPullRequestId(pr.getId());
            for (CodeReview review : reviews) {
                if (review.getFindings() != null) {
                    long solidViolations = review.getFindings().stream()
                        .filter(f -> f.getType() == ReviewFinding.FindingType.SOLID_PRINCIPLES ||
                                   f.getType() == ReviewFinding.FindingType.DEPENDENCY_INJECTION ||
                                   f.getType() == ReviewFinding.FindingType.SEPARATION_OF_CONCERNS)
                        .count();
                    totalViolations += solidViolations;
                    totalReviews++;
                }
            }
        }
        
        metrics.setSolidViolationsCount(totalViolations);
        if (totalReviews > 0) {
            int complianceRate = Math.max(0, 100 - (totalViolations * 100 / totalReviews));
            metrics.setSolidComplianceRate(complianceRate);
            metrics.setSolidPrinciplesUnderstood(complianceRate >= 80);
        }
    }
    
    private void calculateCleanCodeMetrics(TeamAdoptionMetrics metrics, List<PullRequest> prs) {
        int dryViolations = 0;
        int kissViolations = 0;
        int yagniViolations = 0;
        
        for (PullRequest pr : prs) {
            List<CodeReview> reviews = codeReviewRepository.findByPullRequestId(pr.getId());
            for (CodeReview review : reviews) {
                if (review.getFindings() != null) {
                    dryViolations += review.getFindings().stream()
                        .filter(f -> f.getType() == ReviewFinding.FindingType.DRY_VIOLATION)
                        .mapToInt(f -> 1).sum();
                    
                    kissViolations += review.getFindings().stream()
                        .filter(f -> f.getType() == ReviewFinding.FindingType.KISS_VIOLATION)
                        .mapToInt(f -> 1).sum();
                    
                    yagniViolations += review.getFindings().stream()
                        .filter(f -> f.getType() == ReviewFinding.FindingType.YAGNI_VIOLATION)
                        .mapToInt(f -> 1).sum();
                }
            }
        }
        
        metrics.setDryViolations(dryViolations);
        metrics.setKissViolations(kissViolations);
        metrics.setYagniViolations(yagniViolations);
        
        // Calculate average method and class length (would need Git integration for actual LOC)
        metrics.setAverageMethodLength(15.0); // Placeholder
        metrics.setAverageClassLength(120.0); // Placeholder
    }
    
    private void calculateDDDMetrics(TeamAdoptionMetrics metrics, List<PullRequest> prs) {
        int dddPatternUsage = 0;
        int anemicModelCount = 0;
        
        for (PullRequest pr : prs) {
            List<CodeReview> reviews = codeReviewRepository.findByPullRequestId(pr.getId());
            for (CodeReview review : reviews) {
                if (review.getFindings() != null) {
                    dddPatternUsage += review.getFindings().stream()
                        .filter(f -> f.getType() == ReviewFinding.FindingType.DDD_AGGREGATE ||
                                   f.getType() == ReviewFinding.FindingType.DDD_VALUE_OBJECT ||
                                   f.getType() == ReviewFinding.FindingType.DDD_DOMAIN_SERVICE)
                        .filter(f -> f.getSeverity() == ReviewFinding.Severity.INFO) // Positive findings
                        .mapToInt(f -> 1).sum();
                    
                    anemicModelCount += review.getFindings().stream()
                        .filter(f -> f.getDescription().toLowerCase().contains("anemic"))
                        .mapToInt(f -> 1).sum();
                }
            }
        }
        
        metrics.setDddPatternUsage(dddPatternUsage);
        metrics.setAnemicModelCount(anemicModelCount);
        metrics.setBoundedContextRespected(anemicModelCount == 0);
    }
    
    private void calculateArchitectureMetrics(TeamAdoptionMetrics metrics, List<PullRequest> prs) {
        int layerViolations = 0;
        int diViolations = 0;
        
        for (PullRequest pr : prs) {
            List<CodeReview> reviews = codeReviewRepository.findByPullRequestId(pr.getId());
            for (CodeReview review : reviews) {
                if (review.getFindings() != null) {
                    layerViolations += review.getFindings().stream()
                        .filter(f -> f.getType() == ReviewFinding.FindingType.ARCHITECTURE)
                        .mapToInt(f -> 1).sum();
                    
                    diViolations += review.getFindings().stream()
                        .filter(f -> f.getType() == ReviewFinding.FindingType.DEPENDENCY_INJECTION)
                        .mapToInt(f -> 1).sum();
                }
            }
        }
        
        metrics.setLayerViolations(layerViolations);
        metrics.setDependencyInjectionViolations(diViolations);
        metrics.setArchitecturePatternFollowed(layerViolations == 0);
    }
    
    private void calculateTestingMetrics(TeamAdoptionMetrics metrics, List<PullRequest> prs) {
        int unitTests = 0;
        int integrationTests = 0;
        double totalCoverage = 0;
        int coverageCount = 0;
        
        for (PullRequest pr : prs) {
            List<CodeReview> reviews = codeReviewRepository.findByPullRequestId(pr.getId());
            for (CodeReview review : reviews) {
                if (review.getFindings() != null) {
                    unitTests += review.getFindings().stream()
                        .filter(f -> f.getType() == ReviewFinding.FindingType.TEST_COVERAGE)
                        .filter(f -> f.getDescription().toLowerCase().contains("unit"))
                        .mapToInt(f -> 1).sum();
                    
                    integrationTests += review.getFindings().stream()
                        .filter(f -> f.getType() == ReviewFinding.FindingType.TEST_COVERAGE)
                        .filter(f -> f.getDescription().toLowerCase().contains("integration"))
                        .mapToInt(f -> 1).sum();
                }
            }
            // Placeholder for actual coverage calculation
            totalCoverage += 85.0;
            coverageCount++;
        }
        
        metrics.setUnitTestsWritten(unitTests);
        metrics.setIntegrationTestsWritten(integrationTests);
        if (coverageCount > 0) {
            metrics.setTestCoverage(totalCoverage / coverageCount);
        }
        metrics.setTddPracticed(unitTests > prs.size()); // Heuristic: more tests than features
    }
    
    private void calculateSecurityMetrics(TeamAdoptionMetrics metrics, List<PullRequest> prs) {
        int securityViolations = 0;
        
        for (PullRequest pr : prs) {
            List<CodeReview> reviews = codeReviewRepository.findByPullRequestId(pr.getId());
            for (CodeReview review : reviews) {
                if (review.getFindings() != null) {
                    securityViolations += review.getFindings().stream()
                        .filter(f -> f.getType() == ReviewFinding.FindingType.SECURITY)
                        .mapToInt(f -> 1).sum();
                }
            }
        }
        
        metrics.setSecurityViolations(securityViolations);
        metrics.setSecurityBestPracticesFollowed(securityViolations == 0);
    }
    
    private void calculatePerformanceMetrics(TeamAdoptionMetrics metrics, List<PullRequest> prs) {
        int performanceIssues = 0;
        
        for (PullRequest pr : prs) {
            List<CodeReview> reviews = codeReviewRepository.findByPullRequestId(pr.getId());
            for (CodeReview review : reviews) {
                if (review.getFindings() != null) {
                    performanceIssues += review.getFindings().stream()
                        .filter(f -> f.getType() == ReviewFinding.FindingType.PERFORMANCE)
                        .mapToInt(f -> 1).sum();
                }
            }
        }
        
        metrics.setPerformanceIssues(performanceIssues);
        metrics.setPerformanceConsiderations(performanceIssues <= prs.size() * 0.1); // Max 10% of PRs with issues
    }
    
    private void calculateCodeReviewMetrics(TeamAdoptionMetrics metrics, List<CodeReview> reviewsGiven) {
        metrics.setReviewsGiven(reviewsGiven.size());
        
        double averageQuality = reviewsGiven.stream()
            .filter(review -> review.getFindings() != null)
            .mapToDouble(review -> {
                int findingsCount = review.getFindings().size();
                return findingsCount > 0 ? Math.min(5.0, 3.0 + (findingsCount * 0.2)) : 3.0;
            })
            .average()
            .orElse(3.0);
        
        metrics.setAverageReviewQuality(averageQuality);
        
        boolean architecturalFeedback = reviewsGiven.stream()
            .anyMatch(review -> review.getFindings() != null && 
                review.getFindings().stream().anyMatch(f -> 
                    f.getType() == ReviewFinding.FindingType.ARCHITECTURE ||
                    f.getType() == ReviewFinding.FindingType.DESIGN_PATTERN ||
                    f.getType() == ReviewFinding.FindingType.SOLID_PRINCIPLES));
        
        metrics.setArchitecturalFeedbackProvided(architecturalFeedback);
    }
    
    private void calculateImprovementTrends(TeamAdoptionMetrics metrics, String teamMember, String period) {
        List<TeamAdoptionMetrics> previousMetrics = adoptionMetricsRepository
            .findByTeamMemberOrderByDateDesc(teamMember);
        
        if (!previousMetrics.isEmpty()) {
            TeamAdoptionMetrics previous = previousMetrics.get(0);
            double currentScore = calculateOverallScore(metrics);
            double previousScore = calculateOverallScore(previous);
            
            double improvementTrend = ((currentScore - previousScore) / previousScore) * 100;
            metrics.setImprovementTrend(improvementTrend);
        }
    }
    
    private double calculateOverallScore(TeamAdoptionMetrics metrics) {
        return (metrics.getSolidComplianceRate() * 0.25) +
               (metrics.getTestCoverage() * 0.25) +
               ((100 - metrics.getSecurityViolations()) * 0.20) +
               ((100 - metrics.getPerformanceIssues()) * 0.15) +
               (metrics.getAverageReviewQuality() * 20 * 0.15);
    }
    
    private void generateRecommendations(TeamAdoptionMetrics metrics) {
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        
        // Analyze strengths and weaknesses
        if (metrics.getSolidComplianceRate() >= 90) {
            strengths.add("SOLID Principles");
        } else if (metrics.getSolidComplianceRate() < 70) {
            weaknesses.add("SOLID Principles");
            actions.add("Attend SOLID principles workshop");
        }
        
        if (metrics.getTestCoverage() >= 80) {
            strengths.add("Test Coverage");
        } else {
            weaknesses.add("Test Coverage");
            actions.add("Focus on TDD practices");
        }
        
        if (metrics.getSecurityViolations() == 0) {
            strengths.add("Security Awareness");
        } else {
            weaknesses.add("Security Practices");
            actions.add("Security training required");
        }
        
        if (metrics.getArchitecturalFeedbackProvided()) {
            strengths.add("Architecture Review Skills");
        } else {
            actions.add("Practice architectural review skills");
        }
        
        metrics.setStrengths(String.join(",", strengths));
        metrics.setWeaknesses(String.join(",", weaknesses));
        metrics.setRecommendedActions(String.join(",", actions));
    }
    
    public PrincipleAdoptionAssessment conductAssessment(String teamMember, String principleCategory, 
                                                        String specificPrinciple, String assessmentType) {
        // This would typically be called by a form or automated analysis
        PrincipleAdoptionAssessment assessment = new PrincipleAdoptionAssessment();
        assessment.setTeamMember(teamMember);
        assessment.setPrincipleCategory(principleCategory);
        assessment.setSpecificPrinciple(specificPrinciple);
        assessment.setAssessmentType(assessmentType);
        
        // Auto-determine adoption level based on metrics (simplified)
        TeamAdoptionMetrics recent = adoptionMetricsRepository
            .findByTeamMemberOrderByDateDesc(teamMember)
            .stream()
            .findFirst()
            .orElse(null);
        
        if (recent != null) {
            assessment.setAdoptionLevel(determineAdoptionLevel(recent, principleCategory));
        }
        
        return assessmentRepository.save(assessment);
    }
    
    private PrincipleAdoptionAssessment.AdoptionLevel determineAdoptionLevel(TeamAdoptionMetrics metrics, String category) {
        return switch (category) {
            case "SOLID" -> {
                if (metrics.getSolidComplianceRate() >= 95) yield PrincipleAdoptionAssessment.AdoptionLevel.PROFICIENT;
                if (metrics.getSolidComplianceRate() >= 80) yield PrincipleAdoptionAssessment.AdoptionLevel.PRACTICING;
                if (metrics.getSolidComplianceRate() >= 60) yield PrincipleAdoptionAssessment.AdoptionLevel.LEARNING;
                if (metrics.getSolidComplianceRate() >= 30) yield PrincipleAdoptionAssessment.AdoptionLevel.AWARE;
                yield PrincipleAdoptionAssessment.AdoptionLevel.NOT_AWARE;
            }
            case "TESTING" -> {
                if (metrics.getTestCoverage() >= 90) yield PrincipleAdoptionAssessment.AdoptionLevel.PROFICIENT;
                if (metrics.getTestCoverage() >= 70) yield PrincipleAdoptionAssessment.AdoptionLevel.PRACTICING;
                if (metrics.getTestCoverage() >= 50) yield PrincipleAdoptionAssessment.AdoptionLevel.LEARNING;
                yield PrincipleAdoptionAssessment.AdoptionLevel.AWARE;
            }
            default -> PrincipleAdoptionAssessment.AdoptionLevel.LEARNING;
        };
    }
}
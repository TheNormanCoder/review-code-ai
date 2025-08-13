package com.reviewcode.ai.service;

import com.reviewcode.ai.model.*;
import com.reviewcode.ai.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class MetricsService {
    
    private final ReviewMetricsRepository metricsRepository;
    private final PullRequestRepository pullRequestRepository;
    private final CodeReviewRepository codeReviewRepository;
    
    @Autowired
    public MetricsService(ReviewMetricsRepository metricsRepository,
                         PullRequestRepository pullRequestRepository,
                         CodeReviewRepository codeReviewRepository) {
        this.metricsRepository = metricsRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.codeReviewRepository = codeReviewRepository;
    }
    
    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    public void generateDailyMetrics() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime today = yesterday.plusDays(1);
        
        generateMetricsForPeriod(yesterday, today, "DAILY");
    }
    
    @Scheduled(cron = "0 0 2 ? * MON") // Weekly on Monday at 2 AM
    public void generateWeeklyMetrics() {
        LocalDateTime lastWeek = LocalDateTime.now().minusWeeks(1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime thisWeek = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        
        generateMetricsForPeriod(lastWeek, thisWeek, "WEEKLY");
    }
    
    @Scheduled(cron = "0 0 3 1 * ?") // Monthly on 1st at 3 AM
    public void generateMonthlyMetrics() {
        LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime thisMonth = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        
        generateMetricsForPeriod(lastMonth, thisMonth, "MONTHLY");
    }
    
    public ReviewMetrics generateMetricsForPeriod(LocalDateTime startDate, LocalDateTime endDate, String period) {
        List<PullRequest> pullRequests = pullRequestRepository.findByCreatedAtBetween(startDate, endDate);
        List<CodeReview> reviews = pullRequests.stream()
            .flatMap(pr -> codeReviewRepository.findByPullRequestId(pr.getId()).stream())
            .collect(Collectors.toList());
        
        ReviewMetrics metrics = new ReviewMetrics();
        metrics.setReportDate(startDate);
        metrics.setPeriod(period);
        
        // Calculate PR metrics
        calculatePullRequestMetrics(metrics, pullRequests);
        
        // Calculate review metrics
        calculateReviewMetrics(metrics, reviews);
        
        // Calculate quality metrics
        calculateQualityMetrics(metrics, reviews);
        
        // Calculate developer metrics
        calculateDeveloperMetrics(metrics, pullRequests, reviews);
        
        return metricsRepository.save(metrics);
    }
    
    private void calculatePullRequestMetrics(ReviewMetrics metrics, List<PullRequest> pullRequests) {
        metrics.setTotalPullRequests(pullRequests.size());
        metrics.setOpenPullRequests((int) pullRequests.stream()
            .filter(pr -> pr.getStatus() == PullRequest.PullRequestStatus.OPEN)
            .count());
        metrics.setClosedPullRequests((int) pullRequests.stream()
            .filter(pr -> pr.getStatus() == PullRequest.PullRequestStatus.CLOSED)
            .count());
        metrics.setMergedPullRequests((int) pullRequests.stream()
            .filter(pr -> pr.getStatus() == PullRequest.PullRequestStatus.MERGED)
            .count());
        
        // Calculate average review time
        double avgReviewTime = pullRequests.stream()
            .filter(pr -> pr.getUpdatedAt() != null)
            .mapToDouble(pr -> ChronoUnit.HOURS.between(pr.getCreatedAt(), pr.getUpdatedAt()))
            .average()
            .orElse(0.0);
        metrics.setAverageReviewTime(avgReviewTime);
        
        // For now, set average PR size to 0 (would need Git integration to calculate actual LOC)
        metrics.setAveragePullRequestSize(0.0);
    }
    
    private void calculateReviewMetrics(ReviewMetrics metrics, List<CodeReview> reviews) {
        metrics.setTotalReviews(reviews.size());
        metrics.setAiReviews((int) reviews.stream()
            .filter(r -> r.getReviewerType() == CodeReview.ReviewerType.AI_MCP)
            .count());
        metrics.setHumanReviews((int) reviews.stream()
            .filter(r -> r.getReviewerType() == CodeReview.ReviewerType.HUMAN)
            .count());
        metrics.setApprovedReviews((int) reviews.stream()
            .filter(r -> r.getDecision() == CodeReview.ReviewDecision.APPROVED)
            .count());
        metrics.setChangesRequestedReviews((int) reviews.stream()
            .filter(r -> r.getDecision() == CodeReview.ReviewDecision.CHANGES_REQUESTED)
            .count());
        metrics.setRejectedReviews((int) reviews.stream()
            .filter(r -> r.getDecision() == CodeReview.ReviewDecision.REJECTED)
            .count());
    }
    
    private void calculateQualityMetrics(ReviewMetrics metrics, List<CodeReview> reviews) {
        List<ReviewFinding> findings = reviews.stream()
            .flatMap(r -> r.getFindings() != null ? r.getFindings().stream() : List.<ReviewFinding>of().stream())
            .collect(Collectors.toList());
        
        metrics.setTotalFindings(findings.size());
        metrics.setCriticalFindings((int) findings.stream()
            .filter(f -> f.getSeverity() == ReviewFinding.Severity.CRITICAL)
            .count());
        metrics.setHighFindings((int) findings.stream()
            .filter(f -> f.getSeverity() == ReviewFinding.Severity.HIGH)
            .count());
        metrics.setMediumFindings((int) findings.stream()
            .filter(f -> f.getSeverity() == ReviewFinding.Severity.MEDIUM)
            .count());
        metrics.setLowFindings((int) findings.stream()
            .filter(f -> f.getSeverity() == ReviewFinding.Severity.LOW)
            .count());
        metrics.setInfoFindings((int) findings.stream()
            .filter(f -> f.getSeverity() == ReviewFinding.Severity.INFO)
            .count());
        
        metrics.setSecurityFindings((int) findings.stream()
            .filter(f -> f.getType() == ReviewFinding.FindingType.SECURITY)
            .count());
        metrics.setBugFindings((int) findings.stream()
            .filter(f -> f.getType() == ReviewFinding.FindingType.BUG)
            .count());
        metrics.setPerformanceFindings((int) findings.stream()
            .filter(f -> f.getType() == ReviewFinding.FindingType.PERFORMANCE)
            .count());
        metrics.setMaintainabilityFindings((int) findings.stream()
            .filter(f -> f.getType() == ReviewFinding.FindingType.MAINTAINABILITY)
            .count());
        
        metrics.setArchitectureFindings((int) findings.stream()
            .filter(f -> f.getType() == ReviewFinding.FindingType.ARCHITECTURE)
            .count());
        metrics.setDesignPatternFindings((int) findings.stream()
            .filter(f -> f.getType() == ReviewFinding.FindingType.DESIGN_PATTERN)
            .count());
        metrics.setSolidPrincipleFindings((int) findings.stream()
            .filter(f -> f.getType() == ReviewFinding.FindingType.SOLID_PRINCIPLES)
            .count());
        metrics.setDependencyInjectionFindings((int) findings.stream()
            .filter(f -> f.getType() == ReviewFinding.FindingType.DEPENDENCY_INJECTION)
            .count());
    }
    
    private void calculateDeveloperMetrics(ReviewMetrics metrics, List<PullRequest> pullRequests, List<CodeReview> reviews) {
        Set<String> activeAuthors = pullRequests.stream()
            .map(PullRequest::getAuthor)
            .collect(Collectors.toSet());
        metrics.setActiveAuthors(activeAuthors.size());
        
        Set<String> activeReviewers = reviews.stream()
            .filter(r -> r.getReviewerType() == CodeReview.ReviewerType.HUMAN)
            .map(CodeReview::getReviewer)
            .collect(Collectors.toSet());
        metrics.setActiveReviewers(activeReviewers.size());
        
        // Calculate rework rate (PRs that needed changes / total PRs)
        long prNeedingChanges = pullRequests.stream()
            .filter(pr -> pr.getReviewStatus() == PullRequest.ReviewStatus.CHANGES_REQUESTED)
            .count();
        double reworkRate = pullRequests.isEmpty() ? 0.0 : (double) prNeedingChanges / pullRequests.size() * 100;
        metrics.setReworkRate(reworkRate);
        
        // Calculate approval rate (approved reviews / total reviews)
        long approvedReviews = reviews.stream()
            .filter(r -> r.getDecision() == CodeReview.ReviewDecision.APPROVED)
            .count();
        double approvalRate = reviews.isEmpty() ? 0.0 : (double) approvedReviews / reviews.size() * 100;
        metrics.setApprovalRate(approvalRate);
    }
    
    public List<ReviewMetrics> getMetricsForPeriod(String period, int limit) {
        return metricsRepository.findByPeriodOrderByReportDateDesc(period)
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    public List<ReviewMetrics> getMetricsSince(LocalDateTime startDate) {
        return metricsRepository.findMetricsSince(startDate);
    }
}
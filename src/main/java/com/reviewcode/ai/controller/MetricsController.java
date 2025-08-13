package com.reviewcode.ai.controller;

import com.reviewcode.ai.model.ReviewMetrics;
import com.reviewcode.ai.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews/metrics")
@CrossOrigin(origins = "*")
public class MetricsController {
    
    private final MetricsService metricsService;
    
    @Autowired
    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
    
    @GetMapping("/daily")
    public ResponseEntity<List<ReviewMetrics>> getDailyMetrics(
            @RequestParam(defaultValue = "30") int days) {
        List<ReviewMetrics> metrics = metricsService.getMetricsForPeriod("DAILY", days);
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/weekly")
    public ResponseEntity<List<ReviewMetrics>> getWeeklyMetrics(
            @RequestParam(defaultValue = "12") int weeks) {
        List<ReviewMetrics> metrics = metricsService.getMetricsForPeriod("WEEKLY", weeks);
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/monthly")
    public ResponseEntity<List<ReviewMetrics>> getMonthlyMetrics(
            @RequestParam(defaultValue = "12") int months) {
        List<ReviewMetrics> metrics = metricsService.getMetricsForPeriod("MONTHLY", months);
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/since")
    public ResponseEntity<List<ReviewMetrics>> getMetricsSince(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate) {
        List<ReviewMetrics> metrics = metricsService.getMetricsSince(startDate);
        return ResponseEntity.ok(metrics);
    }
    
    @PostMapping("/generate")
    public ResponseEntity<ReviewMetrics> generateMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam String period) {
        
        ReviewMetrics metrics = metricsService.generateMetricsForPeriod(startDate, endDate, period);
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardMetrics() {
        LocalDateTime lastWeek = LocalDateTime.now().minusWeeks(1);
        List<ReviewMetrics> recentMetrics = metricsService.getMetricsSince(lastWeek);
        
        if (recentMetrics.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No metrics available"));
        }
        
        ReviewMetrics latest = recentMetrics.get(recentMetrics.size() - 1);
        
        Map<String, Object> dashboard = Map.of(
            "summary", Map.of(
                "totalPullRequests", latest.getTotalPullRequests(),
                "averageReviewTime", latest.getAverageReviewTime(),
                "approvalRate", latest.getApprovalRate(),
                "reworkRate", latest.getReworkRate()
            ),
            "quality", Map.of(
                "totalFindings", latest.getTotalFindings(),
                "criticalFindings", latest.getCriticalFindings(),
                "highFindings", latest.getHighFindings(),
                "securityFindings", latest.getSecurityFindings()
            ),
            "team", Map.of(
                "activeAuthors", latest.getActiveAuthors(),
                "activeReviewers", latest.getActiveReviewers(),
                "totalReviews", latest.getTotalReviews(),
                "aiReviews", latest.getAiReviews()
            ),
            "trends", recentMetrics
        );
        
        return ResponseEntity.ok(dashboard);
    }
}
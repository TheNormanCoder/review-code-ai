package com.reviewcode.ai.repository;

import com.reviewcode.ai.model.ReviewMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewMetricsRepository extends JpaRepository<ReviewMetrics, Long> {
    
    List<ReviewMetrics> findByPeriod(String period);
    
    List<ReviewMetrics> findByReportDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    Optional<ReviewMetrics> findByReportDateAndPeriod(LocalDateTime reportDate, String period);
    
    @Query("SELECT rm FROM ReviewMetrics rm WHERE rm.period = :period ORDER BY rm.reportDate DESC")
    List<ReviewMetrics> findByPeriodOrderByReportDateDesc(@Param("period") String period);
    
    @Query("SELECT rm FROM ReviewMetrics rm WHERE rm.reportDate >= :startDate ORDER BY rm.reportDate ASC")
    List<ReviewMetrics> findMetricsSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT AVG(rm.averageReviewTime) FROM ReviewMetrics rm WHERE rm.period = :period AND rm.reportDate >= :startDate")
    Double getAverageReviewTimeForPeriod(@Param("period") String period, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT AVG(rm.approvalRate) FROM ReviewMetrics rm WHERE rm.period = :period AND rm.reportDate >= :startDate")
    Double getAverageApprovalRateForPeriod(@Param("period") String period, @Param("startDate") LocalDateTime startDate);
}
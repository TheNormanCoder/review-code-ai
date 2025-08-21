package com.reviewcode.ai.repository;

import com.reviewcode.ai.model.TeamAdoptionMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamAdoptionMetricsRepository extends JpaRepository<TeamAdoptionMetrics, Long> {
    
    List<TeamAdoptionMetrics> findByTeamMember(String teamMember);
    
    List<TeamAdoptionMetrics> findByPeriod(String period);
    
    Optional<TeamAdoptionMetrics> findByTeamMemberAndPeriodAndReportDate(
        String teamMember, String period, LocalDateTime reportDate);
    
    @Query("SELECT tam FROM TeamAdoptionMetrics tam WHERE tam.reportDate >= :startDate ORDER BY tam.reportDate DESC")
    List<TeamAdoptionMetrics> findRecentMetrics(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT tam FROM TeamAdoptionMetrics tam WHERE tam.teamMember = :teamMember ORDER BY tam.reportDate DESC")
    List<TeamAdoptionMetrics> findByTeamMemberOrderByDateDesc(@Param("teamMember") String teamMember);
    
    @Query("SELECT AVG(tam.solidComplianceRate) FROM TeamAdoptionMetrics tam WHERE tam.period = :period AND tam.reportDate >= :startDate")
    Double getAverageSOLIDComplianceForPeriod(@Param("period") String period, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT AVG(tam.testCoverage) FROM TeamAdoptionMetrics tam WHERE tam.period = :period AND tam.reportDate >= :startDate")
    Double getAverageTestCoverageForPeriod(@Param("period") String period, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT tam.teamMember, COUNT(tam) as violationCount FROM TeamAdoptionMetrics tam WHERE tam.solidViolationsCount > :threshold GROUP BY tam.teamMember ORDER BY violationCount DESC")
    List<Object[]> findTeamMembersWithHighViolations(@Param("threshold") Integer threshold);
}
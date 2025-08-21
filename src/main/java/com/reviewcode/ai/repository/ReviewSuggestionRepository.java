package com.reviewcode.ai.repository;

import com.reviewcode.ai.model.ReviewSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewSuggestionRepository extends JpaRepository<ReviewSuggestion, Long> {
    
    List<ReviewSuggestion> findByPullRequestId(Long pullRequestId);
    
    List<ReviewSuggestion> findByPullRequestIdAndStatus(Long pullRequestId, ReviewSuggestion.SuggestionStatus status);
    
    @Query("SELECT rs FROM ReviewSuggestion rs WHERE rs.pullRequest.id = :pullRequestId AND rs.status IN :statuses")
    List<ReviewSuggestion> findByPullRequestIdAndStatusIn(@Param("pullRequestId") Long pullRequestId, 
                                                          @Param("statuses") List<ReviewSuggestion.SuggestionStatus> statuses);
    
    @Query("SELECT rs FROM ReviewSuggestion rs WHERE rs.pullRequest.id = :pullRequestId AND rs.severity = :severity")
    List<ReviewSuggestion> findByPullRequestIdAndSeverity(@Param("pullRequestId") Long pullRequestId, 
                                                          @Param("severity") ReviewSuggestion.Severity severity);
    
    @Query("SELECT rs FROM ReviewSuggestion rs WHERE rs.pullRequest.id = :pullRequestId AND rs.type = :type")
    List<ReviewSuggestion> findByPullRequestIdAndType(@Param("pullRequestId") Long pullRequestId, 
                                                      @Param("type") ReviewSuggestion.SuggestionType type);
    
    @Query("SELECT COUNT(rs) FROM ReviewSuggestion rs WHERE rs.pullRequest.id = :pullRequestId AND rs.status = :status")
    long countByPullRequestIdAndStatus(@Param("pullRequestId") Long pullRequestId, 
                                      @Param("status") ReviewSuggestion.SuggestionStatus status);
    
    void deleteByPullRequestId(Long pullRequestId);
}
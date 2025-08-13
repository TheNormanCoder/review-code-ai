package com.reviewcode.ai.repository;

import com.reviewcode.ai.model.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
    
    List<PullRequest> findByAuthor(String author);
    
    List<PullRequest> findByStatus(PullRequest.PullRequestStatus status);
    
    List<PullRequest> findByReviewStatus(PullRequest.ReviewStatus reviewStatus);
    
    List<PullRequest> findByRepositoryUrl(String repositoryUrl);
    
    @Query("SELECT pr FROM PullRequest pr WHERE pr.createdAt BETWEEN :startDate AND :endDate")
    List<PullRequest> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pr FROM PullRequest pr WHERE pr.author = :author AND pr.status = :status")
    List<PullRequest> findByAuthorAndStatus(@Param("author") String author, 
                                          @Param("status") PullRequest.PullRequestStatus status);
}
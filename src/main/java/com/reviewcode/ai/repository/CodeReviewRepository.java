package com.reviewcode.ai.repository;

import com.reviewcode.ai.model.CodeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {
    
    List<CodeReview> findByPullRequestId(Long pullRequestId);
    
    List<CodeReview> findByReviewer(String reviewer);
    
    List<CodeReview> findByReviewerType(CodeReview.ReviewerType reviewerType);
    
    List<CodeReview> findByDecision(CodeReview.ReviewDecision decision);
    
    @Query("SELECT cr FROM CodeReview cr WHERE cr.pullRequest.id = :pullRequestId AND cr.reviewerType = :reviewerType")
    List<CodeReview> findByPullRequestIdAndReviewerType(@Param("pullRequestId") Long pullRequestId, 
                                                       @Param("reviewerType") CodeReview.ReviewerType reviewerType);
}
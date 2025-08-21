package com.reviewcode.ai.repository;

import com.reviewcode.ai.model.PrincipleAdoptionAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PrincipleAdoptionAssessmentRepository extends JpaRepository<PrincipleAdoptionAssessment, Long> {
    
    List<PrincipleAdoptionAssessment> findByTeamMember(String teamMember);
    
    List<PrincipleAdoptionAssessment> findByPrincipleCategory(String principleCategory);
    
    List<PrincipleAdoptionAssessment> findByAdoptionLevel(PrincipleAdoptionAssessment.AdoptionLevel adoptionLevel);
    
    List<PrincipleAdoptionAssessment> findByTeamMemberAndPrincipleCategory(String teamMember, String principleCategory);
    
    @Query("SELECT paa FROM PrincipleAdoptionAssessment paa WHERE paa.teamMember = :teamMember AND paa.specificPrinciple = :principle ORDER BY paa.assessmentDate DESC")
    List<PrincipleAdoptionAssessment> findProgressionForPrinciple(@Param("teamMember") String teamMember, @Param("principle") String principle);
    
    @Query("SELECT paa.teamMember, AVG(CAST(paa.adoptionLevel AS int)) as avgLevel FROM PrincipleAdoptionAssessment paa WHERE paa.assessmentDate >= :startDate GROUP BY paa.teamMember ORDER BY avgLevel DESC")
    List<Object[]> getTeamAdoptionRanking(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT paa.principleCategory, COUNT(paa) as assessmentCount FROM PrincipleAdoptionAssessment paa WHERE paa.adoptionLevel IN ('PROFICIENT', 'TEACHING', 'INNOVATING') GROUP BY paa.principleCategory")
    List<Object[]> getPrincipleStrengthsByCategory();
    
    @Query("SELECT paa FROM PrincipleAdoptionAssessment paa WHERE paa.nextReviewDate <= :currentDate")
    List<PrincipleAdoptionAssessment> findAssessmentsDueForReview(@Param("currentDate") LocalDateTime currentDate);
}
package com.reviewcode.ai.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "principle_adoption_assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrincipleAdoptionAssessment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String teamMember;
    
    @Column(nullable = false)
    private String assessmentType; // SELF, PEER, TECHNICAL_LEAD, AUTOMATED
    
    @Column(nullable = false)
    private String principleCategory; // SOLID, CLEAN_CODE, DDD, ARCHITECTURE, SECURITY
    
    @Column(nullable = false)
    private String specificPrinciple; // SRP, OCP, DRY, ENTITY_DESIGN, etc.
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdoptionLevel adoptionLevel;
    
    @Column(nullable = false)
    private Integer confidenceScore = 0; // 1-10 scale
    
    @Column(columnDefinition = "TEXT")
    private String evidence; // Code examples, PR links, etc.
    
    @Column(columnDefinition = "TEXT")
    private String observations;
    
    @Column(columnDefinition = "TEXT")
    private String improvementSuggestions;
    
    @Column(nullable = false)
    private LocalDateTime assessmentDate = LocalDateTime.now();
    
    @Column
    private LocalDateTime nextReviewDate;
    
    @Column
    private String assessorId; // Who performed the assessment
    
    public enum AdoptionLevel {
        NOT_AWARE,        // Doesn't know the principle exists
        AWARE,            // Knows about it but doesn't use it
        LEARNING,         // Trying to apply but inconsistent
        PRACTICING,       // Applies consistently with guidance
        PROFICIENT,       // Applies independently and correctly
        TEACHING,         // Can teach and mentor others
        INNOVATING        // Creates new patterns based on principles
    }
}
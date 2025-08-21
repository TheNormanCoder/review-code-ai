package com.reviewcode.ai.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "review_findings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewFinding {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_review_id", nullable = false)
    private CodeReview codeReview;
    
    @NotBlank
    @Column(nullable = false)
    private String fileName;
    
    @Column
    private Integer lineNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FindingType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;
    
    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String suggestion;
    
    @Column(columnDefinition = "TEXT")
    private String codeSnippet;
    
    @Column
    private String ruleId;
    
    public enum FindingType {
        // Quality & Style
        CODE_STYLE, DOCUMENTATION, BEST_PRACTICE,
        
        // Issues
        SECURITY, PERFORMANCE, BUG, MAINTAINABILITY,
        
        // Architecture & Design
        ARCHITECTURE, DESIGN_PATTERN, SOLID_PRINCIPLES, DEPENDENCY_INJECTION, SEPARATION_OF_CONCERNS,
        
        // Clean Code
        DRY_VIOLATION, KISS_VIOLATION, YAGNI_VIOLATION,
        
        // Domain-Driven Design
        DDD_BOUNDED_CONTEXT, DDD_AGGREGATE, DDD_VALUE_OBJECT, DDD_DOMAIN_SERVICE,
        
        // Functional Programming
        IMMUTABILITY, PURE_FUNCTION, SIDE_EFFECTS,
        
        // Testing
        TEST_COVERAGE, TEST_QUALITY, TDD_VIOLATION,
        
        // API Design
        API_DESIGN, REST_COMPLIANCE, ERROR_HANDLING,
        
        // Scalability
        SCALABILITY, ASYNC_PROCESSING, CACHING,
        
        // Observability
        LOGGING, MONITORING, TRACING
    }
    
    public enum Severity {
        INFO, LOW, MEDIUM, HIGH, CRITICAL
    }
}
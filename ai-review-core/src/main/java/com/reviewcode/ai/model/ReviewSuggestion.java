package com.reviewcode.ai.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_suggestions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSuggestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequest pullRequest;
    
    @NotBlank
    @Column(nullable = false)
    private String fileName;
    
    @Column
    private Integer lineNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionType type;
    
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
    
    @Column(columnDefinition = "TEXT")
    private String proposedCode;
    
    @Column
    private String ruleId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus status = SuggestionStatus.PENDING;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime approvedAt;
    
    @Column
    private LocalDateTime appliedAt;
    
    @Column
    private String approvedBy;
    
    public enum SuggestionType {
        SECURITY,
        PERFORMANCE,
        BUG,
        MAINTAINABILITY,
        DOCUMENTATION,
        BEST_PRACTICE,
        ARCHITECTURE,
        DESIGN_PATTERN,
        SOLID_PRINCIPLES,
        DEPENDENCY_INJECTION,
        SEPARATION_OF_CONCERNS,
        CODE_STYLE
    }
    
    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        INFO
    }
    
    public enum SuggestionStatus {
        PENDING,
        APPROVED,
        REJECTED,
        APPLIED
    }
    
    @PreUpdate
    public void preUpdate() {
        if (status == SuggestionStatus.APPROVED && approvedAt == null) {
            approvedAt = LocalDateTime.now();
        }
        if (status == SuggestionStatus.APPLIED && appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }
    }
}
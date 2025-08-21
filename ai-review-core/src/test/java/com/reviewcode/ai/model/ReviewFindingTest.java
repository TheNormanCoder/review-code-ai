package com.reviewcode.ai.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class ReviewFindingTest {

    private ReviewFinding reviewFinding;
    private CodeReview codeReview;

    @BeforeEach
    void setUp() {
        reviewFinding = new ReviewFinding();
        codeReview = new CodeReview();
    }

    @Test
    void shouldCreateReviewFindingWithAllFields() {
        // Given
        reviewFinding.setCodeReview(codeReview);
        reviewFinding.setFileName("TestService.java");
        reviewFinding.setLineNumber(42);
        reviewFinding.setType(ReviewFinding.FindingType.SOLID_PRINCIPLES);
        reviewFinding.setSeverity(ReviewFinding.Severity.HIGH);
        reviewFinding.setDescription("Single Responsibility Principle violation");
        reviewFinding.setSuggestion("Extract utility methods to separate class");
        reviewFinding.setCodeSnippet("public class TestService { ... }");
        reviewFinding.setRuleId("SRP001");

        // Then
        assertEquals(codeReview, reviewFinding.getCodeReview());
        assertEquals("TestService.java", reviewFinding.getFileName());
        assertEquals(42, reviewFinding.getLineNumber());
        assertEquals(ReviewFinding.FindingType.SOLID_PRINCIPLES, reviewFinding.getType());
        assertEquals(ReviewFinding.Severity.HIGH, reviewFinding.getSeverity());
        assertEquals("Single Responsibility Principle violation", reviewFinding.getDescription());
        assertEquals("Extract utility methods to separate class", reviewFinding.getSuggestion());
        assertEquals("public class TestService { ... }", reviewFinding.getCodeSnippet());
        assertEquals("SRP001", reviewFinding.getRuleId());
    }

    @Test
    void shouldValidateAllFindingTypes() {
        // Test all finding types exist and can be set
        ReviewFinding.FindingType[] allTypes = ReviewFinding.FindingType.values();
        
        assertTrue(allTypes.length > 0);
        
        // Test some key types
        reviewFinding.setType(ReviewFinding.FindingType.SOLID_PRINCIPLES);
        assertEquals(ReviewFinding.FindingType.SOLID_PRINCIPLES, reviewFinding.getType());
        
        reviewFinding.setType(ReviewFinding.FindingType.DRY_VIOLATION);
        assertEquals(ReviewFinding.FindingType.DRY_VIOLATION, reviewFinding.getType());
        
        reviewFinding.setType(ReviewFinding.FindingType.SECURITY);
        assertEquals(ReviewFinding.FindingType.SECURITY, reviewFinding.getType());
        
        reviewFinding.setType(ReviewFinding.FindingType.PERFORMANCE);
        assertEquals(ReviewFinding.FindingType.PERFORMANCE, reviewFinding.getType());
        
        reviewFinding.setType(ReviewFinding.FindingType.DDD_AGGREGATE);
        assertEquals(ReviewFinding.FindingType.DDD_AGGREGATE, reviewFinding.getType());
    }

    @Test
    void shouldValidateAllSeverityLevels() {
        // Test all severity levels
        ReviewFinding.Severity[] allSeverities = ReviewFinding.Severity.values();
        
        assertEquals(5, allSeverities.length);
        
        reviewFinding.setSeverity(ReviewFinding.Severity.INFO);
        assertEquals(ReviewFinding.Severity.INFO, reviewFinding.getSeverity());
        
        reviewFinding.setSeverity(ReviewFinding.Severity.LOW);
        assertEquals(ReviewFinding.Severity.LOW, reviewFinding.getSeverity());
        
        reviewFinding.setSeverity(ReviewFinding.Severity.MEDIUM);
        assertEquals(ReviewFinding.Severity.MEDIUM, reviewFinding.getSeverity());
        
        reviewFinding.setSeverity(ReviewFinding.Severity.HIGH);
        assertEquals(ReviewFinding.Severity.HIGH, reviewFinding.getSeverity());
        
        reviewFinding.setSeverity(ReviewFinding.Severity.CRITICAL);
        assertEquals(ReviewFinding.Severity.CRITICAL, reviewFinding.getSeverity());
    }

    @Test
    void shouldAllowOptionalFields() {
        // Given - minimal required fields only
        reviewFinding.setCodeReview(codeReview);
        reviewFinding.setFileName("Optional.java");
        reviewFinding.setType(ReviewFinding.FindingType.CODE_STYLE);
        reviewFinding.setSeverity(ReviewFinding.Severity.LOW);
        reviewFinding.setDescription("Minor style issue");

        // Then - optional fields can be null
        assertNull(reviewFinding.getLineNumber());
        assertNull(reviewFinding.getSuggestion());
        assertNull(reviewFinding.getCodeSnippet());
        assertNull(reviewFinding.getRuleId());
    }

    @Test
    void shouldCreateFindingWithConstructor() {
        // When
        ReviewFinding finding = new ReviewFinding(
            1L,
            codeReview,
            "TestClass.java",
            100,
            ReviewFinding.FindingType.ARCHITECTURE,
            ReviewFinding.Severity.MEDIUM,
            "Layer violation detected",
            "Move class to appropriate package",
            "package wrong.layer;",
            "LAYER001"
        );

        // Then
        assertEquals(1L, finding.getId());
        assertEquals(codeReview, finding.getCodeReview());
        assertEquals("TestClass.java", finding.getFileName());
        assertEquals(100, finding.getLineNumber());
        assertEquals(ReviewFinding.FindingType.ARCHITECTURE, finding.getType());
        assertEquals(ReviewFinding.Severity.MEDIUM, finding.getSeverity());
        assertEquals("Layer violation detected", finding.getDescription());
        assertEquals("Move class to appropriate package", finding.getSuggestion());
        assertEquals("package wrong.layer;", finding.getCodeSnippet());
        assertEquals("LAYER001", finding.getRuleId());
    }

    @Test
    void shouldCategorizeArchitecturalFindingTypes() {
        // Verify architectural finding types are properly categorized
        assertTrue(isArchitecturalType(ReviewFinding.FindingType.ARCHITECTURE));
        assertTrue(isArchitecturalType(ReviewFinding.FindingType.DESIGN_PATTERN));
        assertTrue(isArchitecturalType(ReviewFinding.FindingType.SOLID_PRINCIPLES));
        assertTrue(isArchitecturalType(ReviewFinding.FindingType.DEPENDENCY_INJECTION));
        assertTrue(isArchitecturalType(ReviewFinding.FindingType.DDD_AGGREGATE));
        assertTrue(isArchitecturalType(ReviewFinding.FindingType.DDD_VALUE_OBJECT));
        
        // Verify non-architectural types
        assertFalse(isArchitecturalType(ReviewFinding.FindingType.CODE_STYLE));
        assertFalse(isArchitecturalType(ReviewFinding.FindingType.DOCUMENTATION));
    }

    private boolean isArchitecturalType(ReviewFinding.FindingType type) {
        return type == ReviewFinding.FindingType.ARCHITECTURE ||
               type == ReviewFinding.FindingType.DESIGN_PATTERN ||
               type == ReviewFinding.FindingType.SOLID_PRINCIPLES ||
               type == ReviewFinding.FindingType.DEPENDENCY_INJECTION ||
               type == ReviewFinding.FindingType.SEPARATION_OF_CONCERNS ||
               type.name().startsWith("DDD_");
    }
}
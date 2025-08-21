package com.reviewcode.ai.features;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Java21FeaturesTest {

    @InjectMocks
    private Java21Features java21Features;

    @Test
    void shouldProcessReviewWithVirtualThreadsEfficiently() {
        // Given
        List<String> files = List.of(
            "UserService.java",
            "OrderController.java", 
            "PaymentProcessor.java",
            "SecurityValidator.java"
        );
        
        var startTime = Instant.now();

        // When
        CompletableFuture<String> result = java21Features.processReviewWithVirtualThreads(files);
        String analysisResult = result.join();

        // Then
        assertNotNull(analysisResult);
        assertTrue(analysisResult.contains("UserService.java"));
        assertTrue(analysisResult.contains("OrderController.java"));
        
        var duration = Duration.between(startTime, Instant.now());
        // Should complete within reasonable time due to virtual threads
        assertTrue(duration.toMillis() < 2000, "Virtual threads should make this fast");
    }

    @Test
    void shouldCategorizeSecurityFindingsCorrectly() {
        // Given
        var criticalSecurity = new Java21Features.SecurityFinding("CRITICAL", "SQL injection detected");
        var highSecurity = new Java21Features.SecurityFinding("HIGH", "Weak encryption");

        // When
        String criticalResult = java21Features.categorizeReviewResult(criticalSecurity);
        String highResult = java21Features.categorizeReviewResult(highSecurity);

        // Then
        assertTrue(criticalResult.contains("🚨 Critical Security Issue"));
        assertTrue(criticalResult.contains("SQL injection detected"));
        assertFalse(highResult.contains("🚨 Critical Security Issue"));
    }

    @Test
    void shouldCategorizePerformanceFindingsCorrectly() {
        // Given
        var highImpactPerf = new Java21Features.PerformanceFinding(75, "Optimize database queries");
        var lowImpactPerf = new Java21Features.PerformanceFinding(25, "Minor optimization possible");

        // When
        String highResult = java21Features.categorizeReviewResult(highImpactPerf);
        String lowResult = java21Features.categorizeReviewResult(lowImpactPerf);

        // Then
        assertTrue(highResult.contains("⚡ High Performance Impact"));
        assertTrue(highResult.contains("Optimize database queries"));
        assertFalse(lowResult.contains("⚡ High Performance Impact"));
    }

    @Test
    void shouldCategorizeCodeQualityFindingsCorrectly() {
        // Given
        var qualityFinding = new Java21Features.CodeQualityFinding("Long Method", 42);

        // When
        String result = java21Features.categorizeReviewResult(qualityFinding);

        // Then
        assertTrue(result.contains("📝 Code Quality Issue"));
        assertTrue(result.contains("line 42"));
        assertTrue(result.contains("Long Method"));
    }

    @Test
    void shouldHandleStringResults() {
        // When
        String approvedResult = java21Features.categorizeReviewResult("APPROVED");
        String rejectedResult = java21Features.categorizeReviewResult("REJECTED");
        String unknownResult = java21Features.categorizeReviewResult("PENDING");

        // Then
        assertTrue(approvedResult.contains("✅ Review Approved"));
        assertTrue(rejectedResult.contains("❌ Review Rejected"));
        assertTrue(unknownResult.contains("ℹ️ Unknown result type"));
    }

    @Test
    void shouldHandleNullResult() {
        // When
        String result = java21Features.categorizeReviewResult(null);

        // Then
        assertTrue(result.contains("⚠️ No review result available"));
    }

    @Test
    void shouldGenerateFormattedReviewSummary() {
        // When
        String summary = java21Features.generateReviewSummary("john.doe", 85, 3);

        // Then
        assertNotNull(summary);
        assertTrue(summary.contains("📊 Code Review Summary"));
        assertTrue(summary.contains("john.doe"));
        assertTrue(summary.contains("85"));
        assertTrue(summary.contains("3"));
        assertTrue(summary.contains("┌─────────────────────────────────┐"));
    }

    @Test
    void shouldProcessReviewCommentsWithSequencedCollections() {
        // Given
        List<String> comments = List.of(
            "Initial review comment",
            "Found security issue",
            "Performance concern noted",
            "Final approval comment"
        );

        // When/Then - Should not throw exceptions
        assertDoesNotThrow(() -> {
            java21Features.processReviewComments(comments);
        });

        // Test with empty list
        assertDoesNotThrow(() -> {
            java21Features.processReviewComments(List.of());
        });
    }

    @Test
    void shouldDetermineCorrectPriorityForSecurityFindings() {
        // When
        var urgentPriority = java21Features.determineReviewPriority(
            Java21Features.FindingType.SECURITY, "CRITICAL");
        var highPriority = java21Features.determineReviewPriority(
            Java21Features.FindingType.SECURITY, "HIGH");
        var mediumPriority = java21Features.determineReviewPriority(
            Java21Features.FindingType.SECURITY, "MEDIUM");

        // Then
        assertEquals(Java21Features.Priority.URGENT, urgentPriority);
        assertEquals(Java21Features.Priority.HIGH, highPriority);
        assertEquals(Java21Features.Priority.MEDIUM, mediumPriority);
    }

    @Test
    void shouldDetermineCorrectPriorityForPerformanceFindings() {
        // When
        var highPriority1 = java21Features.determineReviewPriority(
            Java21Features.FindingType.PERFORMANCE, "CRITICAL");
        var highPriority2 = java21Features.determineReviewPriority(
            Java21Features.FindingType.PERFORMANCE, "HIGH");
        var mediumPriority = java21Features.determineReviewPriority(
            Java21Features.FindingType.PERFORMANCE, "MEDIUM");

        // Then
        assertEquals(Java21Features.Priority.HIGH, highPriority1);
        assertEquals(Java21Features.Priority.HIGH, highPriority2);
        assertEquals(Java21Features.Priority.MEDIUM, mediumPriority);
    }

    @Test
    void shouldDetermineCorrectPriorityForOtherFindings() {
        // When
        var lowPriority1 = java21Features.determineReviewPriority(
            Java21Features.FindingType.CODE_QUALITY, "HIGH");
        var lowPriority2 = java21Features.determineReviewPriority(
            Java21Features.FindingType.DOCUMENTATION, "MEDIUM");

        // Then
        assertEquals(Java21Features.Priority.LOW, lowPriority1);
        assertEquals(Java21Features.Priority.LOW, lowPriority2);
    }

    @Test
    void shouldProcessLargeCodebaseEfficiently() {
        // Given
        List<String> largeFileList = generateLargeFileList(100);
        var startTime = Instant.now();

        // When
        assertDoesNotThrow(() -> {
            java21Features.processLargeCodebase(largeFileList);
        });

        // Then
        var duration = Duration.between(startTime, Instant.now());
        // With Java 21 optimizations, should handle large loads efficiently
        assertTrue(duration.toSeconds() < 10, "Should process efficiently with Java 21 optimizations");
    }

    @Test
    void shouldHandleVirtualThreadsWithLargeWorkload() {
        // Given - Large number of files to simulate real-world scenario
        List<String> manyFiles = generateLargeFileList(1000);
        var startTime = Instant.now();

        // When
        CompletableFuture<String> result = java21Features.processReviewWithVirtualThreads(manyFiles);
        String analysisResult = result.join();

        // Then
        assertNotNull(analysisResult);
        var duration = Duration.between(startTime, Instant.now());
        
        // Virtual threads should handle this load efficiently
        assertTrue(duration.toSeconds() < 30, 
            "Virtual threads should handle 1000 files efficiently");
        
        // Verify all files were processed
        for (int i = 0; i < 10; i++) { // Check first 10
            assertTrue(analysisResult.contains("file_" + i + ".java"));
        }
    }

    private List<String> generateLargeFileList(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> "file_" + i + ".java")
            .toList();
    }
}
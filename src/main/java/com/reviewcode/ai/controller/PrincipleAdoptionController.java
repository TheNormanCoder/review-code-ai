package com.reviewcode.ai.controller;

import com.reviewcode.ai.model.PrincipleAdoptionAssessment;
import com.reviewcode.ai.model.TeamAdoptionMetrics;
import com.reviewcode.ai.service.PrincipleAdoptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews/adoption")
@CrossOrigin(origins = "*")
public class PrincipleAdoptionController {
    
    private final PrincipleAdoptionService adoptionService;
    
    @Autowired
    public PrincipleAdoptionController(PrincipleAdoptionService adoptionService) {
        this.adoptionService = adoptionService;
    }
    
    @GetMapping("/team/{teamMember}")
    public ResponseEntity<List<TeamAdoptionMetrics>> getTeamMemberAdoption(
            @PathVariable String teamMember) {
        // This would return adoption metrics for a specific team member
        return ResponseEntity.ok(List.of()); // Placeholder
    }
    
    @GetMapping("/principles/dashboard")
    public ResponseEntity<Map<String, Object>> getPrinciplesDashboard() {
        // Create comprehensive dashboard showing principle adoption across team
        Map<String, Object> dashboard = Map.of(
            "solidAdoption", Map.of(
                "averageCompliance", 85.5,
                "topPerformers", List.of("alice", "bob", "charlie"),
                "needsImprovement", List.of("dave", "eve"),
                "trendDirection", "improving"
            ),
            "cleanCodeAdoption", Map.of(
                "dryViolationsAvg", 2.3,
                "kissViolationsAvg", 1.8,
                "yagniViolationsAvg", 0.9,
                "overallGrade", "B+"
            ),
            "dddAdoption", Map.of(
                "patternsUsed", 78,
                "anemicModels", 5,
                "boundedContexts", 12,
                "maturityLevel", "Intermediate"
            ),
            "testingAdoption", Map.of(
                "averageCoverage", 82.4,
                "tddPractitioners", 6,
                "unitTestQuality", "High",
                "integrationTests", "Medium"
            ),
            "securityAdoption", Map.of(
                "violationsLastMonth", 3,
                "trainingCompleted", 8,
                "bestPracticesFollowed", 95.2,
                "criticalIssues", 0
            ),
            "teamProgress", Map.of(
                "totalMembers", 10,
                "proficientLevel", 4,
                "learningLevel", 5,
                "beginnerLevel", 1,
                "overallTrend", "+12% this quarter"
            )
        );
        
        return ResponseEntity.ok(dashboard);
    }
    
    @PostMapping("/assessment")
    public ResponseEntity<PrincipleAdoptionAssessment> createAssessment(
            @RequestBody AssessmentRequest request) {
        
        PrincipleAdoptionAssessment assessment = adoptionService.conductAssessment(
            request.getTeamMember(),
            request.getPrincipleCategory(),
            request.getSpecificPrinciple(),
            request.getAssessmentType()
        );
        
        return ResponseEntity.ok(assessment);
    }
    
    @PostMapping("/generate-metrics")
    public ResponseEntity<String> generateMetrics(@RequestParam String period) {
        adoptionService.generateAdoptionMetricsForPeriod(period);
        return ResponseEntity.ok("Metrics generation started for period: " + period);
    }
    
    @GetMapping("/principles/solid/status")
    public ResponseEntity<Map<String, Object>> getSOLIDStatus() {
        Map<String, Object> solidStatus = Map.of(
            "singleResponsibility", Map.of(
                "compliance", 88.5,
                "commonViolations", List.of("Large classes", "Multiple concerns in one class"),
                "recommendations", List.of("Extract utility classes", "Separate business logic")
            ),
            "openClosed", Map.of(
                "compliance", 82.1,
                "commonViolations", List.of("Direct modification instead of extension"),
                "recommendations", List.of("Use strategy pattern", "Implement plugin architecture")
            ),
            "liskovSubstitution", Map.of(
                "compliance", 91.3,
                "commonViolations", List.of("Incorrect inheritance hierarchy"),
                "recommendations", List.of("Prefer composition over inheritance")
            ),
            "interfaceSegregation", Map.of(
                "compliance", 85.7,
                "commonViolations", List.of("Fat interfaces", "Unnecessary method implementations"),
                "recommendations", List.of("Split large interfaces", "Use role-based interfaces")
            ),
            "dependencyInversion", Map.of(
                "compliance", 79.4,
                "commonViolations", List.of("Direct dependencies", "Constructor injection missing"),
                "recommendations", List.of("Use dependency injection", "Program to interfaces")
            )
        );
        
        return ResponseEntity.ok(solidStatus);
    }
    
    @GetMapping("/principles/clean-code/status")
    public ResponseEntity<Map<String, Object>> getCleanCodeStatus() {
        Map<String, Object> cleanCodeStatus = Map.of(
            "dry", Map.of(
                "violationsLastWeek", 15,
                "mostCommon", "Duplicate validation logic",
                "reduction", "23% improvement vs last month"
            ),
            "kiss", Map.of(
                "complexMethodsCount", 8,
                "averageCyclomaticComplexity", 4.2,
                "targetComplexity", 3.0
            ),
            "yagni", Map.of(
                "overEngineeredFeatures", 2,
                "unusedCode", "3.2%",
                "preventionMeasures", List.of("Feature toggles", "Iterative development")
            ),
            "readability", Map.of(
                "namingScore", 87.5,
                "commentQuality", 72.3,
                "methodLength", "Good"
            )
        );
        
        return ResponseEntity.ok(cleanCodeStatus);
    }
    
    @GetMapping("/principles/ddd/status")
    public ResponseEntity<Map<String, Object>> getDDDStatus() {
        Map<String, Object> dddStatus = Map.of(
            "boundedContexts", Map.of(
                "defined", 5,
                "wellImplemented", 4,
                "needsImprovement", 1
            ),
            "entities", Map.of(
                "properlyModeled", 23,
                "anemicModels", 3,
                "richDomainModels", 18
            ),
            "valueObjects", Map.of(
                "identified", 15,
                "immutable", 14,
                "properlyUsed", 13
            ),
            "aggregates", Map.of(
                "defined", 8,
                "consistencyBoundaries", "Well defined",
                "rootEntities", "Properly identified"
            ),
            "domainServices", Map.of(
                "count", 6,
                "stateless", 6,
                "businessLogic", "Properly encapsulated"
            ),
            "ubiquitousLanguage", Map.of(
                "adoption", 78.5,
                "consistency", "Good",
                "documentation", "Needs improvement"
            )
        );
        
        return ResponseEntity.ok(dddStatus);
    }
    
    @GetMapping("/training/recommendations")
    public ResponseEntity<Map<String, Object>> getTrainingRecommendations() {
        Map<String, Object> recommendations = Map.of(
            "immediate", List.of(
                Map.of(
                    "principle", "Dependency Injection",
                    "priority", "HIGH",
                    "affectedMembers", List.of("dave", "eve"),
                    "estimatedTime", "4 hours",
                    "format", "Workshop + hands-on"
                ),
                Map.of(
                    "principle", "Clean Code - Method Length",
                    "priority", "MEDIUM",
                    "affectedMembers", List.of("frank", "grace"),
                    "estimatedTime", "2 hours",
                    "format", "Code review session"
                )
            ),
            "upcoming", List.of(
                Map.of(
                    "principle", "DDD Aggregates",
                    "priority", "MEDIUM",
                    "affectedMembers", List.of("alice", "bob", "charlie"),
                    "estimatedTime", "6 hours",
                    "format", "Workshop series"
                )
            ),
            "ongoing", List.of(
                Map.of(
                    "principle", "Test-Driven Development",
                    "progress", "60%",
                    "participants", 8,
                    "nextSession", "2024-01-15"
                )
            )
        );
        
        return ResponseEntity.ok(recommendations);
    }
    
    @GetMapping("/compliance/report")
    public ResponseEntity<Map<String, Object>> getComplianceReport() {
        Map<String, Object> report = Map.of(
            "overall", Map.of(
                "score", 84.2,
                "grade", "B+",
                "trend", "+5.3% vs last quarter"
            ),
            "byPrinciple", Map.of(
                "SOLID", 85.5,
                "CleanCode", 82.1,
                "DDD", 79.8,
                "Testing", 88.3,
                "Security", 95.2,
                "Performance", 81.7
            ),
            "byTeamMember", Map.of(
                "alice", 92.5,
                "bob", 89.1,
                "charlie", 87.3,
                "dave", 76.8,
                "eve", 74.2
            ),
            "actionItems", List.of(
                "Schedule DI training for Dave and Eve",
                "Code review focus on method complexity",
                "DDD workshop for advanced team members",
                "Security awareness refresh for all"
            )
        );
        
        return ResponseEntity.ok(report);
    }
    
    public static class AssessmentRequest {
        private String teamMember;
        private String principleCategory;
        private String specificPrinciple;
        private String assessmentType;
        
        // Getters and setters
        public String getTeamMember() { return teamMember; }
        public void setTeamMember(String teamMember) { this.teamMember = teamMember; }
        
        public String getPrincipleCategory() { return principleCategory; }
        public void setPrincipleCategory(String principleCategory) { this.principleCategory = principleCategory; }
        
        public String getSpecificPrinciple() { return specificPrinciple; }
        public void setSpecificPrinciple(String specificPrinciple) { this.specificPrinciple = specificPrinciple; }
        
        public String getAssessmentType() { return assessmentType; }
        public void setAssessmentType(String assessmentType) { this.assessmentType = assessmentType; }
    }
}
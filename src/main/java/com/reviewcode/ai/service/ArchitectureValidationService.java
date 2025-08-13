package com.reviewcode.ai.service;

import com.reviewcode.ai.model.ReviewFinding;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ArchitectureValidationService {
    
    private static final Pattern LONG_METHOD_PATTERN = Pattern.compile("(?s).*public\\s+\\w+.*\\{.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\}");
    private static final Pattern MANY_PARAMETERS_PATTERN = Pattern.compile("\\([^)]*,.*,.*,.*,.*,.*[^)]*\\)");
    private static final Pattern HARDCODED_SECRET_PATTERN = Pattern.compile("(?i)(password|secret|apikey|token)\\s*=\\s*[\"'][^\"']{8,}[\"']");
    private static final Pattern FIELD_INJECTION_PATTERN = Pattern.compile("@Autowired\\s+private");
    private static final Pattern SELECT_ALL_PATTERN = Pattern.compile("(?i)select\\s+\\*\\s+from");
    
    public List<ReviewFinding> validateArchitecturalPrinciples(String fileName, String code) {
        List<ReviewFinding> findings = new ArrayList<>();
        
        // Clean Code Principles
        findings.addAll(validateCleanCodePrinciples(fileName, code));
        
        // SOLID Principles
        findings.addAll(validateSolidPrinciples(fileName, code));
        
        // DDD Principles
        findings.addAll(validateDddPrinciples(fileName, code));
        
        // Performance & Security
        findings.addAll(validatePerformanceAndSecurity(fileName, code));
        
        return findings;
    }
    
    private List<ReviewFinding> validateCleanCodePrinciples(String fileName, String code) {
        List<ReviewFinding> findings = new ArrayList<>();
        
        // Check for long methods
        if (LONG_METHOD_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName, 
                0, 
                ReviewFinding.FindingType.KISS_VIOLATION,
                ReviewFinding.Severity.MEDIUM,
                "Method appears to be too long",
                "Break down long methods into smaller, focused methods. Each method should do one thing well.",
                "Long method detected"
            ));
        }
        
        // Check for many parameters
        if (MANY_PARAMETERS_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName,
                0,
                ReviewFinding.FindingType.BEST_PRACTICE,
                ReviewFinding.Severity.MEDIUM,
                "Method has too many parameters",
                "Consider using a parameter object or builder pattern to reduce parameter count.",
                "Method with many parameters"
            ));
        }
        
        // Check for code duplication indicators
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length - 5; i++) {
            String currentBlock = String.join("\n", 
                lines[i], lines[i+1], lines[i+2], lines[i+3], lines[i+4]);
            
            for (int j = i + 5; j < lines.length - 5; j++) {
                String compareBlock = String.join("\n", 
                    lines[j], lines[j+1], lines[j+2], lines[j+3], lines[j+4]);
                
                if (currentBlock.trim().equals(compareBlock.trim()) && 
                    currentBlock.trim().length() > 50) {
                    findings.add(createFinding(
                        fileName,
                        i + 1,
                        ReviewFinding.FindingType.DRY_VIOLATION,
                        ReviewFinding.Severity.HIGH,
                        "Duplicate code block detected",
                        "Extract duplicate code into a reusable method to follow DRY principle.",
                        currentBlock.substring(0, Math.min(100, currentBlock.length()))
                    ));
                    break;
                }
            }
        }
        
        return findings;
    }
    
    private List<ReviewFinding> validateSolidPrinciples(String fileName, String code) {
        List<ReviewFinding> findings = new ArrayList<>();
        
        // Check for field injection (violates Dependency Inversion)
        if (FIELD_INJECTION_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName,
                0,
                ReviewFinding.FindingType.DEPENDENCY_INJECTION,
                ReviewFinding.Severity.MEDIUM,
                "Field injection detected",
                "Use constructor injection instead of field injection for better testability and immutability.",
                "@Autowired field injection"
            ));
        }
        
        // Check for large classes (violates Single Responsibility)
        long lineCount = code.lines().count();
        if (lineCount > 300) {
            findings.add(createFinding(
                fileName,
                0,
                ReviewFinding.FindingType.SOLID_PRINCIPLES,
                ReviewFinding.Severity.HIGH,
                "Class is too large (" + lineCount + " lines)",
                "Large classes often violate Single Responsibility Principle. Consider breaking into smaller, focused classes.",
                "Large class detected"
            ));
        }
        
        // Check for concrete dependencies in constructors
        if (code.contains("new ") && code.contains("public ") && code.contains("(")) {
            findings.add(createFinding(
                fileName,
                0,
                ReviewFinding.FindingType.DEPENDENCY_INJECTION,
                ReviewFinding.Severity.MEDIUM,
                "Potential hard dependency detected",
                "Avoid creating dependencies with 'new'. Use dependency injection instead.",
                "Hard dependency creation"
            ));
        }
        
        return findings;
    }
    
    private List<ReviewFinding> validateDddPrinciples(String fileName, String code) {
        List<ReviewFinding> findings = new ArrayList<>();
        
        // Check if Entity has proper ID
        if (code.contains("@Entity") && !code.contains("@Id")) {
            findings.add(createFinding(
                fileName,
                0,
                ReviewFinding.FindingType.DDD_AGGREGATE,
                ReviewFinding.Severity.HIGH,
                "Entity missing @Id annotation",
                "Domain entities must have identity. Add @Id annotation to the identifier field.",
                "@Entity without @Id"
            ));
        }
        
        // Check for anemic domain model
        if (code.contains("@Entity") && !code.contains("public ") && code.contains("get") && code.contains("set")) {
            long methodCount = code.split("public ").length - 1;
            long getterSetterCount = code.split("get|set").length - 1;
            
            if (getterSetterCount > methodCount * 0.8) {
                findings.add(createFinding(
                    fileName,
                    0,
                    ReviewFinding.FindingType.DDD_DOMAIN_SERVICE,
                    ReviewFinding.Severity.MEDIUM,
                    "Potential anemic domain model",
                    "Domain entities should contain business logic, not just getters/setters. Consider adding domain methods.",
                    "Mostly getters/setters"
                ));
            }
        }
        
        return findings;
    }
    
    private List<ReviewFinding> validatePerformanceAndSecurity(String fileName, String code) {
        List<ReviewFinding> findings = new ArrayList<>();
        
        // Check for hardcoded secrets
        if (HARDCODED_SECRET_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName,
                0,
                ReviewFinding.FindingType.SECURITY,
                ReviewFinding.Severity.CRITICAL,
                "Hardcoded secret detected",
                "Never hardcode passwords, API keys, or secrets. Use configuration properties or environment variables.",
                "Hardcoded secret"
            ));
        }
        
        // Check for SELECT * queries
        if (SELECT_ALL_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName,
                0,
                ReviewFinding.FindingType.PERFORMANCE,
                ReviewFinding.Severity.MEDIUM,
                "SELECT * query detected",
                "Avoid SELECT * queries. Specify only the required columns for better performance.",
                "SELECT * query"
            ));
        }
        
        // Check for potential N+1 query problem
        if (code.contains("@OneToMany") && !code.contains("fetch = FetchType.LAZY")) {
            findings.add(createFinding(
                fileName,
                0,
                ReviewFinding.FindingType.PERFORMANCE,
                ReviewFinding.Severity.MEDIUM,
                "Potential N+1 query problem",
                "Use LAZY loading for @OneToMany relationships to avoid N+1 query problems.",
                "@OneToMany without LAZY loading"
            ));
        }
        
        return findings;
    }
    
    private ReviewFinding createFinding(String fileName, int lineNumber, 
                                       ReviewFinding.FindingType type, 
                                       ReviewFinding.Severity severity,
                                       String description, String suggestion, 
                                       String codeSnippet) {
        ReviewFinding finding = new ReviewFinding();
        finding.setFileName(fileName);
        finding.setLineNumber(lineNumber > 0 ? lineNumber : null);
        finding.setType(type);
        finding.setSeverity(severity);
        finding.setDescription(description);
        finding.setSuggestion(suggestion);
        finding.setCodeSnippet(codeSnippet);
        finding.setRuleId("ARCH_" + type.name());
        return finding;
    }
}
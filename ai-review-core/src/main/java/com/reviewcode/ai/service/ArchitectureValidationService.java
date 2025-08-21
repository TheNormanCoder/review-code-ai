package com.reviewcode.ai.service;

import com.reviewcode.ai.model.ReviewFinding;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.Arrays;

@Service
public class ArchitectureValidationService {
    
    // Enhanced regex patterns for better detection
    private static final Pattern LONG_METHOD_PATTERN = Pattern.compile("(?s)public\\s+\\w+[^{]*\\{([^{}]*\\{[^{}]*\\}[^{}]*)*[^{}]*\\}");
    private static final Pattern MANY_PARAMETERS_PATTERN = Pattern.compile("\\([^)]*,.*,.*,.*,.*,.*[^)]*\\)");
    private static final Pattern HARDCODED_SECRET_PATTERN = Pattern.compile("(?i)(password|secret|apikey|token|key)\\s*[=:]\\s*[\"'][^\"']{8,}[\"']");
    private static final Pattern FIELD_INJECTION_PATTERN = Pattern.compile("@Autowired\\s+private");
    private static final Pattern SELECT_ALL_PATTERN = Pattern.compile("(?i)select\\s+\\*\\s+from");
    
    // New patterns for enhanced detection
    private static final Pattern DEEP_NESTING_PATTERN = Pattern.compile("(?s)if\\s*\\([^{]*\\{[^{}]*if\\s*\\([^{]*\\{[^{}]*if\\s*\\([^{]*\\{[^{}]*if\\s*\\(");
    private static final Pattern MAGIC_NUMBER_PATTERN = Pattern.compile("\\b(?<!\\.)(?:(?:[2-9]|[1-9][0-9]+)(?:\\.[0-9]+)?)\\b(?!\\s*[)}]|\\s*;\\s*//)");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("(?i)(select|insert|update|delete).*\\+.*[\"'].*[\"']");
    private static final Pattern POOR_NAMING_PATTERN = Pattern.compile("(?:public|private|protected)\\s+\\w+\\s+(get|set|do|handle|process|manage|data|info|obj|temp|var)\\d*\\s*\\(");
    private static final Pattern EMPTY_CATCH_PATTERN = Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{\\s*(?://.*)?\\s*\\}");
    private static final Pattern INSECURE_RANDOM_PATTERN = Pattern.compile("new\\s+Random\\s*\\(");
    private static final Pattern WEAK_CRYPTO_PATTERN = Pattern.compile("(?i)(DES|MD5|SHA1)[\"']|getInstance\\s*\\(\\s*[\"'](DES|MD5|SHA1)[\"']");
    private static final Pattern MISSING_VALIDATION_PATTERN = Pattern.compile("@RequestBody\\s+(?!@Valid)\\w+");
    private static final Pattern EXPOSED_EXCEPTION_PATTERN = Pattern.compile("(?:printStackTrace|getMessage)\\(\\)");
    private static final Pattern STRING_CONCAT_LOOP_PATTERN = Pattern.compile("(?s)for\\s*\\([^{]*\\{[^{}]*\\w+\\s*\\+=?\\s*\\w+\\s*\\+");
    
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
        
        // Check for deep nesting
        if (DEEP_NESTING_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName, 0, ReviewFinding.FindingType.KISS_VIOLATION,
                ReviewFinding.Severity.HIGH,
                "Deep nesting detected (4+ levels)",
                "Avoid deep nesting. Use guard clauses, early returns, or extract methods.",
                "Deep nesting violation"
            ));
        }
        
        // Check for magic numbers
        Matcher magicMatcher = MAGIC_NUMBER_PATTERN.matcher(code);
        if (magicMatcher.find()) {
            findings.add(createFinding(
                fileName, getLineNumber(code, magicMatcher.start()),
                ReviewFinding.FindingType.BEST_PRACTICE,
                ReviewFinding.Severity.MEDIUM,
                "Magic number detected: " + magicMatcher.group(),
                "Extract magic numbers to named constants for better readability.",
                magicMatcher.group()
            ));
        }
        
        // Check for poor naming
        if (POOR_NAMING_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName, 0, ReviewFinding.FindingType.BEST_PRACTICE,
                ReviewFinding.Severity.MEDIUM,
                "Poor naming convention detected",
                "Use descriptive method names that clearly indicate their purpose.",
                "Vague method names"
            ));
        }
        
        // Check for string concatenation in loops
        if (STRING_CONCAT_LOOP_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName, 0, ReviewFinding.FindingType.PERFORMANCE,
                ReviewFinding.Severity.MEDIUM,
                "String concatenation in loop detected",
                "Use StringBuilder for efficient string concatenation in loops.",
                "Inefficient string concatenation"
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
        
        // Check for SQL injection vulnerabilities
        if (SQL_INJECTION_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName, 0, ReviewFinding.FindingType.SECURITY,
                ReviewFinding.Severity.CRITICAL,
                "Potential SQL injection vulnerability",
                "Use parameterized queries or prepared statements instead of string concatenation.",
                "SQL injection risk"
            ));
        }
        
        // Check for insecure random usage
        if (INSECURE_RANDOM_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName, 0, ReviewFinding.FindingType.SECURITY,
                ReviewFinding.Severity.HIGH,
                "Insecure random number generation",
                "Use SecureRandom instead of Random for security-sensitive operations.",
                "Insecure Random usage"
            ));
        }
        
        // Check for weak cryptography
        if (WEAK_CRYPTO_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName, 0, ReviewFinding.FindingType.SECURITY,
                ReviewFinding.Severity.HIGH,
                "Weak cryptographic algorithm detected",
                "Use strong cryptographic algorithms like AES, SHA-256, or SHA-3.",
                "Weak cryptography"
            ));
        }
        
        // Check for missing input validation
        if (MISSING_VALIDATION_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName, 0, ReviewFinding.FindingType.SECURITY,
                ReviewFinding.Severity.MEDIUM,
                "Missing input validation",
                "Add @Valid annotation to validate input data automatically.",
                "Missing validation"
            ));
        }
        
        // Check for exposed exception information
        if (EXPOSED_EXCEPTION_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName, 0, ReviewFinding.FindingType.SECURITY,
                ReviewFinding.Severity.MEDIUM,
                "Exception information exposure",
                "Avoid exposing internal exception details to clients.",
                "Information disclosure"
            ));
        }
        
        // Check for empty catch blocks
        if (EMPTY_CATCH_PATTERN.matcher(code).find()) {
            findings.add(createFinding(
                fileName, 0, ReviewFinding.FindingType.BEST_PRACTICE,
                ReviewFinding.Severity.HIGH,
                "Empty catch block detected",
                "Handle exceptions properly or at least log them for debugging.",
                "Empty exception handling"
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
    
    private int getLineNumber(String code, int position) {
        if (position < 0 || position >= code.length()) {
            return 1;
        }
        
        String beforePosition = code.substring(0, position);
        return (int) beforePosition.chars().filter(ch -> ch == '\n').count() + 1;
    }
}
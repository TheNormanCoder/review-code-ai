package com.reviewcode.ai.service;

import com.reviewcode.ai.config.ReviewConfiguration;
import com.reviewcode.ai.model.ReviewFinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ConfigurableArchitectureValidationService extends ArchitectureValidationService {
    
    private final ReviewConfiguration reviewConfig;
    
    // Enhanced patterns with configuration support
    private static final Pattern LONG_METHOD_PATTERN = Pattern.compile("(?s)public\\s+\\w+[^{]*\\{([^{}]*\\{[^{}]*\\}[^{}]*)*[^{}]*\\}");
    private static final Pattern MANY_PARAMETERS_PATTERN = Pattern.compile("\\([^)]*,.*,.*,.*,.*,.*[^)]*\\)");
    private static final Pattern HARDCODED_SECRET_PATTERN = Pattern.compile("(?i)(password|secret|apikey|token|key)\\s*[=:]\\s*[\"'][^\"']{8,}[\"']");
    private static final Pattern FIELD_INJECTION_PATTERN = Pattern.compile("@Autowired\\s+private");
    private static final Pattern SELECT_ALL_PATTERN = Pattern.compile("(?i)select\\s+\\*\\s+from");
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

    @Autowired
    public ConfigurableArchitectureValidationService(ReviewConfiguration reviewConfig) {
        this.reviewConfig = reviewConfig;
    }

    @Override
    public List<ReviewFinding> validateArchitecturalPrinciples(String fileName, String code) {
        List<ReviewFinding> findings = new ArrayList<>();
        
        // Skip files matching ignore patterns
        if (shouldIgnoreFile(fileName)) {
            return findings;
        }
        
        // Check if file is critical (higher scrutiny)
        boolean isCriticalFile = isCriticalFile(fileName);
        
        // Apply validations based on configuration
        if (reviewConfig.getRules().isEnableCleanCode()) {
            findings.addAll(validateCleanCodePrinciples(fileName, code, isCriticalFile));
        }
        
        if (reviewConfig.getRules().isEnableSolid()) {
            findings.addAll(validateSolidPrinciples(fileName, code, isCriticalFile));
        }
        
        if (reviewConfig.getRules().isEnableDdd()) {
            findings.addAll(validateDddPrinciples(fileName, code, isCriticalFile));
        }
        
        if (reviewConfig.getRules().isEnableSecurity()) {
            findings.addAll(validateSecurityPrinciples(fileName, code, isCriticalFile));
        }
        
        if (reviewConfig.getRules().isEnablePerformance()) {
            findings.addAll(validatePerformancePrinciples(fileName, code, isCriticalFile));
        }
        
        // Filter disabled rules
        findings = filterByRuleConfiguration(findings);
        
        // Apply custom severity overrides
        findings = applySeverityOverrides(findings);
        
        return findings;
    }

    private List<ReviewFinding> validateCleanCodePrinciples(String fileName, String code, boolean isCriticalFile) {
        List<ReviewFinding> findings = new ArrayList<>();
        
        // Configurable method length check
        if (isMethodTooLong(code)) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.KISS_VIOLATION,
                isCriticalFile ? ReviewFinding.Severity.HIGH : ReviewFinding.Severity.MEDIUM,
                "Method exceeds " + reviewConfig.getThresholds().getMaxMethodLength() + " lines",
                "Break down long methods into smaller, focused methods.",
                "Long method detected"
            ));
        }
        
        // Configurable parameter count check
        if (hasTooManyParameters(code)) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.BEST_PRACTICE,
                ReviewFinding.Severity.MEDIUM,
                "Method has more than " + reviewConfig.getThresholds().getMaxParameters() + " parameters",
                "Consider using a parameter object or builder pattern.",
                "Too many parameters"
            ));
        }
        
        // Deep nesting check
        if (DEEP_NESTING_PATTERN.matcher(code).find()) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.KISS_VIOLATION,
                isCriticalFile ? ReviewFinding.Severity.CRITICAL : ReviewFinding.Severity.HIGH,
                "Deep nesting detected (4+ levels)",
                "Use guard clauses, early returns, or extract methods.",
                "Deep nesting violation"
            ));
        }
        
        // Magic numbers check with whitelist
        findings.addAll(detectMagicNumbers(fileName, code));
        
        // Poor naming check
        if (POOR_NAMING_PATTERN.matcher(code).find()) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.BEST_PRACTICE,
                ReviewFinding.Severity.MEDIUM,
                "Poor naming convention detected",
                "Use descriptive method names that clearly indicate their purpose.",
                "Vague method names"
            ));
        }
        
        // Empty catch blocks
        if (EMPTY_CATCH_PATTERN.matcher(code).find()) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.BEST_PRACTICE,
                isCriticalFile ? ReviewFinding.Severity.HIGH : ReviewFinding.Severity.MEDIUM,
                "Empty catch block detected",
                "Handle exceptions properly or at least log them.",
                "Empty exception handling"
            ));
        }
        
        return findings;
    }

    private List<ReviewFinding> validateSolidPrinciples(String fileName, String code, boolean isCriticalFile) {
        List<ReviewFinding> findings = new ArrayList<>();
        
        // Field injection check
        if (FIELD_INJECTION_PATTERN.matcher(code).find()) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.DEPENDENCY_INJECTION,
                ReviewFinding.Severity.MEDIUM,
                "Field injection detected",
                "Use constructor injection for better testability.",
                "@Autowired field injection"
            ));
        }
        
        // Class size check
        long lineCount = code.lines().count();
        if (lineCount > reviewConfig.getThresholds().getMaxClassLength()) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.SOLID_PRINCIPLES,
                isCriticalFile ? ReviewFinding.Severity.CRITICAL : ReviewFinding.Severity.HIGH,
                "Class is too large (" + lineCount + " lines, max: " + reviewConfig.getThresholds().getMaxClassLength() + ")",
                "Large classes violate Single Responsibility Principle. Break into smaller classes.",
                "Large class detected"
            ));
        }
        
        return findings;
    }

    private List<ReviewFinding> validateDddPrinciples(String fileName, String code, boolean isCriticalFile) {
        List<ReviewFinding> findings = new ArrayList<>();
        
        // Entity without ID check
        if (code.contains("@Entity") && !code.contains("@Id")) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.DDD_AGGREGATE,
                ReviewFinding.Severity.HIGH,
                "Entity missing @Id annotation",
                "Domain entities must have identity. Add @Id annotation.",
                "@Entity without @Id"
            ));
        }
        
        return findings;
    }

    private List<ReviewFinding> validateSecurityPrinciples(String fileName, String code, boolean isCriticalFile) {
        List<ReviewFinding> findings = new ArrayList<>();
        
        // Skip security checks for whitelisted files
        if (shouldSkipSecurityChecks(fileName)) {
            return findings;
        }
        
        // Hardcoded secrets with whitelist
        findings.addAll(detectHardcodedSecrets(fileName, code, isCriticalFile));
        
        // SQL injection check
        if (SQL_INJECTION_PATTERN.matcher(code).find()) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.SECURITY,
                ReviewFinding.Severity.CRITICAL,
                "Potential SQL injection vulnerability",
                "Use parameterized queries instead of string concatenation.",
                "SQL injection risk"
            ));
        }
        
        // Insecure random check
        if (INSECURE_RANDOM_PATTERN.matcher(code).find()) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.SECURITY,
                isCriticalFile ? ReviewFinding.Severity.CRITICAL : ReviewFinding.Severity.HIGH,
                "Insecure random number generation",
                "Use SecureRandom for security-sensitive operations.",
                "Insecure Random usage"
            ));
        }
        
        // Weak cryptography check
        if (WEAK_CRYPTO_PATTERN.matcher(code).find()) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.SECURITY,
                ReviewFinding.Severity.HIGH,
                "Weak cryptographic algorithm detected",
                "Use strong algorithms like AES, SHA-256, or SHA-3.",
                "Weak cryptography"
            ));
        }
        
        // Missing validation check
        if (MISSING_VALIDATION_PATTERN.matcher(code).find()) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.SECURITY,
                ReviewFinding.Severity.MEDIUM,
                "Missing input validation",
                "Add @Valid annotation to validate input data.",
                "Missing validation"
            ));
        }
        
        // Exposed exception information
        if (EXPOSED_EXCEPTION_PATTERN.matcher(code).find()) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.SECURITY,
                ReviewFinding.Severity.MEDIUM,
                "Exception information exposure",
                "Avoid exposing internal exception details to clients.",
                "Information disclosure"
            ));
        }
        
        return findings;
    }

    private List<ReviewFinding> validatePerformancePrinciples(String fileName, String code, boolean isCriticalFile) {
        List<ReviewFinding> findings = new ArrayList<>();
        
        // SELECT * check
        if (SELECT_ALL_PATTERN.matcher(code).find()) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.PERFORMANCE,
                ReviewFinding.Severity.MEDIUM,
                "SELECT * query detected",
                "Specify only required columns for better performance.",
                "SELECT * query"
            ));
        }
        
        // String concatenation in loops
        if (STRING_CONCAT_LOOP_PATTERN.matcher(code).find()) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.PERFORMANCE,
                ReviewFinding.Severity.MEDIUM,
                "String concatenation in loop detected",
                "Use StringBuilder for efficient string concatenation.",
                "Inefficient string concatenation"
            ));
        }
        
        // N+1 query check
        if (code.contains("@OneToMany") && !code.contains("fetch = FetchType.LAZY")) {
            findings.add(createConfigurableFinding(
                fileName, 0, ReviewFinding.FindingType.PERFORMANCE,
                ReviewFinding.Severity.MEDIUM,
                "Potential N+1 query problem",
                "Use LAZY loading for @OneToMany relationships.",
                "@OneToMany without LAZY loading"
            ));
        }
        
        return findings;
    }

    private boolean shouldIgnoreFile(String fileName) {
        return reviewConfig.getPatterns().getIgnoreFiles().stream()
            .anyMatch(pattern -> matchesPattern(fileName, pattern));
    }

    private boolean isCriticalFile(String fileName) {
        return reviewConfig.getPatterns().getCriticalFiles().stream()
            .anyMatch(pattern -> matchesPattern(fileName, pattern));
    }

    private boolean shouldSkipSecurityChecks(String fileName) {
        return reviewConfig.getPatterns().getWhitelist().getSkipSecurityChecks().stream()
            .anyMatch(pattern -> matchesPattern(fileName, pattern));
    }

    private boolean matchesPattern(String fileName, String pattern) {
        String regex = pattern.replace("*", ".*").replace("?", ".");
        return fileName.matches(regex);
    }

    private boolean isMethodTooLong(String code) {
        // Simple line counting approach - could be enhanced
        String[] lines = code.split("\\n");
        int maxLength = reviewConfig.getThresholds().getMaxMethodLength();
        
        boolean inMethod = false;
        int methodLineCount = 0;
        
        for (String line : lines) {
            if (line.trim().matches(".*public\\s+\\w+.*\\{.*")) {
                inMethod = true;
                methodLineCount = 1;
            } else if (inMethod) {
                methodLineCount++;
                if (line.trim().equals("}")) {
                    if (methodLineCount > maxLength) {
                        return true;
                    }
                    inMethod = false;
                    methodLineCount = 0;
                }
            }
        }
        
        return false;
    }

    private boolean hasTooManyParameters(String code) {
        int maxParams = reviewConfig.getThresholds().getMaxParameters();
        Pattern dynamicPattern = Pattern.compile("\\([^)]*" + ",.*".repeat(Math.max(0, maxParams)) + "[^)]*\\)");
        return dynamicPattern.matcher(code).find();
    }

    private List<ReviewFinding> detectMagicNumbers(String fileName, String code) {
        List<ReviewFinding> findings = new ArrayList<>();
        List<String> whitelist = reviewConfig.getPatterns().getWhitelist().getMagicNumbers();
        
        Matcher matcher = MAGIC_NUMBER_PATTERN.matcher(code);
        while (matcher.find()) {
            String number = matcher.group();
            if (!whitelist.contains(number)) {
                findings.add(createConfigurableFinding(
                    fileName, getLineNumber(code, matcher.start()),
                    ReviewFinding.FindingType.BEST_PRACTICE,
                    ReviewFinding.Severity.MEDIUM,
                    "Magic number detected: " + number,
                    "Extract magic numbers to named constants.",
                    number
                ));
            }
        }
        
        return findings;
    }

    private List<ReviewFinding> detectHardcodedSecrets(String fileName, String code, boolean isCriticalFile) {
        List<ReviewFinding> findings = new ArrayList<>();
        List<String> allowedSecrets = reviewConfig.getPatterns().getWhitelist().getAllowedSecrets();
        
        Matcher matcher = HARDCODED_SECRET_PATTERN.matcher(code);
        while (matcher.find()) {
            String secret = matcher.group();
            boolean isAllowed = allowedSecrets.stream()
                .anyMatch(allowed -> secret.toLowerCase().contains(allowed.toLowerCase()));
                
            if (!isAllowed) {
                findings.add(createConfigurableFinding(
                    fileName, getLineNumber(code, matcher.start()),
                    ReviewFinding.FindingType.SECURITY,
                    isCriticalFile ? ReviewFinding.Severity.CRITICAL : ReviewFinding.Severity.HIGH,
                    "Hardcoded secret detected",
                    "Use configuration properties or environment variables.",
                    "Hardcoded secret"
                ));
            }
        }
        
        return findings;
    }

    private List<ReviewFinding> filterByRuleConfiguration(List<ReviewFinding> findings) {
        List<String> disabledRules = reviewConfig.getRules().getDisabled();
        
        return findings.stream()
            .filter(finding -> !disabledRules.contains(finding.getRuleId()))
            .filter(finding -> !disabledRules.contains(finding.getType().name()))
            .collect(Collectors.toList());
    }

    private List<ReviewFinding> applySeverityOverrides(List<ReviewFinding> findings) {
        var severityOverrides = reviewConfig.getRules().getSeverity();
        
        return findings.stream()
            .map(finding -> {
                String override = severityOverrides.get(finding.getRuleId());
                if (override != null) {
                    try {
                        ReviewFinding.Severity newSeverity = ReviewFinding.Severity.valueOf(override.toUpperCase());
                        finding.setSeverity(newSeverity);
                    } catch (IllegalArgumentException e) {
                        // Invalid severity override, keep original
                    }
                }
                return finding;
            })
            .collect(Collectors.toList());
    }

    private ReviewFinding createConfigurableFinding(String fileName, int lineNumber,
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
package com.reviewcode.ai.config;

import com.reviewcode.ai.model.ReviewFinding;
import com.reviewcode.ai.service.ConfigurableArchitectureValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConfigurableArchitectureValidationServiceTest {

    private ConfigurableArchitectureValidationService validationService;
    private ReviewConfiguration reviewConfig;

    @BeforeEach
    void setUp() {
        reviewConfig = new ReviewConfiguration();
        validationService = new ConfigurableArchitectureValidationService(reviewConfig);
    }

    @Test
    void shouldRespectCustomThresholds() {
        // Given - Custom thresholds
        reviewConfig.getThresholds().setMaxMethodLength(10);
        reviewConfig.getThresholds().setMaxParameters(3);
        
        String codeWithMediumMethod = """
            public class TestService {
                public void shortMethod(String a, String b, String c, String d) {
                    // Line 1
                    // Line 2
                    // Line 3
                    // Line 4
                    // Line 5
                    // Line 6
                    // Line 7
                    // Line 8
                    // Line 9
                    // Line 10
                    // Line 11
                    // Line 12
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples("TestService.java", codeWithMediumMethod);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getDescription().contains("10 lines")));
        assertTrue(findings.stream().anyMatch(f -> 
            f.getDescription().contains("3 parameters")));
    }

    @Test
    void shouldIgnoreFilesMatchingPatterns() {
        // Given - Test file patterns
        reviewConfig.getPatterns().setIgnoreFiles(List.of("*Test.java", "*.spec.ts"));
        
        String problematicCode = """
            public class UserServiceTest {
                private String password = "hardcoded123";
                
                public void testMethod(String a, String b, String c, String d, String e, String f) {
                    // This has multiple violations but should be ignored
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples("UserServiceTest.java", problematicCode);

        // Then
        assertTrue(findings.isEmpty(), "Test files should be ignored");
    }

    @Test
    void shouldApplyStricterRulesToCriticalFiles() {
        // Given - Security file patterns
        reviewConfig.getPatterns().setCriticalFiles(List.of("*Security*.java", "*Auth*.java"));
        
        String authCode = """
            public class AuthenticationService {
                public void authenticate() {
                    if (user != null) {
                        if (user.isActive()) {
                            if (user.hasRole("admin")) {
                                if (user.isNotLocked()) {
                                    // Deep nesting in critical file
                                    login(user);
                                }
                            }
                        }
                    }
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples("AuthenticationService.java", authCode);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getSeverity() == ReviewFinding.Severity.CRITICAL &&
            f.getDescription().contains("Deep nesting")));
    }

    @Test
    void shouldRespectMagicNumberWhitelist() {
        // Given - Custom magic number whitelist
        reviewConfig.getPatterns().getWhitelist().setMagicNumbers(List.of("0", "1", "42", "100"));
        
        String codeWithNumbers = """
            public class MagicNumberService {
                public void processData() {
                    int allowedNumber = 42;  // Should be allowed
                    int notAllowed = 999;    // Should be flagged
                    int alsoAllowed = 100;   // Should be allowed
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples("MagicNumberService.java", codeWithNumbers);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getDescription().contains("999")));
        assertFalse(findings.stream().anyMatch(f -> 
            f.getDescription().contains("42")));
        assertFalse(findings.stream().anyMatch(f -> 
            f.getDescription().contains("100")));
    }

    @Test
    void shouldRespectSecretWhitelist() {
        // Given - Allowed test secrets
        reviewConfig.getPatterns().getWhitelist().setAllowedSecrets(List.of("test", "localhost", "example"));
        
        String codeWithSecrets = """
            public class ConfigService {
                private String testPassword = "test123";      // Should be allowed
                private String prodPassword = "realSecret";   // Should be flagged
                private String exampleKey = "example_key";    // Should be allowed
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples("ConfigService.java", codeWithSecrets);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getDescription().contains("Hardcoded secret") &&
            f.getCodeSnippet().contains("realSecret")));
        assertFalse(findings.stream().anyMatch(f -> 
            f.getCodeSnippet().contains("test123")));
        assertFalse(findings.stream().anyMatch(f -> 
            f.getCodeSnippet().contains("example_key")));
    }

    @Test
    void shouldDisableSpecificRules() {
        // Given - Disabled rules
        reviewConfig.getRules().setDisabled(List.of("ARCH_DEPENDENCY_INJECTION", "ARCH_BEST_PRACTICE"));
        
        String codeWithViolations = """
            @Service
            public class UserService {
                @Autowired
                private UserRepository repository; // Field injection - should be ignored
                
                public User getData(String id) {   // Poor naming - should be ignored
                    return repository.findById(id);
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples("UserService.java", codeWithViolations);

        // Then
        assertFalse(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.DEPENDENCY_INJECTION));
        assertFalse(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.BEST_PRACTICE));
    }

    @Test
    void shouldOverrideSeverity() {
        // Given - Severity overrides
        reviewConfig.getRules().setSeverity(Map.of(
            "ARCH_KISS_VIOLATION", "CRITICAL",
            "ARCH_DEPENDENCY_INJECTION", "LOW"
        ));
        
        String codeWithViolations = """
            @Service
            public class TestService {
                @Autowired
                private UserRepository repository;
                
                public void method() {
                    if (condition) {
                        if (anotherCondition) {
                            if (thirdCondition) {
                                if (fourthCondition) {
                                    // Deep nesting
                                }
                            }
                        }
                    }
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples("TestService.java", codeWithViolations);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.KISS_VIOLATION &&
            f.getSeverity() == ReviewFinding.Severity.CRITICAL));
    }

    @Test
    void shouldSkipSecurityChecksForWhitelistedFiles() {
        // Given - Skip security for test files
        reviewConfig.getPatterns().getWhitelist().setSkipSecurityChecks(List.of("**/test/**", "*Test.java"));
        
        String testCodeWithSecrets = """
            public class SecurityTest {
                private String password = "hardcodedPassword123";
                private String apiKey = "sk-1234567890abcdef";
                
                public void testMethod() {
                    String sql = "SELECT * FROM users WHERE id = '" + userId + "'";
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples("SecurityTest.java", testCodeWithSecrets);

        // Then - Should have no security findings
        assertFalse(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY));
        
        // But should still have other findings like performance issues
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.PERFORMANCE));
    }

    @Test
    void shouldDisableEntireRuleCategories() {
        // Given - Disable security and performance checks
        reviewConfig.getRules().setEnableSecurity(false);
        reviewConfig.getRules().setEnablePerformance(false);
        
        String problematicCode = """
            @Service
            public class ProblematicService {
                private String password = "hardcoded123";    // Security issue
                
                public List<User> getUsers() {
                    return jdbcTemplate.query("SELECT * FROM users", mapper); // Performance issue
                }
                
                @Autowired
                private UserRepository repository;           // DI issue (should remain)
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples("ProblematicService.java", problematicCode);

        // Then
        assertFalse(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY));
        assertFalse(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.PERFORMANCE));
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.DEPENDENCY_INJECTION));
    }

    @Test
    void shouldHandleEmptyConfiguration() {
        // Given - Minimal configuration (defaults)
        ReviewConfiguration minimalConfig = new ReviewConfiguration();
        ConfigurableArchitectureValidationService minimalService = 
            new ConfigurableArchitectureValidationService(minimalConfig);
        
        String normalCode = """
            @Service
            public class NormalService {
                private final UserRepository repository;
                
                public NormalService(UserRepository repository) {
                    this.repository = repository;
                }
                
                public User findUser(Long id) {
                    return repository.findById(id).orElse(null);
                }
            }
            """;

        // When
        List<ReviewFinding> findings = minimalService.validateArchitecturalPrinciples("NormalService.java", normalCode);

        // Then - Should work with defaults and have minimal findings
        assertTrue(findings.size() <= 2, "Should have minimal findings with clean code and default config");
    }
}
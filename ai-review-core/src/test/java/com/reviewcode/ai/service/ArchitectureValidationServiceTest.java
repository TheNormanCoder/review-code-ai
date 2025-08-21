package com.reviewcode.ai.service;

import com.reviewcode.ai.model.ReviewFinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ArchitectureValidationServiceTest {

    @InjectMocks
    private ArchitectureValidationService validationService;

    private String testFileName;

    @BeforeEach
    void setUp() {
        testFileName = "TestService.java";
    }

    @Test
    void shouldDetectLongMethods() {
        // Given
        String codeWithLongMethod = """
            public class TestService {
                public void longMethod() {
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
                    // Line 13
                    // Line 14
                    // Line 15
                    // Line 16
                    // Line 17
                    // Line 18
                    // Line 19
                    // Line 20
                    // Line 21
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, codeWithLongMethod);

        // Then
        assertFalse(findings.isEmpty());
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.KISS_VIOLATION &&
            f.getDescription().contains("too long")));
    }

    @Test
    void shouldDetectManyParameters() {
        // Given
        String codeWithManyParams = """
            public class TestService {
                public void methodWithManyParams(String a, String b, String c, String d, String e, String f) {
                    // method body
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, codeWithManyParams);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.BEST_PRACTICE &&
            f.getDescription().contains("too many parameters")));
    }

    @Test
    void shouldDetectFieldInjection() {
        // Given
        String codeWithFieldInjection = """
            @Service
            public class TestService {
                @Autowired
                private UserRepository userRepository;
                
                public void doSomething() {
                    // method body
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, codeWithFieldInjection);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.DEPENDENCY_INJECTION &&
            f.getDescription().contains("Field injection detected")));
    }

    @Test
    void shouldDetectHardcodedSecrets() {
        // Given
        String codeWithSecrets = """
            public class TestService {
                private String password = "mySecretPassword123";
                private String apiKey = "sk-1234567890abcdef";
                
                public void connect() {
                    // connection logic
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, codeWithSecrets);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY &&
            f.getSeverity() == ReviewFinding.Severity.CRITICAL &&
            f.getDescription().contains("Hardcoded secret")));
    }

    @Test
    void shouldDetectSelectAllQueries() {
        // Given
        String codeWithSelectAll = """
            @Repository
            public class UserRepository {
                @Query("SELECT * FROM users WHERE active = true")
                public List<User> findActiveUsers() {
                    return null;
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, codeWithSelectAll);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.PERFORMANCE &&
            f.getDescription().contains("SELECT * query")));
    }

    @Test
    void shouldDetectEntityWithoutId() {
        // Given
        String codeWithEntityNoId = """
            @Entity
            public class User {
                private String name;
                private String email;
                
                // getters and setters
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, codeWithEntityNoId);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.DDD_AGGREGATE &&
            f.getSeverity() == ReviewFinding.Severity.HIGH &&
            f.getDescription().contains("Entity missing @Id")));
    }

    @Test
    void shouldDetectPotentialN1Query() {
        // Given
        String codeWithN1Problem = """
            @Entity
            public class Order {
                @OneToMany
                private List<OrderItem> items;
                
                // other fields
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, codeWithN1Problem);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.PERFORMANCE &&
            f.getDescription().contains("N+1 query problem")));
    }

    @Test
    void shouldDetectLargeClass() {
        // Given
        StringBuilder largeClass = new StringBuilder();
        largeClass.append("public class LargeService {\n");
        for (int i = 0; i < 350; i++) {
            largeClass.append("    // Line ").append(i).append("\n");
        }
        largeClass.append("}");

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, largeClass.toString());

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SOLID_PRINCIPLES &&
            f.getSeverity() == ReviewFinding.Severity.HIGH &&
            f.getDescription().contains("too large")));
    }

    @Test
    void shouldDetectDuplicateCode() {
        // Given
        String codeWithDuplication = """
            public class TestService {
                public void method1() {
                    System.out.println("Starting operation");
                    validate();
                    process();
                    System.out.println("Operation completed");
                }
                
                public void method2() {
                    System.out.println("Starting operation");
                    validate();
                    process();
                    System.out.println("Operation completed");
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, codeWithDuplication);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.DRY_VIOLATION &&
            f.getDescription().contains("Duplicate code")));
    }

    @Test
    void shouldDetectHardDependencyCreation() {
        // Given
        String codeWithHardDependency = """
            public class OrderService {
                public void processOrder() {
                    EmailService emailService = new EmailService();
                    emailService.sendConfirmation();
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, codeWithHardDependency);

        // Then
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.DEPENDENCY_INJECTION &&
            f.getDescription().contains("hard dependency")));
    }

    @Test
    void shouldNotReportFindingsForCleanCode() {
        // Given
        String cleanCode = """
            @Service
            public class UserService {
                
                private final UserRepository userRepository;
                
                public UserService(UserRepository userRepository) {
                    this.userRepository = userRepository;
                }
                
                public User findById(Long id) {
                    return userRepository.findById(id).orElse(null);
                }
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, cleanCode);

        // Then
        // Should have minimal or no critical findings for this clean code
        assertTrue(findings.stream().noneMatch(f -> 
            f.getSeverity() == ReviewFinding.Severity.CRITICAL));
    }

    @Test
    void shouldCreateFindingWithCorrectProperties() {
        // Given
        String problemCode = """
            @Service
            public class TestService {
                @Autowired
                private SomeRepository repository;
            }
            """;

        // When
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, problemCode);

        // Then
        ReviewFinding finding = findings.stream()
            .filter(f -> f.getType() == ReviewFinding.FindingType.DEPENDENCY_INJECTION)
            .findFirst()
            .orElse(null);

        assertNotNull(finding);
        assertEquals(testFileName, finding.getFileName());
        assertNotNull(finding.getDescription());
        assertNotNull(finding.getSuggestion());
        assertTrue(finding.getRuleId().startsWith("ARCH_"));
    }
}
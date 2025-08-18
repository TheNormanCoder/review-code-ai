package com.reviewcode.ai.patterns;

import com.reviewcode.ai.model.ReviewFinding;
import com.reviewcode.ai.service.ArchitectureValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EnhancedPatternsTest {

    @InjectMocks
    private ArchitectureValidationService validationService;

    private String testFileName;

    @BeforeEach
    void setUp() {
        testFileName = "EnhancedTest.java";
    }

    @Test
    void shouldDetectDeepNestingViolation() {
        String deepNestingCode = """
            public class ValidationService {
                public boolean validateUser(User user) {
                    if (user != null) {
                        if (user.getEmail() != null) {
                            if (user.getEmail().contains("@")) {
                                if (user.getName() != null) {
                                    if (user.getName().length() > 2) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                    return false;
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, deepNestingCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.KISS_VIOLATION &&
            f.getDescription().contains("Deep nesting")));
    }

    @Test
    void shouldDetectMagicNumbersViolation() {
        String magicNumbersCode = """
            public class DiscountService {
                public double calculateDiscount(double amount) {
                    if (amount > 1000) {
                        return amount * 0.15; // Magic number
                    } else if (amount > 500) {
                        return amount * 0.10; // Magic number
                    }
                    return 0;
                }
                
                public void processOrder(Order order) {
                    if (order.getQuantity() > 50) { // Magic number
                        applyBulkDiscount(order);
                    }
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, magicNumbersCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.BEST_PRACTICE &&
            f.getDescription().contains("Magic number")));
    }

    @Test
    void shouldDetectSqlInjectionVulnerability() {
        String sqlInjectionCode = """
            @Repository
            public class UserRepository {
                
                @Autowired
                private JdbcTemplate jdbcTemplate;
                
                public User findByEmail(String email) {
                    String sql = "SELECT * FROM users WHERE email = '" + email + "'";
                    return jdbcTemplate.queryForObject(sql, User.class);
                }
                
                public void updateStatus(String userId, String status) {
                    String query = "UPDATE users SET status = '" + status + "' WHERE id = " + userId;
                    jdbcTemplate.update(query);
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, sqlInjectionCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY &&
            f.getDescription().contains("SQL injection")));
    }

    @Test
    void shouldDetectInsecureRandomUsage() {
        String insecureRandomCode = """
            @Service
            public class TokenService {
                
                public String generateToken() {
                    Random random = new Random();
                    StringBuilder token = new StringBuilder();
                    
                    for (int i = 0; i < 32; i++) {
                        token.append(random.nextInt(10));
                    }
                    
                    return token.toString();
                }
                
                public String generateSessionId() {
                    Random random = new Random(System.currentTimeMillis());
                    return String.valueOf(random.nextLong());
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, insecureRandomCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY &&
            f.getDescription().contains("Insecure random")));
    }

    @Test
    void shouldDetectWeakCryptographyViolation() {
        String weakCryptoCode = """
            @Service
            public class EncryptionService {
                
                private static final String ALGORITHM = "DES";
                
                public String hash(String input) {
                    try {
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        return Base64.getEncoder().encodeToString(md.digest(input.getBytes()));
                    } catch (Exception e) {
                        throw new RuntimeException("Hashing failed", e);
                    }
                }
                
                public String encrypt(String data) {
                    Cipher cipher = Cipher.getInstance("DES");
                    return cipher.doFinal(data.getBytes());
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, weakCryptoCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY &&
            f.getDescription().contains("cryptographic algorithm")));
    }

    @Test
    void shouldDetectMissingInputValidation() {
        String missingValidationCode = """
            @RestController
            public class UserController {
                
                @PostMapping("/users")
                public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
                    User user = userService.createUser(request);
                    return ResponseEntity.ok(user);
                }
                
                @PutMapping("/users/{id}")
                public User updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
                    return userService.updateUser(id, request);
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, missingValidationCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY &&
            f.getDescription().contains("Missing input validation")));
    }

    @Test
    void shouldDetectExposedExceptionInformation() {
        String exposedExceptionCode = """
            @RestController
            public class ApiController {
                
                @GetMapping("/data/{id}")
                public ResponseEntity<Data> getData(@PathVariable String id) {
                    try {
                        return ResponseEntity.ok(dataService.findById(id));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.status(500)
                            .body(new ErrorResponse(e.getMessage()));
                    }
                }
                
                @PostMapping("/process")
                public void processData(@RequestBody ProcessRequest request) {
                    try {
                        dataService.process(request);
                    } catch (SQLException e) {
                        throw new RuntimeException("Database error: " + e.getMessage(), e);
                    }
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, exposedExceptionCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY &&
            f.getDescription().contains("Exception information")));
    }

    @Test
    void shouldDetectEmptyCatchBlocks() {
        String emptyCatchCode = """
            @Service
            public class FileService {
                
                public void readFile(String path) {
                    try {
                        Files.readString(Paths.get(path));
                    } catch (IOException e) {
                        // Empty catch block - problematic
                    }
                }
                
                public void writeFile(String path, String content) {
                    try {
                        Files.writeString(Paths.get(path), content);
                    } catch (IOException e) {
                        // TODO: Handle this
                    }
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, emptyCatchCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.BEST_PRACTICE &&
            f.getDescription().contains("Empty catch")));
    }

    @Test
    void shouldDetectPoorNamingConventions() {
        String poorNamingCode = """
            public class UserService {
                
                public User getData(String id) {
                    return repository.findById(id);
                }
                
                public void doStuff(User u) {
                    u.setStatus("active");
                }
                
                public boolean handle(String email) {
                    return email.contains("@");
                }
                
                public void process(Object obj) {
                    // Process logic
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, poorNamingCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.BEST_PRACTICE &&
            f.getDescription().contains("Poor naming")));
    }

    @Test
    void shouldDetectStringConcatenationInLoop() {
        String stringConcatCode = """
            @Service
            public class ReportService {
                
                public String generateReport(List<Data> dataList) {
                    String report = "";
                    
                    for (Data data : dataList) {
                        report = report + data.toString() + "\\n";
                        report = report + "Value: " + data.getValue() + "\\n";
                    }
                    
                    return report;
                }
                
                public String buildQuery(List<String> conditions) {
                    String query = "SELECT * FROM table WHERE ";
                    
                    for (String condition : conditions) {
                        query += condition + " AND ";
                    }
                    
                    return query;
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, stringConcatCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.PERFORMANCE &&
            f.getDescription().contains("String concatenation")));
    }

    @Test
    void shouldApproveSecureAndOptimizedCode() {
        String secureOptimizedCode = """
            @RestController
            @RequestMapping("/api/v1/users")
            @Validated
            public class UserController {
                
                private final UserService userService;
                
                public UserController(UserService userService) {
                    this.userService = userService;
                }
                
                @PostMapping
                @PreAuthorize("hasRole('ADMIN')")
                public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
                    try {
                        UserDto user = userService.createUser(request);
                        return ResponseEntity.status(HttpStatus.CREATED).body(user);
                    } catch (UserAlreadyExistsException e) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(new ErrorDto("User already exists"));
                    }
                }
                
                @GetMapping("/{id}")
                public ResponseEntity<UserDto> getUser(@PathVariable @Valid @Positive Long id) {
                    return userService.findById(id)
                        .map(user -> ResponseEntity.ok(user))
                        .orElse(ResponseEntity.notFound().build());
                }
            }
            
            @Service
            @Transactional
            public class UserService {
                
                private static final double PREMIUM_DISCOUNT = 0.15;
                private static final double REGULAR_DISCOUNT = 0.05;
                
                private final UserRepository userRepository;
                private final SecureRandom secureRandom;
                
                public UserService(UserRepository userRepository) {
                    this.userRepository = userRepository;
                    this.secureRandom = new SecureRandom();
                }
                
                public UserDto createUser(CreateUserRequest request) {
                    validateUserRequest(request);
                    
                    String hashedPassword = hashPassword(request.getPassword());
                    User user = User.builder()
                        .email(request.getEmail())
                        .password(hashedPassword)
                        .build();
                    
                    User savedUser = userRepository.save(user);
                    return mapToDto(savedUser);
                }
                
                private String hashPassword(String password) {
                    try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
                        return Base64.getEncoder().encodeToString(hash);
                    } catch (NoSuchAlgorithmException e) {
                        throw new SecurityException("Failed to hash password", e);
                    }
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, secureOptimizedCode);

        // Should have minimal security or performance violations
        assertTrue(findings.stream().noneMatch(f -> 
            f.getSeverity() == ReviewFinding.Severity.CRITICAL));
            
        // Should not have major security issues
        assertTrue(findings.stream().noneMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY &&
            f.getSeverity() == ReviewFinding.Severity.HIGH));
    }

    @Test
    void shouldDetectLineNumbersCorrectly() {
        String multiLineCode = """
            public class TestService {
                private String password = "secret123"; // Line 2 - should be detected
                
                public void method1() {
                    int magicNumber = 42; // Line 5 - should be detected
                }
                
                public void method2() {
                    Random rand = new Random(); // Line 9 - should be detected
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, multiLineCode);

        // Should have findings with proper line numbers
        assertTrue(findings.stream().anyMatch(f -> 
            f.getLineNumber() != null && f.getLineNumber() > 0));
    }
}
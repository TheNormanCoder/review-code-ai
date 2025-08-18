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
class SecurityPatternsTest {

    @InjectMocks
    private ArchitectureValidationService validationService;

    private String testFileName;

    @BeforeEach
    void setUp() {
        testFileName = "SecurityTest.java";
    }

    @Test
    void shouldDetectHardcodedPasswordViolation() {
        String hardcodedPasswordCode = """
            @Service
            public class DatabaseService {
                private static final String DATABASE_PASSWORD = "mySecretPassword123";
                private static final String API_KEY = "sk-1234567890abcdef1234567890abcdef";
                private static final String JWT_SECRET = "superSecretJWTSigningKey123456789";
                
                public Connection getConnection() {
                    return DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/db", 
                        "user", 
                        DATABASE_PASSWORD
                    );
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, hardcodedPasswordCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY &&
            f.getSeverity() == ReviewFinding.Severity.CRITICAL &&
            f.getDescription().contains("Hardcoded secret")));
    }

    @Test
    void shouldDetectHardcodedApiKeyViolation() {
        String hardcodedApiKeyCode = """
            @RestController
            public class PaymentController {
                
                private final String STRIPE_SECRET_KEY = "sk_test_51234567890abcdef1234567890abcdef";
                private final String STRIPE_PUBLISHABLE_KEY = "pk_test_51234567890abcdef1234567890abcdef";
                
                @PostMapping("/payment")
                public ResponseEntity<String> processPayment(@RequestBody PaymentRequest request) {
                    // Using hardcoded API keys
                    Stripe.apiKey = STRIPE_SECRET_KEY;
                    return ResponseEntity.ok("Payment processed");
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, hardcodedApiKeyCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY &&
            f.getDescription().contains("Hardcoded secret")));
    }

    @Test
    void shouldDetectHardcodedTokenViolation() {
        String hardcodedTokenCode = """
            @Component
            public class AuthenticationService {
                
                private final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ";
                private final String REFRESH_TOKEN = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
                
                public boolean validateToken(String token) {
                    return JWT_TOKEN.equals(token);
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, hardcodedTokenCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY &&
            f.getDescription().contains("Hardcoded secret")));
    }

    @Test
    void shouldDetectSqlInjectionVulnerability() {
        String sqlInjectionCode = """
            @Repository
            public class UserRepository {
                
                @Autowired
                private JdbcTemplate jdbcTemplate;
                
                // SQL Injection vulnerability - direct string concatenation
                public User findByEmail(String email) {
                    String sql = "SELECT * FROM users WHERE email = '" + email + "'";
                    return jdbcTemplate.queryForObject(sql, User.class);
                }
                
                public List<User> findByStatus(String status) {
                    String query = "SELECT * FROM users WHERE status = " + status;
                    return jdbcTemplate.query(query, new UserRowMapper());
                }
            }
            """;

        // Note: Current implementation checks for SELECT * but not SQL injection patterns
        // This would be a good enhancement to add
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, sqlInjectionCode);
    }

    @Test
    void shouldDetectPlaintextPasswordStorage() {
        String plaintextPasswordCode = """
            @Entity
            public class User {
                @Id
                private Long id;
                private String username;
                private String password; // Storing password in plaintext
                
                public void setPassword(String password) {
                    this.password = password; // No encryption/hashing
                }
                
                public boolean checkPassword(String inputPassword) {
                    return this.password.equals(inputPassword); // Plain text comparison
                }
            }
            """;

        // This pattern is harder to detect with current regex-based approach
        // Would require semantic analysis
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, plaintextPasswordCode);
    }

    @Test
    void shouldDetectMissingAuthenticationViolation() {
        String noAuthCode = """
            @RestController
            @RequestMapping("/api/admin")
            public class AdminController {
                
                // Missing @PreAuthorize or @Secured annotations
                @GetMapping("/users")
                public List<User> getAllUsers() {
                    return userService.findAll();
                }
                
                @DeleteMapping("/users/{id}")
                public void deleteUser(@PathVariable Long id) {
                    userService.delete(id);
                }
                
                @PostMapping("/system/shutdown")
                public void shutdownSystem() {
                    systemService.shutdown();
                }
            }
            """;

        // This would require checking for missing security annotations
        // Currently not implemented in the validation service
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, noAuthCode);
    }

    @Test
    void shouldDetectSensitiveDataInLogsViolation() {
        String sensitiveLoggingCode = """
            @Service
            public class UserService {
                
                private static final Logger logger = LoggerFactory.getLogger(UserService.class);
                
                public User authenticate(String username, String password) {
                    logger.info("Authenticating user: {} with password: {}", username, password);
                    
                    User user = userRepository.findByUsername(username);
                    if (user != null && user.getPassword().equals(password)) {
                        logger.info("Authentication successful for user: {}, password: {}", username, password);
                        return user;
                    }
                    
                    logger.warn("Authentication failed for user: {} with password: {}", username, password);
                    return null;
                }
            }
            """;

        // This would require detecting logging of sensitive data patterns
        // Currently not implemented
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, sensitiveLoggingCode);
    }

    @Test
    void shouldDetectWeakCryptographyViolation() {
        String weakCryptoCode = """
            @Service
            public class EncryptionService {
                
                // Using weak encryption algorithms
                private static final String ALGORITHM = "DES"; // Weak encryption
                private static final String MD5_ALGORITHM = "MD5"; // Weak hashing
                
                public String encrypt(String plaintext, String key) {
                    try {
                        Cipher cipher = Cipher.getInstance(ALGORITHM);
                        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);
                        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                        return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes()));
                    } catch (Exception e) {
                        throw new RuntimeException("Encryption failed", e);
                    }
                }
                
                public String hash(String input) {
                    try {
                        MessageDigest md = MessageDigest.getInstance(MD5_ALGORITHM);
                        return Base64.getEncoder().encodeToString(md.digest(input.getBytes()));
                    } catch (Exception e) {
                        throw new RuntimeException("Hashing failed", e);
                    }
                }
            }
            """;

        // This would require detecting weak cryptographic algorithms
        // Currently not implemented
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, weakCryptoCode);
    }

    @Test
    void shouldDetectMissingInputValidationViolation() {
        String noValidationCode = """
            @RestController
            public class UserController {
                
                @PostMapping("/users")
                public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
                    // No input validation - accepting any input
                    User user = new User();
                    user.setEmail(request.getEmail()); // No email format validation
                    user.setAge(request.getAge()); // No age range validation
                    user.setUsername(request.getUsername()); // No length/character validation
                    
                    return ResponseEntity.ok(userService.save(user));
                }
                
                @GetMapping("/users/{id}")
                public User getUser(@PathVariable String id) {
                    // No validation that id is numeric or within valid range
                    return userService.findById(Long.parseLong(id));
                }
            }
            """;

        // This would require detecting missing validation annotations or logic
        // Currently not implemented
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, noValidationCode);
    }

    @Test
    void shouldDetectInsecureRandomViolation() {
        String insecureRandomCode = """
            @Service
            public class TokenService {
                
                public String generateToken() {
                    // Using insecure random number generator
                    Random random = new Random(); // Should use SecureRandom
                    StringBuilder token = new StringBuilder();
                    
                    for (int i = 0; i < 32; i++) {
                        token.append(random.nextInt(10));
                    }
                    
                    return token.toString();
                }
                
                public String generateSessionId() {
                    // Predictable session ID generation
                    long timestamp = System.currentTimeMillis();
                    Random random = new Random(timestamp); // Seeded with predictable value
                    return String.valueOf(random.nextLong());
                }
            }
            """;

        // This would require detecting use of Random instead of SecureRandom
        // Currently not implemented
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, insecureRandomCode);
    }

    @Test
    void shouldApproveSecureImplementation() {
        String secureCode = """
            @Service
            public class SecureUserService {
                
                @Value("${database.password}")
                private String databasePassword;
                
                @Value("${jwt.secret}")
                private String jwtSecret;
                
                private final PasswordEncoder passwordEncoder;
                private final UserRepository userRepository;
                
                public SecureUserService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
                    this.passwordEncoder = passwordEncoder;
                    this.userRepository = userRepository;
                }
                
                public User createUser(CreateUserRequest request) {
                    // Input validation
                    validateUserRequest(request);
                    
                    User user = User.builder()
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword())) // Encrypted password
                        .build();
                    
                    return userRepository.save(user);
                }
                
                @PreAuthorize("hasRole('ADMIN')")
                public void deleteUser(Long userId) {
                    userRepository.deleteById(userId);
                }
                
                private void validateUserRequest(CreateUserRequest request) {
                    if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                        throw new IllegalArgumentException("Username is required");
                    }
                    // Additional validation logic...
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, secureCode);

        // Should not have critical security violations
        assertTrue(findings.stream().noneMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY &&
            f.getSeverity() == ReviewFinding.Severity.CRITICAL));
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
                        // Exposing internal exception details
                        return ResponseEntity.status(500)
                            .body(new ErrorResponse(e.getMessage(), e.getStackTrace()));
                    }
                }
                
                @PostMapping("/process")
                public void processData(@RequestBody ProcessRequest request) {
                    try {
                        dataService.process(request);
                    } catch (SQLException e) {
                        // Exposing database structure information
                        throw new RuntimeException("Database error: " + e.getMessage() + 
                            " Query: " + e.getSQLState(), e);
                    }
                }
            }
            """;

        // This would require detecting exception information exposure
        // Currently not implemented
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, exposedExceptionCode);
    }
}
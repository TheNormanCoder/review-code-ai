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
class CodeReviewIntegrationPatternsTest {

    @InjectMocks
    private ArchitectureValidationService validationService;

    private String testFileName;

    @BeforeEach
    void setUp() {
        testFileName = "IntegrationTest.java";
    }

    @Test
    void shouldDetectMultipleViolationsInSingleClass() {
        String multipleViolationsCode = """
            @Service
            public class BadUserService {
                
                @Autowired
                private UserRepository userRepository; // Field injection violation
                
                @Autowired  
                private EmailService emailService; // Field injection violation
                
                private String hardcodedPassword = "adminPassword123"; // Security violation
                
                // Long method with multiple responsibilities (Clean Code + SOLID violations)
                public User createUserWithEmailAndReports(String name, String email, String phone, 
                                                         String address, String city, String country,
                                                         String zipCode, String company, String title,
                                                         String department) { // Too many parameters
                    
                    // Input validation (20+ lines)
                    if (name == null || name.trim().isEmpty()) {
                        throw new IllegalArgumentException("Name is required");
                    }
                    if (email == null || email.trim().isEmpty()) {
                        throw new IllegalArgumentException("Email is required");
                    }
                    if (!email.contains("@")) {
                        throw new IllegalArgumentException("Invalid email format");
                    }
                    if (phone == null || phone.trim().isEmpty()) {
                        throw new IllegalArgumentException("Phone is required");
                    }
                    if (address == null || address.trim().isEmpty()) {
                        throw new IllegalArgumentException("Address is required");
                    }
                    if (city == null || city.trim().isEmpty()) {
                        throw new IllegalArgumentException("City is required");
                    }
                    if (country == null || country.trim().isEmpty()) {
                        throw new IllegalArgumentException("Country is required");
                    }
                    if (zipCode == null || zipCode.trim().isEmpty()) {
                        throw new IllegalArgumentException("ZipCode is required");
                    }
                    if (company == null || company.trim().isEmpty()) {
                        throw new IllegalArgumentException("Company is required");
                    }
                    if (title == null || title.trim().isEmpty()) {
                        throw new IllegalArgumentException("Title is required");
                    }
                    if (department == null || department.trim().isEmpty()) {
                        throw new IllegalArgumentException("Department is required");
                    }
                    
                    // User creation logic
                    User user = new User();
                    user.setName(name);
                    user.setEmail(email);
                    user.setPhone(phone);
                    user.setAddress(address);
                    user.setCity(city);
                    user.setCountry(country);
                    user.setZipCode(zipCode);
                    user.setCompany(company);
                    user.setTitle(title);
                    user.setDepartment(department);
                    user.setCreatedDate(new Date());
                    user.setStatus(UserStatus.ACTIVE);
                    
                    // Database operations
                    User savedUser = userRepository.save(user);
                    
                    // Email operations
                    emailService.sendWelcomeEmail(savedUser.getEmail());
                    emailService.sendNewUserNotificationToAdmins(savedUser);
                    
                    // Report generation
                    generateUserReport(savedUser);
                    updateUserStatistics();
                    
                    // Audit logging
                    auditService.logUserCreation(savedUser);
                    
                    return savedUser;
                }
                
                // SELECT * violation
                @Query("SELECT * FROM users WHERE active = true")
                public List<User> findAllActiveUsers() {
                    return null;
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, multipleViolationsCode);

        // Should detect multiple violations
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.DEPENDENCY_INJECTION));
        
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY));
            
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.BEST_PRACTICE));
            
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.PERFORMANCE));
    }

    @Test
    void shouldDetectCrossLayerViolations() {
        String crossLayerViolationCode = """
            @RestController
            public class UserController {
                
                @Autowired
                private UserRepository userRepository; // Controller accessing repository directly
                
                @Autowired
                private JdbcTemplate jdbcTemplate; // Controller with direct database access
                
                @PostMapping("/users")
                public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
                    
                    // Business logic in controller (should be in service)
                    if (request.getEmail().endsWith("@company.com")) {
                        request.setRole("EMPLOYEE");
                    } else {
                        request.setRole("CUSTOMER");
                    }
                    
                    // Direct database manipulation in controller
                    String checkQuery = "SELECT * FROM users WHERE email = '" + request.getEmail() + "'";
                    List<User> existingUsers = jdbcTemplate.query(checkQuery, new UserRowMapper());
                    
                    if (!existingUsers.isEmpty()) {
                        return ResponseEntity.badRequest().build();
                    }
                    
                    // Direct repository access from controller
                    User user = new User();
                    user.setEmail(request.getEmail());
                    user.setName(request.getName());
                    
                    User savedUser = userRepository.save(user);
                    
                    // Email logic in controller
                    emailService.sendWelcomeEmail(savedUser.getEmail());
                    
                    return ResponseEntity.ok(savedUser);
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, crossLayerViolationCode);

        // Should detect field injection and potentially SQL issues
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.DEPENDENCY_INJECTION));
    }

    @Test
    void shouldDetectDataModelLeakage() {
        String dataLeakageCode = """
            @RestController
            public class OrderController {
                
                // Exposing JPA entities directly in API
                @GetMapping("/orders/{id}")
                public OrderEntity getOrder(@PathVariable Long id) { // Should return DTO
                    return orderRepository.findById(id);
                }
                
                @PostMapping("/orders")
                public OrderEntity createOrder(@RequestBody OrderEntity order) { // Should use request DTO
                    return orderRepository.save(order);
                }
                
                @GetMapping("/orders")
                public List<OrderEntity> getAllOrders() { // Exposing internal data structure
                    return orderRepository.findAll(); // No pagination, no DTO mapping
                }
            }
            
            @Entity
            public class OrderEntity {
                @Id
                private Long id;
                
                private String internalNotes; // Internal field exposed to API
                private String adminComments; // Admin-only field exposed
                private double internalCost; // Cost calculation exposed
                
                // All getters/setters expose internal data
                public String getInternalNotes() { return internalNotes; }
                public String getAdminComments() { return adminComments; }
                public double getInternalCost() { return internalCost; }
            }
            """;

        // This would require detecting entity exposure in controllers
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, dataLeakageCode);
    }

    @Test
    void shouldDetectInconsistentErrorHandling() {
        String inconsistentErrorHandlingCode = """
            @Service
            public class InconsistentService {
                
                public User findUser(String id) {
                    try {
                        return userRepository.findById(id);
                    } catch (Exception e) {
                        return null; // Swallowing exception
                    }
                }
                
                public void deleteUser(String id) {
                    try {
                        userRepository.deleteById(id);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to delete"); // Generic exception
                    }
                }
                
                public User updateUser(String id, User user) {
                    User existing = userRepository.findById(id);
                    if (existing == null) {
                        throw new UserNotFoundException("User not found"); // Specific exception
                    }
                    return userRepository.save(user);
                }
                
                public List<User> searchUsers(String query) throws Exception { // Throws generic exception
                    if (query == null) {
                        throw new Exception("Query cannot be null");
                    }
                    return userRepository.findByNameContaining(query);
                }
            }
            """;

        // This would require detecting inconsistent exception handling patterns
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, inconsistentErrorHandlingCode);
    }

    @Test
    void shouldDetectTestingAntiPatterns() {
        String testingAntiPatternsCode = """
            @SpringBootTest // Heavy integration test for unit test
            class UserServiceTest {
                
                @Autowired
                private UserService userService; // Testing against real dependencies
                
                @Test
                void testCreateUser() {
                    // No setup/cleanup - tests affect each other
                    User user = new User("John", "john@example.com");
                    User created = userService.createUser(user);
                    
                    // Testing implementation details
                    verify(userRepository, times(1)).save(any());
                    
                    // Hard-coded expectations
                    assertEquals("John", created.getName());
                    assertTrue(created.getId() > 0);
                }
                
                @Test
                void testFindUser() {
                    // Depends on previous test state
                    User found = userService.findUser("john@example.com");
                    assertNotNull(found); // Brittle assertion
                }
                
                @Test
                void complexBusinessLogicTest() {
                    // Testing too much in one test
                    User user = userService.createUser(new User("Jane", "jane@example.com"));
                    emailService.sendWelcomeEmail(user.getEmail());
                    reportService.generateUserReport(user);
                    auditService.logUserCreation(user);
                    
                    // Multiple assertions across different concerns
                    assertNotNull(user);
                    verify(emailService).sendWelcomeEmail(anyString());
                    verify(reportService).generateUserReport(any());
                    verify(auditService).logUserCreation(any());
                }
            }
            """;

        // This would require detecting testing anti-patterns
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, testingAntiPatternsCode);
    }

    @Test
    void shouldDetectConfigurationAntiPatterns() {
        String configAntiPatternsCode = """
            @Configuration
            public class BadConfiguration {
                
                // Hard-coded values in configuration
                @Bean
                public DataSource dataSource() {
                    HikariDataSource ds = new HikariDataSource();
                    ds.setJdbcUrl("jdbc:mysql://localhost:3306/mydb"); // Hard-coded URL
                    ds.setUsername("root"); // Hard-coded username
                    ds.setPassword("password123"); // Hard-coded password
                    ds.setMaximumPoolSize(10); // Hard-coded pool size
                    return ds;
                }
                
                @Bean
                public RestTemplate restTemplate() {
                    RestTemplate template = new RestTemplate();
                    // No timeout configuration - potential hanging requests
                    return template;
                }
                
                @Bean
                public CacheManager cacheManager() {
                    SimpleCacheManager manager = new SimpleCacheManager();
                    // No cache configuration - using defaults
                    return manager;
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, configAntiPatternsCode);

        // Should detect hardcoded secrets
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SECURITY &&
            f.getDescription().contains("Hardcoded secret")));
    }

    @Test
    void shouldApproveWellStructuredIntegration() {
        String wellStructuredCode = """
            @RestController
            @RequestMapping("/api/users")
            @Validated
            public class UserController {
                
                private final UserService userService;
                
                public UserController(UserService userService) {
                    this.userService = userService;
                }
                
                @PostMapping
                @PreAuthorize("hasRole('ADMIN')")
                public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
                    UserDto user = userService.createUser(request);
                    return ResponseEntity.status(201).body(user);
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
                
                private final UserRepository userRepository;
                private final EmailService emailService;
                private final UserMapper userMapper;
                
                public UserService(UserRepository userRepository, 
                                 EmailService emailService,
                                 UserMapper userMapper) {
                    this.userRepository = userRepository;
                    this.emailService = emailService;
                    this.userMapper = userMapper;
                }
                
                public UserDto createUser(CreateUserRequest request) {
                    validateRequest(request);
                    
                    User user = userMapper.fromRequest(request);
                    User savedUser = userRepository.save(user);
                    
                    emailService.sendWelcomeEmailAsync(savedUser.getEmail());
                    
                    return userMapper.toDto(savedUser);
                }
                
                @Transactional(readOnly = true)
                public Optional<UserDto> findById(Long id) {
                    return userRepository.findById(id)
                        .map(userMapper::toDto);
                }
                
                private void validateRequest(CreateUserRequest request) {
                    if (userRepository.existsByEmail(request.getEmail())) {
                        throw new UserAlreadyExistsException("Email already in use");
                    }
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, wellStructuredCode);

        // Should have minimal violations for well-structured code
        assertTrue(findings.stream().noneMatch(f -> 
            f.getSeverity() == ReviewFinding.Severity.CRITICAL));
    }

    @Test
    void shouldDetectMixedResponsibilitiesAcrossLayers() {
        String mixedResponsibilitiesCode = """
            @RestController
            public class MixedController {
                
                @PostMapping("/process")
                public void processData(@RequestBody DataRequest request) {
                    
                    // Presentation logic
                    if (request.getFormat().equals("json")) {
                        response.setContentType("application/json");
                    }
                    
                    // Business logic (should be in service)
                    for (DataItem item : request.getItems()) {
                        if (item.getValue() > 100) {
                            item.setCategory("HIGH");
                        } else {
                            item.setCategory("LOW");
                        }
                    }
                    
                    // Data access logic (should be in repository)
                    String sql = "INSERT INTO processed_data (value, category) VALUES (?, ?)";
                    for (DataItem item : request.getItems()) {
                        jdbcTemplate.update(sql, item.getValue(), item.getCategory());
                    }
                    
                    // Infrastructure logic (should be in separate service)
                    emailService.sendEmail("admin@company.com", "Data processed");
                    
                    // Reporting logic (should be in separate service)
                    generateProcessingReport(request.getItems());
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, mixedResponsibilitiesCode);

        // Should detect field injection and possibly method length issues
        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.KISS_VIOLATION ||
            f.getType() == ReviewFinding.FindingType.SOLID_PRINCIPLES));
    }
}
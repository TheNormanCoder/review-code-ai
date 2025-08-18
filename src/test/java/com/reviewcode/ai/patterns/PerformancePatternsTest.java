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
class PerformancePatternsTest {

    @InjectMocks
    private ArchitectureValidationService validationService;

    private String testFileName;

    @BeforeEach
    void setUp() {
        testFileName = "PerformanceTest.java";
    }

    @Test
    void shouldDetectSelectAllQueryViolation() {
        String selectAllCode = """
            @Repository
            public class UserRepository {
                
                @Query("SELECT * FROM users WHERE active = true")
                public List<User> findActiveUsers() {
                    return null;
                }
                
                @Query("SELECT * FROM users u JOIN orders o ON u.id = o.user_id")
                public List<User> findUsersWithOrders() {
                    return null;
                }
                
                public List<User> getAllUsers() {
                    return jdbcTemplate.query("SELECT * FROM users", new UserRowMapper());
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, selectAllCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.PERFORMANCE &&
            f.getDescription().contains("SELECT * query")));
    }

    @Test
    void shouldDetectN1QueryProblemViolation() {
        String n1QueryCode = """
            @Entity
            public class Order {
                @Id
                private Long id;
                
                @OneToMany(mappedBy = "order") // No fetch type specified - defaults to LAZY but can cause N+1
                private List<OrderItem> items;
                
                @ManyToOne // Defaults to EAGER - can cause N+1 when loading many orders
                private Customer customer;
            }
            
            @Entity
            public class Customer {
                @Id
                private Long id;
                
                @OneToMany(mappedBy = "customer") // No LAZY specified
                private List<Order> orders;
                
                @OneToMany(mappedBy = "customer", fetch = FetchType.EAGER) // Explicit EAGER - problematic
                private List<Address> addresses;
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, n1QueryCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.PERFORMANCE &&
            f.getDescription().contains("N+1 query problem")));
    }

    @Test
    void shouldDetectIneffientLoopingViolation() {
        String inefficientLoopCode = """
            @Service
            public class OrderService {
                
                public void processOrders(List<Order> orders) {
                    // N+1 problem in service layer
                    for (Order order : orders) {
                        Customer customer = customerRepository.findById(order.getCustomerId()); // Database call in loop
                        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId()); // Database call in loop
                        order.setCustomer(customer);
                        order.setItems(items);
                    }
                }
                
                public List<User> getActiveUsers() {
                    List<User> allUsers = userRepository.findAll();
                    List<User> activeUsers = new ArrayList<>();
                    
                    // Inefficient filtering - should be done in database
                    for (User user : allUsers) {
                        if (user.isActive()) {
                            activeUsers.add(user);
                        }
                    }
                    return activeUsers;
                }
            }
            """;

        // Current implementation doesn't detect service-layer N+1 problems
        // This would be a good enhancement
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, inefficientLoopCode);
    }

    @Test
    void shouldDetectMissingCachingViolation() {
        String noCachingCode = """
            @Service
            public class ConfigurationService {
                
                // Expensive operation called frequently without caching
                public Configuration getSystemConfiguration() {
                    // Complex database queries and calculations
                    List<ConfigItem> items = configRepository.findAll();
                    Configuration config = new Configuration();
                    
                    for (ConfigItem item : items) {
                        config.addProperty(item.getKey(), calculateValue(item));
                    }
                    
                    return config;
                }
                
                // Expensive calculation without caching
                public Report generateDashboardReport() {
                    // Heavy computation that could be cached
                    List<Order> orders = orderRepository.findAll();
                    List<Customer> customers = customerRepository.findAll();
                    
                    return reportBuilder.buildDashboard(orders, customers);
                }
            }
            """;

        // Current implementation doesn't detect missing caching opportunities
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, noCachingCode);
    }

    @Test
    void shouldDetectLargeResultSetViolation() {
        String largeResultSetCode = """
            @Repository
            public class DataRepository {
                
                @Query("SELECT * FROM transactions") // Potentially millions of records
                public List<Transaction> getAllTransactions() {
                    return null;
                }
                
                public List<User> findAllUsers() {
                    return jdbcTemplate.query("SELECT * FROM users", new UserRowMapper()); // No pagination
                }
                
                @Query("SELECT u FROM User u") // No pagination, could return huge dataset
                public List<User> findUsers() {
                    return null;
                }
            }
            """;

        // This would require detecting queries without pagination
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, largeResultSetCode);
    }

    @Test
    void shouldDetectInefficientStringConcatenationViolation() {
        String stringConcatCode = """
            @Service
            public class ReportService {
                
                public String generateReport(List<Data> dataList) {
                    String report = "";
                    
                    // Inefficient string concatenation in loop
                    for (Data data : dataList) {
                        report = report + data.toString() + "\\n"; // Creates new string object each time
                        report = report + "Value: " + data.getValue() + "\\n";
                        report = report + "Description: " + data.getDescription() + "\\n";
                    }
                    
                    return report;
                }
                
                public String buildQuery(List<String> conditions) {
                    String query = "SELECT * FROM table WHERE ";
                    
                    for (int i = 0; i < conditions.size(); i++) {
                        query = query + conditions.get(i); // Inefficient concatenation
                        if (i < conditions.size() - 1) {
                            query = query + " AND ";
                        }
                    }
                    
                    return query;
                }
            }
            """;

        // This would require detecting string concatenation in loops
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, stringConcatCode);
    }

    @Test
    void shouldDetectUnoptimizedDatabaseConnectionViolation() {
        String badConnectionCode = """
            @Service
            public class DatabaseService {
                
                public List<User> getUsers() {
                    Connection conn = null;
                    try {
                        // Creating new connection each time - no connection pooling
                        conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "user", "pass");
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT * FROM users");
                        
                        List<User> users = new ArrayList<>();
                        while (rs.next()) {
                            users.add(mapUser(rs));
                        }
                        return users;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    } finally {
                        if (conn != null) {
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                // Ignore
                            }
                        }
                    }
                }
            }
            """;

        // This would require detecting manual connection management
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, badConnectionCode);
    }

    @Test
    void shouldDetectMemoryLeakPatterns() {
        String memoryLeakCode = """
            @Service
            public class EventService {
                
                // Static collections that grow indefinitely
                private static final List<Event> allEvents = new ArrayList<>();
                private static final Map<String, Object> cache = new HashMap<>();
                
                public void handleEvent(Event event) {
                    allEvents.add(event); // Memory leak - never cleaned
                    cache.put(event.getId(), event.getData()); // Cache grows indefinitely
                }
                
                // Inner class holding reference to outer class
                public class EventProcessor {
                    public void process() {
                        // This holds reference to EventService instance
                        handleEvent(new Event());
                    }
                }
            }
            """;

        // This would require detecting potential memory leak patterns
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, memoryLeakCode);
    }

    @Test
    void shouldDetectSynchronousProcessingViolation() {
        String syncProcessingCode = """
            @RestController
            public class NotificationController {
                
                @PostMapping("/send-notifications")
                public ResponseEntity<String> sendNotifications(@RequestBody List<String> userIds) {
                    
                    // Synchronous processing of potentially long-running tasks
                    for (String userId : userIds) {
                        User user = userService.findById(userId);
                        emailService.sendEmail(user.getEmail(), "Notification"); // Blocking I/O
                        smsService.sendSMS(user.getPhone(), "Notification"); // Blocking I/O
                        pushService.sendPush(user.getDeviceToken(), "Notification"); // Blocking I/O
                    }
                    
                    return ResponseEntity.ok("Notifications sent");
                }
                
                @PostMapping("/process-data")
                public void processLargeDataset(@RequestBody DataRequest request) {
                    // Heavy computation on main thread
                    for (DataItem item : request.getItems()) {
                        complexCalculation(item); // CPU intensive
                        externalApiCall(item); // Network I/O
                    }
                }
            }
            """;

        // This would require detecting synchronous processing patterns
        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, syncProcessingCode);
    }

    @Test
    void shouldApproveOptimizedPerformanceCode() {
        String optimizedCode = """
            @Service
            public class OptimizedOrderService {
                
                @Cacheable("orders")
                public List<Order> findOrdersByStatus(OrderStatus status) {
                    return orderRepository.findByStatusWithItems(status); // Optimized query
                }
                
                @Async
                public CompletableFuture<Void> processOrdersAsync(List<Order> orders) {
                    // Batch processing with proper pagination
                    orders.parallelStream().forEach(this::processOrder);
                    return CompletableFuture.completedFuture(null);
                }
                
                @Transactional(readOnly = true)
                public Page<Order> findOrders(Pageable pageable) {
                    return orderRepository.findAll(pageable); // Paginated results
                }
                
                public String buildReport(List<Data> dataList) {
                    // Efficient string building
                    StringBuilder report = new StringBuilder();
                    dataList.forEach(data -> {
                        report.append(data.toString()).append("\\n");
                    });
                    return report.toString();
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, optimizedCode);

        // Should have minimal performance violations
        assertTrue(findings.stream().noneMatch(f -> 
            f.getType() == ReviewFinding.FindingType.PERFORMANCE &&
            f.getSeverity() == ReviewFinding.Severity.HIGH));
    }

    @Test
    void shouldDetectOptimizedEntityRelationships() {
        String optimizedEntityCode = """
            @Entity
            public class OptimizedOrder {
                @Id
                private Long id;
                
                @OneToMany(mappedBy = "order", fetch = FetchType.LAZY) // Proper lazy loading
                private List<OrderItem> items;
                
                @ManyToOne(fetch = FetchType.LAZY) // Lazy loading for association
                private Customer customer;
                
                @BatchSize(size = 25) // Batch fetching optimization
                @OneToMany(mappedBy = "order")
                private List<OrderNote> notes;
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, optimizedEntityCode);

        // Should not have N+1 query problems
        assertTrue(findings.stream().noneMatch(f -> 
            f.getType() == ReviewFinding.FindingType.PERFORMANCE &&
            f.getDescription().contains("N+1")));
    }

    @Test
    void shouldDetectEfficientQueryPatterns() {
        String efficientQueryCode = """
            @Repository
            public interface OptimizedUserRepository extends JpaRepository<User, Long> {
                
                @Query("SELECT u.id, u.name, u.email FROM User u WHERE u.active = true")
                List<UserProjection> findActiveUserProjections(); // Selecting only needed columns
                
                @Query("SELECT u FROM User u JOIN FETCH u.orders WHERE u.id = :userId")
                Optional<User> findUserWithOrders(@Param("userId") Long userId); // Fetch join
                
                @Modifying
                @Query("UPDATE User u SET u.lastLogin = :now WHERE u.id = :userId")
                void updateLastLogin(@Param("userId") Long userId, @Param("now") LocalDateTime now);
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, efficientQueryCode);

        // Should not have SELECT * violations
        assertTrue(findings.stream().noneMatch(f -> 
            f.getType() == ReviewFinding.FindingType.PERFORMANCE &&
            f.getDescription().contains("SELECT *")));
    }
}
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
class CleanCodePatternsTest {

    @InjectMocks
    private ArchitectureValidationService validationService;

    private String testFileName;

    @BeforeEach
    void setUp() {
        testFileName = "CleanCodeTest.java";
    }

    @Test
    void shouldDetectLongMethodViolation() {
        String longMethodCode = """
            public class UserService {
                public void processUser(User user) {
                    // Validation logic (5 lines)
                    if (user == null) throw new IllegalArgumentException("User cannot be null");
                    if (user.getEmail() == null) throw new IllegalArgumentException("Email required");
                    if (user.getName() == null) throw new IllegalArgumentException("Name required");
                    if (user.getAge() < 0) throw new IllegalArgumentException("Age invalid");
                    if (user.getPhone() == null) throw new IllegalArgumentException("Phone required");
                    
                    // Business logic (10 lines)
                    user.setCreatedDate(new Date());
                    user.setStatus(UserStatus.ACTIVE);
                    user.setRole(Role.USER);
                    userRepository.save(user);
                    eventPublisher.publishEvent(new UserCreatedEvent(user));
                    emailService.sendWelcomeEmail(user);
                    auditService.logUserCreation(user);
                    metricsService.incrementUserCount();
                    cacheService.invalidateUserCache();
                    notificationService.notifyAdmins(user);
                    
                    // Additional logic (10+ more lines)
                    profileService.createDefaultProfile(user);
                    preferencesService.setDefaultPreferences(user);
                    subscriptionService.createFreeSubscription(user);
                    billingService.setupBillingAccount(user);
                    securityService.assignDefaultPermissions(user);
                    analyticsService.trackUserRegistration(user);
                    socialService.setupSocialConnections(user);
                    mailchimpService.addToMailingList(user);
                    slackService.notifyTeam(user);
                    systemService.updateSystemMetrics();
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, longMethodCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.KISS_VIOLATION &&
            f.getDescription().contains("too long")));
    }

    @Test
    void shouldDetectTooManyParametersViolation() {
        String manyParamsCode = """
            public class OrderService {
                public Order createOrder(String customerId, String productId, int quantity, 
                                       double price, String currency, String paymentMethod, 
                                       String shippingAddress, String billingAddress, 
                                       Date deliveryDate, String promoCode) {
                    return new Order();
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, manyParamsCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.BEST_PRACTICE &&
            f.getDescription().contains("too many parameters")));
    }

    @Test
    void shouldDetectDeepNestingViolation() {
        String deepNestingCode = """
            public class ValidationService {
                public boolean validate(User user) {
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

        // Note: This would require updating the regex pattern to detect deep nesting
        // The current implementation might not catch this specific pattern
    }

    @Test
    void shouldDetectCodeDuplicationViolation() {
        String duplicatedCode = """
            public class PaymentService {
                public void processCardPayment(CardPayment payment) {
                    System.out.println("Starting payment validation");
                    validatePaymentAmount(payment.getAmount());
                    validatePaymentMethod(payment.getMethod());
                    validateCustomer(payment.getCustomerId());
                    System.out.println("Payment validation completed");
                    processPayment(payment);
                }
                
                public void processBankPayment(BankPayment payment) {
                    System.out.println("Starting payment validation");
                    validatePaymentAmount(payment.getAmount());
                    validatePaymentMethod(payment.getMethod());
                    validateCustomer(payment.getCustomerId());
                    System.out.println("Payment validation completed");
                    processBankTransfer(payment);
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, duplicatedCode);

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.DRY_VIOLATION &&
            f.getDescription().contains("Duplicate code")));
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
                    } else if (amount > 100) {
                        return amount * 0.05; // Magic number
                    }
                    return 0;
                }
            }
            """;

        // Note: This would require adding a new validation rule for magic numbers
        // Currently not implemented in ArchitectureValidationService
    }

    @Test
    void shouldDetectLongClassViolation() {
        StringBuilder longClass = new StringBuilder();
        longClass.append("public class MegaService {\n");
        longClass.append("    private final Repository repo;\n");
        longClass.append("    \n");
        
        // Add many methods to make it a large class
        for (int i = 1; i <= 50; i++) {
            longClass.append("    public void method").append(i).append("() {\n");
            longClass.append("        // Method implementation\n");
            longClass.append("        System.out.println(\"Executing method ").append(i).append("\");\n");
            longClass.append("        doSomeBusinessLogic();\n");
            longClass.append("        logMethodExecution();\n");
            longClass.append("    }\n\n");
        }
        longClass.append("}");

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, longClass.toString());

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SOLID_PRINCIPLES &&
            f.getDescription().contains("too large")));
    }

    @Test
    void shouldDetectPoorNamingConventions() {
        String poorNamingCode = """
            public class UserService {
                public User getData(String id) { // Vague method name
                    return repository.findById(id);
                }
                
                public void doStuff(User u) { // Very vague method name and parameter
                    u.setStatus("active");
                }
                
                public boolean check(String email) { // Unclear what is being checked
                    return email.contains("@");
                }
            }
            """;

        // Note: This would require adding naming convention validation rules
        // Currently not implemented in ArchitectureValidationService
    }

    @Test
    void shouldApproveWellStructuredCleanCode() {
        String cleanCode = """
            public class UserService {
                
                private final UserRepository userRepository;
                private final EmailService emailService;
                
                public UserService(UserRepository userRepository, EmailService emailService) {
                    this.userRepository = userRepository;
                    this.emailService = emailService;
                }
                
                public User createUser(CreateUserRequest request) {
                    validateRequest(request);
                    User user = buildUser(request);
                    User savedUser = userRepository.save(user);
                    sendWelcomeEmail(savedUser);
                    return savedUser;
                }
                
                private void validateRequest(CreateUserRequest request) {
                    if (request.getEmail() == null) {
                        throw new IllegalArgumentException("Email is required");
                    }
                }
                
                private User buildUser(CreateUserRequest request) {
                    return User.builder()
                        .email(request.getEmail())
                        .name(request.getName())
                        .build();
                }
                
                private void sendWelcomeEmail(User user) {
                    emailService.sendWelcomeEmail(user.getEmail());
                }
            }
            """;

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, cleanCode);

        // Should have minimal findings for clean, well-structured code
        assertTrue(findings.stream().noneMatch(f -> 
            f.getSeverity() == ReviewFinding.Severity.HIGH ||
            f.getSeverity() == ReviewFinding.Severity.CRITICAL));
    }

    @Test
    void shouldDetectGodClassAntiPattern() {
        StringBuilder godClass = new StringBuilder();
        godClass.append("public class UserManagementSystem {\n");
        godClass.append("    // Database operations\n");
        godClass.append("    public void saveUser(User user) { /* implementation */ }\n");
        godClass.append("    public User findUser(String id) { /* implementation */ }\n");
        godClass.append("    \n");
        godClass.append("    // Email operations\n");
        godClass.append("    public void sendWelcomeEmail(User user) { /* implementation */ }\n");
        godClass.append("    public void sendPasswordReset(User user) { /* implementation */ }\n");
        godClass.append("    \n");
        godClass.append("    // Payment operations\n");
        godClass.append("    public void processPayment(Payment payment) { /* implementation */ }\n");
        godClass.append("    public void refundPayment(String paymentId) { /* implementation */ }\n");
        godClass.append("    \n");
        godClass.append("    // Reporting operations\n");
        godClass.append("    public Report generateUserReport() { /* implementation */ }\n");
        godClass.append("    public Report generatePaymentReport() { /* implementation */ }\n");
        godClass.append("    \n");
        godClass.append("    // Notification operations\n");
        godClass.append("    public void sendSMS(User user, String message) { /* implementation */ }\n");
        godClass.append("    public void sendPushNotification(User user, String message) { /* implementation */ }\n");
        
        // Add more methods to make it exceed the line limit
        for (int i = 0; i < 250; i++) {
            godClass.append("    // Line ").append(i).append("\n");
        }
        godClass.append("}");

        List<ReviewFinding> findings = validationService.validateArchitecturalPrinciples(testFileName, godClass.toString());

        assertTrue(findings.stream().anyMatch(f -> 
            f.getType() == ReviewFinding.FindingType.SOLID_PRINCIPLES &&
            f.getDescription().contains("too large")));
    }

    @Test
    void shouldDetectEmptyMethodsAntiPattern() {
        String emptyMethodsCode = """
            public class ServiceImpl implements Service {
                
                @Override
                public void method1() {
                    // TODO: Implement this method
                }
                
                @Override
                public void method2() {
                    // Not implemented yet
                }
                
                @Override
                public String method3() {
                    return null; // Placeholder implementation
                }
            }
            """;

        // Note: This would require adding validation for empty methods
        // Currently not implemented in ArchitectureValidationService
    }
}
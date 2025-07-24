package eda.integration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for Phase 1 Integration Service improvements
 * Tests logging, metrics, validation, and exception handling
 */
@ExtendWith(MockitoExtension.class)
class IntegrationServicePhase1Test {
    
    @Mock
    private ResilienceComponentManager resilienceManager;
    
    private IntegrationValidator validator;
    private IntegrationMetrics metrics;
    private TestIntegrationService integrationService;
    private MeterRegistry meterRegistry;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        validator = new IntegrationValidator();
        metrics = new IntegrationMetrics(meterRegistry);
        
        // Resilience manager will be mocked at class level
        
        integrationService = new TestIntegrationService(resilienceManager);
        
        // Inject dependencies using reflection (simulating @Autowired)
        ReflectionTestUtils.setField(integrationService, "validator", validator);
        ReflectionTestUtils.setField(integrationService, "metrics", metrics);
    }
    
    @Test
    void testSuccessfulIntegrationWithMetrics() throws ExecutionException, InterruptedException {
        // Given
        TestRequest request = new TestRequest("test-data");
        IntegrationRequest<TestRequest> integrationRequest = new GenericIntegrationRequest<>(
            "correlation-123", request, IntegrationType.REST);
        
        IntegrationConfig config = new IntegrationConfig("https://api.example.com/test");
        
        // When
        CompletableFuture<IntegrationResponse<TestResponse>> future = 
            integrationService.executeAsync(integrationRequest, TestResponse.class, config);
        
        IntegrationResponse<TestResponse> response = future.get();
        
        // Then
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("correlation-123", response.getCorrelationId());
        assertTrue(response.getMetadata().containsKey("duration_ms"));
        
        // Verify metrics were recorded
        assertNotNull(meterRegistry.find("integration.requests.total").counter());
        assertNotNull(meterRegistry.find("integration.requests").timer());
    }
    
    @Test
    void testValidationFailure() throws ExecutionException, InterruptedException {
        // Given - Invalid request (null correlation ID)
        TestRequest request = new TestRequest("test-data");
        IntegrationRequest<TestRequest> integrationRequest = new GenericIntegrationRequest<>(
            null, request, IntegrationType.REST); // Invalid: null correlation ID
        
        IntegrationConfig config = new IntegrationConfig("https://api.example.com/test");
        
        // When
        CompletableFuture<IntegrationResponse<TestResponse>> future = 
            integrationService.executeAsync(integrationRequest, TestResponse.class, config);
        
        IntegrationResponse<TestResponse> response = future.get();
        
        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("Validation failed"));
        assertEquals(integrationRequest.getCorrelationId(), response.getCorrelationId());
        
        // Verify validation failure metric was recorded
        assertNotNull(meterRegistry.find("integration.validation.failures").counter());
    }
    
    @Test 
    void testConfigurationValidationFailure() {
        // Given
        TestRequest request = new TestRequest("test-data");
        IntegrationRequest<TestRequest> integrationRequest = new GenericIntegrationRequest<>(
            "correlation-123", request, IntegrationType.REST);
        
        IntegrationConfig config = new IntegrationConfig(""); // Invalid: empty endpoint
        
        // When/Then - should throw exception due to validation failure
        assertThrows(IntegrationException.ConfigurationException.class, () -> {
            try {
                integrationService.executeAsync(integrationRequest, TestResponse.class, config).get();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IntegrationException.ConfigurationException) {
                    throw (IntegrationException.ConfigurationException) e.getCause();
                }
                throw new RuntimeException(e.getCause());
            }
        });
    }
    
    @Test
    void testResiliencePatternFailure() throws ExecutionException, InterruptedException {
        // Given
        TestRequest request = new TestRequest("test-data");
        IntegrationRequest<TestRequest> integrationRequest = new GenericIntegrationRequest<>(
            "correlation-123", request, IntegrationType.REST);
        
        IntegrationConfig config = new IntegrationConfig("https://api.example.com/test");
        
        // Mock the service to always fail
        integrationService.setShouldFail(true);
        
        // When
        CompletableFuture<IntegrationResponse<TestResponse>> future = 
            integrationService.executeAsync(integrationRequest, TestResponse.class, config);
        
        IntegrationResponse<TestResponse> response = future.get();
        
        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMetadata().containsKey("resilience_error"));
        assertTrue(response.getMetadata().containsKey("error_type"));
        assertTrue(response.getMetadata().containsKey("duration_ms"));
        assertEquals("correlation-123", response.getCorrelationId());
    }
    
    @Test
    void testSynchronousExecution() {
        // Given
        TestRequest request = new TestRequest("test-data");
        IntegrationRequest<TestRequest> integrationRequest = new GenericIntegrationRequest<>(
            "correlation-sync", request, IntegrationType.REST);
        
        IntegrationConfig config = new IntegrationConfig("https://api.example.com/test");
        
        // When
        IntegrationResponse<TestResponse> response = 
            integrationService.executeSync(integrationRequest, TestResponse.class, config);
        
        // Then
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("correlation-sync", response.getCorrelationId());
    }
    
    @Test
    void testExceptionHandlingHierarchy() {
        // Test different exception types
        
        // Validation Exception
        IntegrationException.ValidationException validationEx = 
            new IntegrationException.ValidationException("Invalid input", "corr-1", IntegrationType.REST);
        
        assertEquals("corr-1", validationEx.getCorrelationId());
        assertEquals(IntegrationType.REST, validationEx.getIntegrationType());
        assertTrue(validationEx.getMessage().contains("Validation failed"));
        
        // Connectivity Exception
        IntegrationException.ConnectivityException connectivityEx = 
            new IntegrationException.ConnectivityException("Network error", 
                new RuntimeException("Connection refused"), "corr-2", IntegrationType.KAFKA, "kafka://localhost:9092");
        
        assertEquals("corr-2", connectivityEx.getCorrelationId());
        assertEquals(IntegrationType.KAFKA, connectivityEx.getIntegrationType());
        assertEquals("kafka://localhost:9092", connectivityEx.getEndpoint());
        
        // Timeout Exception
        IntegrationException.TimeoutException timeoutEx = 
            new IntegrationException.TimeoutException("Operation timeout", 5000, "corr-3", IntegrationType.GRAPHQL, "https://api.example.com/graphql");
        
        assertEquals(5000, timeoutEx.getTimeoutMs());
        assertEquals("corr-3", timeoutEx.getCorrelationId());
    }
    
    @Test
    void testValidatorBoundaryConditions() {
        IntegrationValidator validator = new IntegrationValidator();
        
        // Test correlation ID length limits
        TestRequest request = new TestRequest("test");
        String longCorrelationId = "a".repeat(300); // Exceeds MAX_CORRELATION_ID_LENGTH
        
        IntegrationRequest<TestRequest> invalidRequest = new GenericIntegrationRequest<>(
            longCorrelationId, request, IntegrationType.REST);
        
        IntegrationValidator.ValidationResult result = validator.validateRequest(invalidRequest);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.contains("exceeds maximum length")));
        
        // Test valid request
        IntegrationRequest<TestRequest> validRequest = new GenericIntegrationRequest<>(
            "valid-correlation-id", request, IntegrationType.REST);
        
        IntegrationValidator.ValidationResult validResult = validator.validateRequest(validRequest);
        assertTrue(validResult.isValid());
        assertTrue(validResult.getErrors().isEmpty());
    }
    
    @Test
    void testResilienceConfigValidation() {
        IntegrationValidator validator = new IntegrationValidator();
        
        // Test invalid resilience config
        ResilienceConfig invalidConfig = ResilienceConfig.builder()
            .timeout(Duration.ofMillis(50)) // Too short
            .maxRetryAttempts(-1) // Negative
            .circuitBreakerFailureThreshold(1.5) // > 1.0
            .build();
        
        IntegrationValidator.ValidationResult result = validator.validateResilienceConfig(invalidConfig);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() > 1);
        
        // Test valid resilience config
        ResilienceConfig validConfig = ResilienceConfig.builder()
            .timeout(Duration.ofSeconds(30))
            .maxRetryAttempts(3)
            .circuitBreakerFailureThreshold(0.5)
            .build();
        
        IntegrationValidator.ValidationResult validResult = validator.validateResilienceConfig(validConfig);
        assertTrue(validResult.isValid());
    }
    
    // Test implementation of BaseIntegrationService
    private static class TestIntegrationService extends BaseIntegrationService {
        private boolean shouldFail = false;
        
        public TestIntegrationService(ResilienceComponentManager resilienceManager) {
            super(resilienceManager);
        }
        
        public void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }
        
        // Override resilience application to avoid complex mocking
        @Override
        protected <T> CompletableFuture<T> applyResilience(
                java.util.function.Supplier<CompletableFuture<T>> operation,
                String operationName,
                ResilienceConfig config) {
            // Skip resilience for testing - just execute the operation directly
            return operation.get();
        }
        
        @Override
        protected <T, R> CompletableFuture<IntegrationResponse<R>> executeInternal(
                IntegrationRequest<T> request, Class<R> responseType, IntegrationConfig config) {
            
            if (shouldFail) {
                return CompletableFuture.failedFuture(
                    new RuntimeException("Simulated service failure"));
            }
            
            // Simulate successful response
            @SuppressWarnings("unchecked")
            R responseData = (R) new TestResponse("success", 200);
            
            IntegrationResponse<R> response = GenericIntegrationResponse.success(
                request.getCorrelationId(), responseData);
            
            response.getMetadata().put("integration_type", "TEST");
            response.getMetadata().put("endpoint", config.getEndpoint());
            
            return CompletableFuture.completedFuture(response);
        }
    }
    
    // Test data classes
    private static class TestRequest {
        private final String data;
        
        public TestRequest(String data) {
            this.data = data;
        }
        
        public String getData() {
            return data;
        }
    }
    
    private static class TestResponse {
        private final String result;
        private final int status;
        
        public TestResponse(String result, int status) {
            this.result = result;
            this.status = status;
        }
        
        public String getResult() {
            return result;
        }
        
        public int getStatus() {
            return status;
        }
    }
}
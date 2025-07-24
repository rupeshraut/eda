# Integration Framework Comprehensive Examples

This document provides comprehensive examples and usage patterns for the Integration Framework that was enhanced in Phase 1 with production-ready features.

## Overview

The Integration Framework now includes:
- **Enhanced Logging** with SLF4J and structured output
- **Comprehensive Metrics** collection with Micrometer
- **Input Validation** with detailed error reporting  
- **Structured Exception Handling** hierarchy
- **Resilience Patterns** (Circuit Breaker, Retry, Timeout)
- **Production-Ready Observability** and monitoring

## Core Components

### 1. IntegrationValidator

Provides comprehensive validation for requests, configurations, and resilience settings.

```java
// Initialize validator
IntegrationValidator validator = new IntegrationValidator();

// Validate request
IntegrationRequest<UserData> request = new GenericIntegrationRequest<>(
    "correlation-123", userData, IntegrationType.REST);

IntegrationValidator.ValidationResult result = validator.validateRequest(request);
if (!result.isValid()) {
    log.error("Validation failed: {}", result.getErrors());
}

// Validate configuration
IntegrationConfig config = new IntegrationConfig("https://api.example.com/users");
IntegrationValidator.ValidationResult configResult = validator.validateConfig(config);

// Validate resilience configuration
ResilienceConfig resilienceConfig = ResilienceConfig.builder()
    .timeout(Duration.ofSeconds(30))
    .maxRetryAttempts(3)
    .build();
    
IntegrationValidator.ValidationResult resilienceResult = 
    validator.validateResilienceConfig(resilienceConfig);
```

### 2. IntegrationMetrics

Collects comprehensive metrics for observability and monitoring.

```java
// Initialize metrics
MeterRegistry meterRegistry = new SimpleMeterRegistry();
IntegrationMetrics metrics = new IntegrationMetrics(meterRegistry);

// Record successful operation
metrics.recordSuccess(IntegrationType.REST, "https://api.example.com", Duration.ofMillis(150));

// Record failed operation
metrics.recordFailure(IntegrationType.REST, "https://api.example.com", 
    Duration.ofMillis(5000), "TimeoutException");

// Record validation failure
metrics.recordValidationFailure(IntegrationType.REST, "Invalid correlation ID");

// Record circuit breaker state change
metrics.recordCircuitBreakerStateChange("user-service", "CLOSED", "OPEN");

// Record retry attempt
metrics.recordRetryAttempt("user-service", 2);

// Record timeout event
metrics.recordTimeout("slow-service", Duration.ofSeconds(30));
```

### 3. Exception Hierarchy

Structured exceptions for different failure scenarios.

```java
// ValidationException - for input validation failures
try {
    throw new IntegrationException.ValidationException(
        "Required field missing", "correlation-123", IntegrationType.REST);
} catch (IntegrationException.ValidationException e) {
    log.error("Validation error: {}", e.getMessage());
    // Results in 400 Bad Request typically
}

// ConnectivityException - for network/connectivity issues
try {
    throw new IntegrationException.ConnectivityException(
        "Connection refused", new RuntimeException("Network unreachable"),
        "correlation-123", IntegrationType.KAFKA, "kafka://localhost:9092");
} catch (IntegrationException.ConnectivityException e) {
    log.error("Connectivity error at {}: {}", e.getEndpoint(), e.getMessage());
}

// TimeoutException - for operation timeouts
try {
    throw new IntegrationException.TimeoutException(
        "Operation timed out", 30000, "correlation-123", 
        IntegrationType.GRAPHQL, "https://api.example.com/graphql");
} catch (IntegrationException.TimeoutException e) {
    log.error("Timeout after {}ms: {}", e.getTimeoutMs(), e.getMessage());
}

// BusinessException - for business logic failures
try {
    throw new IntegrationException.BusinessException(
        "Insufficient funds", 402, "{\"error\":\"INSUFFICIENT_FUNDS\"}", 
        "correlation-123", IntegrationType.REST, "https://api.payment.com");
} catch (IntegrationException.BusinessException e) {
    log.error("Business error (HTTP {}): {}", e.getStatusCode(), e.getMessage());
    log.debug("Response body: {}", e.getResponseBody());
}

// ConfigurationException - for setup/configuration issues
try {
    throw new IntegrationException.ConfigurationException(
        "Invalid endpoint URL", "correlation-123", 
        IntegrationType.REST, "invalid-url");
} catch (IntegrationException.ConfigurationException e) {
    log.error("Configuration error for {}: {}", e.getEndpoint(), e.getMessage());
}
```

## Resilience Configuration Patterns

### Basic Configuration
```java
ResilienceConfig basicConfig = ResilienceConfig.builder()
    .timeout(Duration.ofSeconds(10))
    .maxRetryAttempts(3)
    .retryDelay(Duration.ofSeconds(1))
    .build();
```

### Critical Operations Configuration
```java
ResilienceConfig criticalConfig = ResilienceConfig.builder()
    .enableCircuitBreaker(true)
    .circuitBreakerFailureThreshold(0.2) // 20% failure rate
    .circuitBreakerMinCalls(5)
    .circuitBreakerWaitDuration(Duration.ofSeconds(30))
    .enableRetry(true)
    .maxRetryAttempts(5)
    .retryDelay(Duration.ofSeconds(2))
    .enableTimeout(true)
    .timeout(Duration.ofSeconds(15))
    .build();
```

### High-Frequency, Low-Latency Configuration
```java
ResilienceConfig highFreqConfig = ResilienceConfig.builder()
    .enableCircuitBreaker(true)
    .circuitBreakerFailureThreshold(0.1) // Very sensitive
    .circuitBreakerMinCalls(10)
    .circuitBreakerWaitDuration(Duration.ofSeconds(5))
    .enableRetry(true)
    .maxRetryAttempts(2) // Fail fast
    .retryDelay(Duration.ofMillis(100))
    .enableTimeout(true)
    .timeout(Duration.ofSeconds(2))
    .build();
```

### Batch Processing Configuration
```java
ResilienceConfig batchConfig = ResilienceConfig.builder()
    .enableCircuitBreaker(false) // Not suitable for batch
    .enableRetry(true)
    .maxRetryAttempts(10) // Many retries acceptable
    .retryDelay(Duration.ofSeconds(30))
    .enableTimeout(true)
    .timeout(Duration.ofMinutes(5)) // Long timeout
    .build();
```

## Integration Type Configurations

### REST Configuration
```java
IntegrationConfig restConfig = new IntegrationConfig(
    "https://api.example.com/v2/users",
    Map.of(
        "timeout", "5000",
        "cache.enabled", "true",
        "cache.ttl", "300",
        "api.version", "v2",
        "content.type", "application/json",
        "accept", "application/json"
    )
);
```

### Kafka Configuration
```java
IntegrationConfig kafkaConfig = new IntegrationConfig(
    "kafka://localhost:9092/user-events",
    Map.of(
        "bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094",
        "key.serializer", "org.apache.kafka.common.serialization.StringSerializer",
        "value.serializer", "org.apache.kafka.common.serialization.JsonSerializer",
        "acks", "all",
        "retries", "3",
        "batch.size", "16384",
        "linger.ms", "10",
        "buffer.memory", "33554432"
    )
);
```

### JMS Configuration
```java
IntegrationConfig jmsConfig = new IntegrationConfig(
    "queue://orders.processing",
    Map.of(
        "connection.factory.jndi", "jms/ConnectionFactory",
        "destination.type", "queue",
        "session.transacted", "true",
        "session.acknowledge.mode", "AUTO_ACKNOWLEDGE",
        "delivery.mode", "PERSISTENT",
        "priority", "4",
        "time.to.live", "0"
    )
);
```

### GraphQL Configuration
```java
IntegrationConfig graphqlConfig = new IntegrationConfig(
    "https://api.example.com/graphql",
    Map.of(
        "timeout", "10000",
        "max.query.depth", "15",
        "max.query.complexity", "1000",
        "enable.introspection", "false",
        "enable.tracing", "true",
        "schema.validation", "strict",
        "persisted.queries", "enabled"
    )
);
```

## Request and Response Patterns

### Creating Requests
```java
// Create request data
UserProfileData userData = new UserProfileData("user-12345", "premium");

// Create integration request with headers
IntegrationRequest<UserProfileData> request = new GenericIntegrationRequest<>(
    "correlation-" + UUID.randomUUID().toString(),
    userData,
    IntegrationType.REST
).withHeader("Authorization", "Bearer token123")
 .withHeader("X-Request-Source", "integration-service")
 .withHeader("X-Trace-Id", UUID.randomUUID().toString());
```

### Creating Responses
```java
// Successful response
UserProfileResult result = new UserProfileResult("user-12345", "John Doe", "john@example.com");
IntegrationResponse<UserProfileResult> successResponse = 
    GenericIntegrationResponse.success(request.getCorrelationId(), result);

// Add metadata
successResponse.getMetadata().put("duration_ms", 150L);
successResponse.getMetadata().put("integration_type", "REST");
successResponse.getMetadata().put("endpoint", "https://api.example.com/users");
successResponse.getMetadata().put("retry_attempts", 0);
successResponse.getMetadata().put("circuit_breaker_state", "CLOSED");

// Error response
IntegrationResponse<UserProfileResult> errorResponse = 
    GenericIntegrationResponse.error(request.getCorrelationId(), "User not found");

errorResponse.getMetadata().put("error_code", "USER_NOT_FOUND");
errorResponse.getMetadata().put("http_status", 404);
```

## Using BaseIntegrationService

Extend `BaseIntegrationService` to create your integration services:

```java
@Service
public class UserIntegrationService extends BaseIntegrationService {
    
    public UserIntegrationService(ResilienceComponentManager resilienceManager) {
        super(resilienceManager);
    }

    @Override
    protected <T, R> CompletableFuture<IntegrationResponse<R>> executeInternal(
            IntegrationRequest<T> request, Class<R> responseType, IntegrationConfig config) {
        
        // Your integration logic here
        // - Validation is handled automatically
        // - Metrics are recorded automatically  
        // - Resilience patterns are applied automatically
        // - Logging is structured automatically
        
        return CompletableFuture.completedFuture(
            GenericIntegrationResponse.success(request.getCorrelationId(), responseData)
        );
    }
}
```

### Usage Example
```java
@Autowired
private UserIntegrationService userService;

public void processUserRequest() {
    // Create request
    UserProfileRequest userRequest = new UserProfileRequest("user-123");
    IntegrationRequest<UserProfileRequest> request = new GenericIntegrationRequest<>(
        UUID.randomUUID().toString(), userRequest, IntegrationType.REST);
    
    // Create configuration
    IntegrationConfig config = new IntegrationConfig("https://api.users.com/profile");
    
    // Execute asynchronously
    CompletableFuture<IntegrationResponse<UserProfileResponse>> future = 
        userService.executeAsync(request, UserProfileResponse.class, config);
    
    future.thenAccept(response -> {
        if (response.isSuccess()) {
            log.info("User profile retrieved: {}", response.getData());
            log.info("Request completed in {}ms", response.getMetadata().get("duration_ms"));
        } else {
            log.error("User profile request failed: {}", response.getErrorMessage());
        }
    });
    
    // Or execute synchronously
    IntegrationResponse<UserProfileResponse> syncResponse = 
        userService.executeSync(request, UserProfileResponse.class, config);
}
```

## Production Best Practices

### Logging Best Practices
- Use structured logging with correlation IDs
- Log request/response metadata for observability
- Include timing information for performance analysis
- Log error context for debugging
- Use appropriate log levels (DEBUG, INFO, WARN, ERROR)

### Metrics Best Practices  
- Track success/failure rates for SLA monitoring
- Monitor response times and set alerts
- Track circuit breaker states for system health
- Monitor retry patterns to identify issues
- Use tags for dimensional analysis

### Validation Best Practices
- Validate all inputs before processing
- Provide detailed error messages for debugging
- Validate configuration at startup
- Check boundary conditions and edge cases
- Fail fast on invalid input

### Exception Handling Best Practices
- Use specific exception types for different scenarios
- Include correlation IDs in all exceptions
- Preserve original error context and stack traces
- Don't swallow exceptions silently
- Convert technical errors to business-friendly messages

### Resilience Best Practices
- Configure timeouts based on SLA requirements
- Use circuit breakers for external dependencies
- Implement exponential backoff for retries
- Isolate failures to prevent cascade effects
- Monitor resilience patterns effectiveness

### Configuration Best Practices
- Use environment-specific configurations
- Validate configurations at application startup
- Support configuration hot-reloading where possible
- Document all configuration parameters
- Use secure storage for sensitive configuration

## Monitoring and Observability

The framework automatically collects the following metrics:

### Request Metrics
- `integration.requests.total` - Total number of integration requests by type, endpoint, and status
- `integration.requests` - Timer for integration request duration by type, endpoint, and status

### Error Metrics
- `integration.validation.failures` - Count of validation failures by type and error
- `integration.requests.total` with status=error - Failed requests by error type

### Resilience Metrics
- `integration.circuit_breaker.state_changes` - Circuit breaker state transitions
- `integration.retry.attempts` - Retry attempts by service and attempt number
- `integration.timeouts` - Timeout events by service

### Example Metrics Query (Prometheus)
```promql
# Success rate by integration type
rate(integration_requests_total{status="success"}[5m]) / 
rate(integration_requests_total[5m])

# Average response time by endpoint
rate(integration_requests_sum[5m]) / 
rate(integration_requests_count[5m])

# Circuit breaker state changes
increase(integration_circuit_breaker_state_changes_total[1h])
```

## Testing

The framework includes comprehensive test coverage:

- `IntegrationServicePhase1Test` - Tests all Phase 1 improvements
- Validation testing with boundary conditions
- Exception handling testing
- Metrics collection verification
- Resilience pattern testing

Run tests:
```bash
./gradlew test
```

## Summary

The Integration Framework Phase 1 improvements provide:

✅ **Production-Ready Logging** - Structured, correlation-tracked logging  
✅ **Comprehensive Metrics** - Full observability with Micrometer  
✅ **Input Validation** - Detailed validation with clear error messages  
✅ **Exception Hierarchy** - Structured error handling for all scenarios  
✅ **Resilience Patterns** - Circuit breaker, retry, and timeout support  
✅ **Configuration Management** - Type-safe, validated configurations  
✅ **Multi-Integration Support** - REST, GraphQL, JMS, and Kafka ready  
✅ **Test Coverage** - Comprehensive test suite included  

The framework is now production-ready and provides all the observability, reliability, and maintainability features needed for enterprise integration scenarios.
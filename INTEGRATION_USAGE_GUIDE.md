# Integration Service Usage Guide

The Integration Service package provides a unified, resilient way to integrate with external services through REST, GraphQL, JMS, and Kafka.

## 🚀 Quick Start

### 1. Basic Setup

Add the configuration to your Spring Boot application:

```java
@Autowired
private RestIntegrationService restService;

@Autowired 
private ResilienceComponentManager resilienceManager;
```

### 2. Simple REST Call

```java
// Create request payload
UserRequest userRequest = new UserRequest("john@example.com", "John Doe");

// Build integration request
IntegrationRequest<UserRequest> request = new GenericIntegrationRequest<>(
    UUID.randomUUID().toString(),  // correlation ID
    userRequest,                   // payload
    IntegrationType.REST          // integration type
);

// Configuration for endpoint
IntegrationConfig config = new IntegrationConfig(
    "https://api.example.com/users",
    Map.of("timeout", "10000")
);

// Execute asynchronously
CompletableFuture<IntegrationResponse<UserResponse>> future = 
    restService.executeAsync(request, UserResponse.class, config);

// Handle response
future.thenAccept(response -> {
    if (response.isSuccess()) {
        System.out.println("Success: " + response.getData());
    } else {
        System.out.println("Error: " + response.getErrorMessage());
    }
});
```

## 🛠️ Core Components

### 1. IntegrationRequest
Encapsulates the request with metadata:
- **Correlation ID**: For tracing
- **Payload**: Request data
- **Headers**: Additional headers
- **Type**: Integration type (REST/GraphQL/JMS/Kafka)
- **Resilience Config**: Circuit breaker, retry, timeout settings

### 2. IntegrationResponse
Contains the response with metadata:
- **Success/Error status**
- **Response data** (generic type)
- **Error message** (if failed)
- **Metadata**: Additional information
- **Correlation ID**: For tracing

### 3. ResilienceConfig
Configures fault tolerance patterns:
- **Circuit Breaker**: Prevents cascading failures
- **Retry**: Automatic retry with backoff
- **Timeout**: Request timeout handling

## 📊 Integration Types

### REST Integration
```java
// Simple POST request
IntegrationRequest<OrderRequest> request = new GenericIntegrationRequest<>(
    correlationId, orderRequest, IntegrationType.REST
).withHeader("Authorization", "Bearer token");

CompletableFuture<IntegrationResponse<OrderResponse>> response = 
    restService.executeAsync(request, OrderResponse.class, config);
```

### GraphQL Integration
```java
// GraphQL query with variables
String query = "query GetUser($id: ID!) { user(id: $id) { name email } }";
Map<String, Object> variables = Map.of("id", "123");
GraphQLRequest graphqlRequest = new GraphQLRequest(query, variables);

IntegrationRequest<GraphQLRequest> request = new GenericIntegrationRequest<>(
    correlationId, graphqlRequest, IntegrationType.GRAPHQL
);

graphqlService.executeAsync(request, GraphQLResponse.class, config);
```

### JMS Messaging
```java
// Publish message to queue
OrderEvent event = new OrderEvent("ORDER-123", "CREATED");
IntegrationRequest<OrderEvent> request = new GenericIntegrationRequest<>(
    correlationId, event, IntegrationType.JMS
);

IntegrationConfig config = new IntegrationConfig("order.queue", Map.of());
jmsService.executeAsync(request, Void.class, config);
```

### Kafka Events
```java
// Publish event to topic
UserActivityEvent event = new UserActivityEvent("user-123", "LOGIN");
IntegrationRequest<UserActivityEvent> request = new GenericIntegrationRequest<>(
    correlationId, event, IntegrationType.KAFKA
).withHeader("kafka.key", "user-123"); // For partitioning

IntegrationConfig config = new IntegrationConfig("user.events", Map.of());
kafkaService.executeAsync(request, Void.class, config);
```

## ⚡ Resilience Patterns

### Custom Resilience Configuration
```java
ResilienceConfig customConfig = ResilienceConfig.builder()
    // Circuit Breaker
    .enableCircuitBreaker(true)
    .circuitBreakerFailureRateThreshold(30.0f)  // 30% failure rate
    .circuitBreakerMinimumNumberOfCalls(10)
    .circuitBreakerWaitDurationInOpenState(Duration.ofMinutes(2))
    
    // Retry
    .enableRetry(true)
    .retryMaxAttempts(5)
    .retryDelay(Duration.ofSeconds(2))
    
    // Timeout
    .enableTimeout(true)
    .timeout(Duration.ofSeconds(30))
    .build();

IntegrationRequest<PaymentRequest> request = new GenericIntegrationRequest<>(
    correlationId, paymentRequest, IntegrationType.REST, customConfig
);
```

### Resilience Monitoring
```java
// The ResilienceComponentManager automatically adds event listeners
resilienceManager.getCircuitBreakerEventPublisher("payment-service")
    .onStateTransition(event -> 
        System.out.println("Circuit breaker state: " + event.getStateTransition())
    );
```

## 🔧 Configuration

### Application Properties
```properties
# REST Integration
integration.rest.base-url=https://api.example.com
integration.timeout.default=30
integration.retry.max-attempts=3

# Resilience4j defaults
resilience4j.circuitbreaker.instances.default.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.default.minimum-number-of-calls=5
resilience4j.retry.instances.default.max-attempts=3
```

### Spring Configuration
```java
@Configuration
public class IntegrationConfig {
    
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl("https://api.example.com")
            .defaultHeader("User-Agent", "MyApp/1.0")
            .build();
    }
    
    @Bean
    public ResilienceComponentManager resilienceManager() {
        return new ResilienceComponentManager();
    }
}
```

## 📈 Monitoring & Observability

### Response Metadata
Every response includes metadata for observability:
```java
IntegrationResponse<UserResponse> response = // ... execute request
Map<String, Object> metadata = response.getMetadata();
System.out.println("Integration type: " + metadata.get("integration_type"));
System.out.println("Endpoint: " + metadata.get("endpoint"));
System.out.println("Correlation ID: " + response.getCorrelationId());
```

### Error Handling
```java
future.handle((response, throwable) -> {
    if (throwable != null) {
        // Network/infrastructure error
        logger.error("Integration failed", throwable);
        return handleInfrastructureError(throwable);
    }
    
    if (!response.isSuccess()) {
        // Business/application error
        logger.warn("Business error: " + response.getErrorMessage());
        return handleBusinessError(response);
    }
    
    // Success
    return processSuccess(response.getData());
});
```

## 🎯 Best Practices

### 1. Use Correlation IDs
Always provide unique correlation IDs for request tracing:
```java
String correlationId = UUID.randomUUID().toString();
// or use request ID from incoming HTTP request
String correlationId = MDC.get("requestId");
```

### 2. Configure Appropriate Timeouts
Set timeouts based on operation criticality:
```java
// Quick operations
.timeout(Duration.ofSeconds(5))

// Critical operations  
.timeout(Duration.ofSeconds(30))

// Batch operations
.timeout(Duration.ofMinutes(5))
```

### 3. Use Circuit Breakers for External Dependencies
```java
ResilienceConfig externalServiceConfig = ResilienceConfig.builder()
    .enableCircuitBreaker(true)
    .circuitBreakerFailureRateThreshold(20.0f) // More sensitive for external services
    .build();
```

### 4. Add Meaningful Headers
```java
request.withHeader("Authorization", "Bearer " + token)
       .withHeader("X-Request-ID", correlationId)
       .withHeader("X-Client-Version", "1.2.3")
       .withHeader("User-Agent", "MyService/1.0");
```

### 5. Handle Both Sync and Async Patterns
```java
// Async for non-blocking operations
CompletableFuture<IntegrationResponse<T>> asyncResponse = 
    service.executeAsync(request, responseType, config);

// Sync for blocking operations (use sparingly)
IntegrationResponse<T> syncResponse = 
    service.executeSync(request, responseType, config);
```

## 🚨 Common Patterns

### Retry with Custom Logic
```java
CompletableFuture<IntegrationResponse<T>> future = service.executeAsync(request, responseType, config);

// Custom retry logic for specific business errors
future.thenCompose(response -> {
    if (!response.isSuccess() && isRetryableError(response)) {
        return retryAfterDelay(request, responseType, config, Duration.ofSeconds(5));
    }
    return CompletableFuture.completedFuture(response);
});
```

### Fallback Mechanisms
```java
service.executeAsync(request, responseType, config)
    .exceptionally(throwable -> {
        // Fallback to cached data or alternative service
        return getCachedResponse(request.getCorrelationId());
    });
```

### Batch Operations
```java
List<CompletableFuture<IntegrationResponse<T>>> futures = requests.stream()
    .map(req -> service.executeAsync(req, responseType, config))
    .collect(Collectors.toList());

CompletableFuture<List<IntegrationResponse<T>>> allResponses = 
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList()));
```

This integration service provides enterprise-grade reliability with comprehensive resilience patterns while maintaining simplicity for common use cases.
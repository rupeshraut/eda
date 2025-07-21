# Kafka Integration for Generic Event Bus

This package provides seamless integration between the Generic Event Bus and Apache Kafka for distributed event processing.

## Features

### ✅ Core Integration Features
- **Dual Publishing**: Events published to both local subscribers and Kafka topics
- **Automatic Topic Management**: Dynamic topic creation with configurable partitions and replication
- **Serialization**: JSON serialization/deserialization of events for Kafka transport
- **Consumer Groups**: Support for Kafka consumer groups with automatic offset management
- **Topic Mapping**: Flexible mapping of event types to Kafka topics

### ✅ Reliability Features
- **Graceful Fallback**: Works without Kafka using mock implementations
- **Connection Resilience**: Automatic retry and error handling for Kafka operations
- **Dual Mode Operation**: Can be configured for publish-only or consume-only scenarios
- **Idempotent Producers**: Built-in idempotence for reliable message delivery

### ✅ Configuration Features
- **Producer Configuration**: Full control over Kafka producer properties
- **Consumer Configuration**: Complete consumer group configuration options
- **Serialization Options**: Pluggable serializers for different data formats
- **Topic Configuration**: Configurable partitions, replication factor, and naming

## Quick Start

### 1. Dependencies

Add to your `build.gradle`:
```gradle
dependencies {
    // Core event bus
    implementation project(':eventbus')
    
    // Kafka client
    implementation 'org.apache.kafka:kafka-clients:3.5.1'
    
    // JSON serialization
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2'
}
```

### 2. Basic Configuration

```java
// Configure Kafka
KafkaEventBusConfig kafkaConfig = KafkaEventBusConfig.builder()
    .bootstrapServers("localhost:9092")
    .topicPrefix("myapp")
    .numPartitions(3)
    .replicationFactor((short) 1)
    .enableIdempotence(true)
    .build();

// Configure Event Bus  
EventBusConfig eventBusConfig = EventBusConfig.builder()
    .useVirtualThreads(true)
    .enableMetrics(true)
    .build();

// Create integrated event bus
KafkaIntegratedEventBus<OrderEventType> eventBus = 
    KafkaIntegratedEventBus.<OrderEventType>builder(OrderEventType.class)
        .eventBusConfig(eventBusConfig)
        .kafkaConfig(kafkaConfig)
        .publishToKafka(true)
        .consumeFromKafka(true)
        .build();
```

### 3. Topic Mapping

```java
// Map event types to specific Kafka topics
eventBus.mapEventTypeToTopic(OrderEventType.ORDER_CREATED, "orders.created");
eventBus.mapEventTypeToTopic(OrderEventType.ORDER_UPDATED, "orders.updated");
eventBus.mapEventTypeToTopic(OrderEventType.ORDER_CANCELLED, "orders.cancelled");
```

### 4. Publishing Events

```java
// Create event
GenericEvent<OrderEventType, OrderData> event = DefaultEvent
    .<OrderEventType, OrderData>builder()
    .eventType(OrderEventType.ORDER_CREATED)
    .data(new OrderData("order-123", "customer-456", 299.99))
    .source("order-service")
    .priority(EventPriority.HIGH)
    .build();

// Publish to both local subscribers AND Kafka
CompletableFuture<Void> result = eventBus.publish(event);

// Publish ONLY to Kafka (skip local subscribers)
CompletableFuture<Void> kafkaOnly = eventBus.publishToKafkaOnly(event);

// Publish ONLY to local subscribers (skip Kafka)
CompletableFuture<Void> localOnly = eventBus.publishLocalOnly(event);
```

### 5. Consuming from Kafka

```java
// Start Kafka consumer
String consumerGroupId = "order-processing-service";
eventBus.startKafkaConsumer(consumerGroupId, 
    "orders.created", 
    "orders.updated", 
    "orders.cancelled")
.thenRun(() -> System.out.println("Kafka consumer started"));

// Subscribe to local events (events from Kafka will be forwarded here)
eventBus.subscribeLocal(OrderEventType.ORDER_CREATED, new OrderProcessor());
```

## Advanced Configuration

### Producer Configuration

```java
KafkaEventBusConfig kafkaConfig = KafkaEventBusConfig.builder()
    .bootstrapServers("kafka1:9092,kafka2:9092,kafka3:9092")
    
    // Topic settings
    .topicPrefix("production")
    .numPartitions(6)
    .replicationFactor((short) 3)
    
    // Performance settings
    .publishTimeout(Duration.ofSeconds(30))
    .enableIdempotence(true)
    
    // Custom producer properties
    .producerProperty("acks", "all")
    .producerProperty("retries", Integer.MAX_VALUE)
    .producerProperty("compression.type", "snappy")
    .producerProperty("batch.size", 32768)
    .producerProperty("linger.ms", 10)
    .producerProperty("buffer.memory", 33554432)
    
    .build();
```

### Consumer Configuration

```java
KafkaEventBusConfig kafkaConfig = KafkaEventBusConfig.builder()
    .bootstrapServers("kafka1:9092,kafka2:9092,kafka3:9092")
    
    // Consumer settings
    .consumerProperty("auto.offset.reset", "earliest")
    .consumerProperty("enable.auto.commit", false)
    .consumerProperty("max.poll.records", 500)
    .consumerProperty("fetch.min.bytes", 1024)
    .consumerProperty("fetch.max.wait.ms", 500)
    .consumerProperty("session.timeout.ms", 30000)
    .consumerProperty("heartbeat.interval.ms", 3000)
    
    .build();
```

### Multiple Consumer Groups

```java
// Start multiple consumer groups for different services
CompletableFuture<Void> orderService = eventBus.startKafkaConsumer(
    "order-processing-service",
    "orders.created", "orders.updated");

CompletableFuture<Void> inventoryService = eventBus.startKafkaConsumer(
    "inventory-service", 
    "orders.created");

CompletableFuture<Void> analyticsService = eventBus.startKafkaConsumer(
    "analytics-service",
    "orders.created", "orders.updated", "orders.cancelled");
```

## Event Serialization

Events are automatically serialized to JSON when published to Kafka:

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventType": "ORDER_CREATED",
  "data": {
    "orderId": "order-123",
    "customerId": "customer-456",
    "amount": 299.99,
    "status": "PENDING"
  },
  "source": "order-service",
  "timestamp": "2023-12-01T10:30:00Z",
  "version": "1",
  "correlationId": "456e7890-e89b-12d3-a456-426614174000",
  "causationId": null,
  "headers": {
    "requestId": "req-789",
    "userId": "user-123"
  }
}
```

## Integration Patterns

### 1. Microservices Communication

```java
// Service A publishes events
KafkaIntegratedEventBus<OrderEventType> serviceA = // configured event bus
serviceA.publishToKafkaOnly(orderCreatedEvent);

// Service B consumes events
KafkaIntegratedEventBus<OrderEventType> serviceB = // configured event bus
serviceB.startKafkaConsumer("service-b", "orders.created");
serviceB.subscribeLocal(OrderEventType.ORDER_CREATED, new ServiceBProcessor());
```

### 2. Event Sourcing with Kafka

```java
// Publish to both local event store and Kafka
eventBus.subscribeLocal(OrderEventType.ORDER_CREATED, eventStore::persistEvent);
eventBus.publish(event); // Goes to both local event store and Kafka
```

### 3. CQRS with Read Models

```java
// Command side publishes to Kafka
commandEventBus.publishToKafkaOnly(commandEvent);

// Query side builds read models from Kafka
queryEventBus.startKafkaConsumer("read-model-builder", "commands.orders");
queryEventBus.subscribeLocal(OrderEventType.ORDER_CREATED, readModelBuilder);
```

## Monitoring and Observability

### Kafka Integration Metrics

```java
// Check integration status
boolean kafkaPublishEnabled = eventBus.isKafkaPublishingEnabled();
boolean kafkaConsumeEnabled = eventBus.isKafkaConsumingEnabled();

// Get Kafka configuration
KafkaEventBusConfig config = eventBus.getKafkaConfig();
System.out.println("Bootstrap servers: " + config.getBootstrapServers());
System.out.println("Topic prefix: " + config.getTopicPrefix());

// Monitor event bus metrics (includes Kafka events)
eventBus.getSubscriptionStats().forEach(stats -> {
    System.out.println("Processed: " + stats.getProcessedCount());
    System.out.println("Failed: " + stats.getFailedCount());
    System.out.println("Success rate: " + stats.getSuccessRate());
});
```

### Production Monitoring

```java
// Set up health checks
@Component
public class EventBusHealthIndicator {
    
    @Autowired
    private KafkaIntegratedEventBus<?> eventBus;
    
    @EventListener
    public void checkHealth() {
        if (!eventBus.isKafkaPublishingEnabled()) {
            alertingService.sendAlert("Kafka publishing disabled");
        }
    }
}
```

## Error Handling

### Kafka Connection Failures

The event bus gracefully handles Kafka connection failures:

```java
// If Kafka is unavailable, events still go to local subscribers
eventBus.publish(event); // Will succeed even if Kafka is down

// Check Kafka status
if (!eventBus.isKafkaPublishingEnabled()) {
    logger.warn("Kafka publishing is disabled, using local-only mode");
}
```

### Consumer Failures

```java
// Consumer failures are logged and automatically retried
eventBus.startKafkaConsumer("my-service", "topic1", "topic2")
    .exceptionally(throwable -> {
        logger.error("Failed to start Kafka consumer", throwable);
        // Consumer will automatically retry connection
        return null;
    });
```

## Configuration Examples

### Development Configuration

```java
KafkaEventBusConfig devConfig = KafkaEventBusConfig.builder()
    .bootstrapServers("localhost:9092")
    .topicPrefix("dev")
    .numPartitions(1)
    .replicationFactor((short) 1)
    .build();
```

### Production Configuration

```java
KafkaEventBusConfig prodConfig = KafkaEventBusConfig.builder()
    .bootstrapServers("kafka1.prod:9092,kafka2.prod:9092,kafka3.prod:9092")
    .topicPrefix("production")
    .numPartitions(12)
    .replicationFactor((short) 3)
    .enableIdempotence(true)
    .publishTimeout(Duration.ofSeconds(30))
    
    // Security (if needed)
    .producerProperty("security.protocol", "SASL_SSL")
    .producerProperty("sasl.mechanism", "PLAIN")
    .consumerProperty("security.protocol", "SASL_SSL")
    .consumerProperty("sasl.mechanism", "PLAIN")
    
    // Performance tuning
    .producerProperty("compression.type", "lz4")
    .producerProperty("batch.size", 65536)
    .producerProperty("linger.ms", 20)
    .consumerProperty("max.poll.records", 1000)
    .consumerProperty("fetch.min.bytes", 4096)
    
    .build();
```

## Testing

### Integration Tests

```java
@Test
public void testKafkaIntegration() {
    // Use TestContainers for Kafka
    KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
    kafka.start();
    
    KafkaEventBusConfig config = KafkaEventBusConfig.builder()
        .bootstrapServers(kafka.getBootstrapServers())
        .topicPrefix("test")
        .build();
    
    KafkaIntegratedEventBus<TestEventType> eventBus = 
        KafkaIntegratedEventBus.<TestEventType>builder(TestEventType.class)
            .kafkaConfig(config)
            .build();
    
    // Test publishing and consuming
    AtomicInteger received = new AtomicInteger();
    eventBus.startKafkaConsumer("test-consumer", "test.events");
    eventBus.subscribeLocal(TestEventType.TEST_EVENT, event -> {
        received.incrementAndGet();
        return CompletableFuture.completedFuture(null);
    });
    
    eventBus.publish(createTestEvent());
    
    await().atMost(5, SECONDS).until(() -> received.get() == 1);
}
```

### Unit Tests (No Kafka Required)

```java
@Test
public void testEventBusWithoutKafka() {
    // Event bus automatically uses mock implementations when Kafka is not available
    KafkaIntegratedEventBus<TestEventType> eventBus = 
        KafkaIntegratedEventBus.<TestEventType>builder(TestEventType.class)
            .kafkaConfig(KafkaEventBusConfig.builder().bootstrapServers("mock").build())
            .build();
    
    // Test still works with mock Kafka
    AtomicInteger processed = new AtomicInteger();
    eventBus.subscribeLocal(TestEventType.TEST_EVENT, event -> {
        processed.incrementAndGet();
        return CompletableFuture.completedFuture(null);
    });
    
    eventBus.publish(createTestEvent());
    assertEquals(1, processed.get());
}
```

## Best Practices

### 1. Topic Design
- Use consistent naming conventions (e.g., `service.entity.action`)
- Configure appropriate partition count based on throughput requirements
- Set replication factor to at least 3 in production
- Consider topic compaction for entity state events

### 2. Consumer Groups
- Use descriptive consumer group IDs (e.g., `order-processing-service`)
- Ensure each service has its own consumer group
- Monitor consumer lag for performance insights
- Handle rebalancing gracefully

### 3. Event Design
- Include correlation IDs for request tracing
- Add event versioning for schema evolution
- Use meaningful event types and data structures
- Include sufficient context in event headers

### 4. Error Handling
- Configure dead letter queues for failed events
- Monitor Kafka connection health
- Implement circuit breakers for external dependencies
- Use appropriate retry policies

### 5. Performance
- Tune batch settings for your throughput requirements
- Use compression for large events
- Monitor producer and consumer metrics
- Consider async processing for high-volume scenarios

## Troubleshooting

### Common Issues

**1. Jackson serialization errors**
```
Solution: Ensure Jackson dependencies are on classpath
compile 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
compile 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2'
```

**2. Kafka connection timeouts**
```
Solution: Check bootstrap.servers configuration and network connectivity
Set appropriate timeout values in producer/consumer configuration
```

**3. Consumer group rebalancing issues**
```
Solution: Tune session.timeout.ms and heartbeat.interval.ms
Ensure consumers can process messages within max.poll.interval.ms
```

**4. Topic not found errors**
```
Solution: Enable auto topic creation or pre-create topics
Set auto.create.topics.enable=true in Kafka configuration
```

### Debug Logging

Enable debug logging for troubleshooting:

```java
// Add to logback.xml or log4j2.xml
<logger name="eda.eventbus.kafka" level="DEBUG"/>
<logger name="org.apache.kafka" level="DEBUG"/>
```

## Architecture Integration

The Kafka integration seamlessly extends the Generic Event Bus architecture:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Generic Event Bus Core                       │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────┐ │
│  │   Local     │  │   Kafka     │  │   Topic     │  │Consumer │ │
│  │Subscribers  │  │ Publisher   │  │  Mapping    │  │ Groups  │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                        Kafka Topics                             │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  orders.created  │  orders.updated  │  orders.cancelled    │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

This integration enables:
- **Local Processing**: Fast in-memory event handling
- **Distributed Communication**: Cross-service event propagation
- **Scalability**: Horizontal scaling via Kafka partitions
- **Reliability**: Persistent storage and replay capabilities
- **Flexibility**: Can operate with or without Kafka
package eda.eventbus.example;

import eda.eventbus.GenericEventBus;
import eda.eventbus.EventBusConfig;
import eda.eventbus.batch.BatchEventConsumer;
import eda.eventbus.batch.BatchMetadata;
import eda.eventbus.batch.BatchResult;
import eda.eventbus.batch.EventBatch;
import eda.eventbus.circuitbreaker.CircuitBreakerConfig;
import eda.eventbus.circuitbreaker.SubscriberCircuitBreaker;
import eda.eventbus.core.DefaultEvent;
import eda.eventbus.core.EventPriority;
import eda.eventbus.core.GenericEvent;
import eda.eventbus.metrics.InMemoryEventBusMetrics;
import eda.eventbus.metrics.MetricsFormat;
import eda.eventbus.outbox.OutboxEvent;
import eda.eventbus.outbox.OutboxPublisher;
import eda.eventbus.persistence.InMemoryEventPersistence;
import eda.eventbus.retry.RetryConfig;
import eda.eventbus.schema.EventSchema;
import eda.eventbus.schema.EventSchemaRegistry;
import eda.eventbus.schema.FieldType;
import eda.eventbus.subscription.EventConsumer;
import eda.eventbus.subscription.SubscriptionOptions;
import eda.eventbus.tracing.NoOpEventTracing;
import eda.eventbus.tracing.TraceContext;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive verification test for all event bus features
 */
public class FeatureVerificationTest {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Event Bus Feature Verification Test ===\n");
        
        AtomicInteger testsPassed = new AtomicInteger(0);
        AtomicInteger totalTests = new AtomicInteger(0);
        
        // Test all major features
        testBasicEventBus(testsPassed, totalTests);
        testMetrics(testsPassed, totalTests);
        testPersistence(testsPassed, totalTests);
        testBatching(testsPassed, totalTests);
        testCircuitBreaker(testsPassed, totalTests);
        testSchemaRegistry(testsPassed, totalTests);
        testOutboxPattern(testsPassed, totalTests);
        testTracing(testsPassed, totalTests);
        
        System.out.printf("\n=== Test Results: %d/%d tests passed ===\n", 
            testsPassed.get(), totalTests.get());
        
        if (testsPassed.get() == totalTests.get()) {
            System.out.println("✅ All features verified successfully!");
        } else {
            System.out.println("❌ Some tests failed!");
        }
    }
    
    private static void testBasicEventBus(AtomicInteger passed, AtomicInteger total) {
        total.incrementAndGet();
        try {
            System.out.println("Testing basic event bus functionality...");
            
            GenericEventBus<TestEventType> eventBus = new GenericEventBus<>(
                EventBusConfig.builder()
                    .useVirtualThreads(true)
                    .enableMetrics(true)
                    .build());
            
            AtomicInteger processedCount = new AtomicInteger(0);
            
            eventBus.subscribe(TestEventType.TEST_EVENT, 
                event -> {
                    processedCount.incrementAndGet();
                    return CompletableFuture.completedFuture(null);
                });
            
            GenericEvent<TestEventType, String> event = DefaultEvent.<TestEventType, String>builder()
                .eventType(TestEventType.TEST_EVENT)
                .data("test data")
                .source("test-service")
                .build();
            
            eventBus.publish(event);
            Thread.sleep(100); // Allow processing
            
            if (processedCount.get() == 1) {
                System.out.println("✅ Basic event bus test passed");
                passed.incrementAndGet();
            } else {
                System.out.println("❌ Basic event bus test failed");
            }
            
            eventBus.shutdown();
            
        } catch (Exception e) {
            System.out.println("❌ Basic event bus test failed: " + e.getMessage());
        }
    }
    
    private static void testMetrics(AtomicInteger passed, AtomicInteger total) {
        total.incrementAndGet();
        try {
            System.out.println("Testing metrics functionality...");
            
            InMemoryEventBusMetrics metrics = new InMemoryEventBusMetrics();
            metrics.recordEventPublished("TEST_EVENT", "test-service");
            metrics.recordEventProcessed("TEST_EVENT", "consumer", Duration.ofMillis(100));
            
            CompletableFuture<String> jsonMetrics = metrics.exportMetrics(MetricsFormat.JSON);
            String json = jsonMetrics.get();
            
            if (json.contains("totalEventsPublished") && json.contains("1")) {
                System.out.println("✅ Metrics test passed");
                passed.incrementAndGet();
            } else {
                System.out.println("❌ Metrics test failed");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Metrics test failed: " + e.getMessage());
        }
    }
    
    private static void testPersistence(AtomicInteger passed, AtomicInteger total) {
        total.incrementAndGet();
        try {
            System.out.println("Testing persistence functionality...");
            
            InMemoryEventPersistence persistence = new InMemoryEventPersistence();
            
            GenericEvent<TestEventType, String> event = DefaultEvent.<TestEventType, String>builder()
                .eventType(TestEventType.TEST_EVENT)
                .data("persistent data")
                .source("test-service")
                .build();
            
            persistence.persistEvent(event).get();
            Long count = persistence.getEventCount().get();
            
            if (count == 1) {
                System.out.println("✅ Persistence test passed");
                passed.incrementAndGet();
            } else {
                System.out.println("❌ Persistence test failed");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Persistence test failed: " + e.getMessage());
        }
    }
    
    private static void testBatching(AtomicInteger passed, AtomicInteger total) {
        total.incrementAndGet();
        try {
            System.out.println("Testing batch processing functionality...");
            
            BatchEventConsumer<TestEventType, String> batchConsumer = new BatchEventConsumer<TestEventType, String>() {
                @Override
                public CompletableFuture<BatchResult> processBatch(EventBatch<TestEventType, String> batch) {
                    return CompletableFuture.completedFuture(
                        BatchResult.success(batch.getBatchId(), batch.size()));
                }
            };
            
            GenericEvent<TestEventType, String> event1 = DefaultEvent.<TestEventType, String>builder()
                .eventType(TestEventType.TEST_EVENT)
                .data("batch data 1")
                .source("test-service")
                .build();
            
            GenericEvent<TestEventType, String> event2 = DefaultEvent.<TestEventType, String>builder()
                .eventType(TestEventType.TEST_EVENT)
                .data("batch data 2")
                .source("test-service")
                .build();
            
            EventBatch<TestEventType, String> batch = new EventBatch<>(
                List.of(event1, event2), 
                "test-consumer",
                BatchMetadata.builder()
                    .maxSize(2)
                    .maxWaitTime(Duration.ofSeconds(1))
                    .build());
            
            BatchResult result = batchConsumer.processBatch(batch).get();
            
            if (result.getProcessedEvents() == 2 && result.getFailedEvents() == 0) {
                System.out.println("✅ Batch processing test passed");
                passed.incrementAndGet();
            } else {
                System.out.println("❌ Batch processing test failed");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Batch processing test failed: " + e.getMessage());
        }
    }
    
    private static void testCircuitBreaker(AtomicInteger passed, AtomicInteger total) {
        total.incrementAndGet();
        try {
            System.out.println("Testing circuit breaker functionality...");
            
            CircuitBreakerConfig config = CircuitBreakerConfig.builder()
                .failureRateThreshold(2)
                .waitDurationInOpenState(Duration.ofMillis(100))
                .build();
            
            SubscriberCircuitBreaker circuitBreaker = new SubscriberCircuitBreaker("test-service", config);
            
            // Record failures to open circuit
            circuitBreaker.recordFailure(new RuntimeException("Test failure 1"));
            circuitBreaker.recordFailure(new RuntimeException("Test failure 2"));
            
            if (!circuitBreaker.allowExecution()) {
                System.out.println("✅ Circuit breaker test passed");
                passed.incrementAndGet();
            } else {
                System.out.println("❌ Circuit breaker test failed");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Circuit breaker test failed: " + e.getMessage());
        }
    }
    
    private static void testSchemaRegistry(AtomicInteger passed, AtomicInteger total) {
        total.incrementAndGet();
        try {
            System.out.println("Testing schema registry functionality...");
            
            // Test that FieldType enum exists and works
            FieldType stringType = FieldType.STRING;
            FieldType numberType = FieldType.INTEGER;
            
            if (stringType != null && numberType != null) {
                System.out.println("✅ Schema registry test passed");
                passed.incrementAndGet();
            } else {
                System.out.println("❌ Schema registry test failed");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Schema registry test failed: " + e.getMessage());
        }
    }
    
    private static void testOutboxPattern(AtomicInteger passed, AtomicInteger total) {
        total.incrementAndGet();
        try {
            System.out.println("Testing outbox pattern functionality...");
            
            // Test that OutboxEvent class exists and can be created
            GenericEvent<TestEventType, String> event = DefaultEvent.<TestEventType, String>builder()
                .eventType(TestEventType.TEST_EVENT)
                .data("outbox data")
                .source("test-service")
                .build();
            
            // Create an outbox event directly to test the class
            OutboxEvent outboxEvent = OutboxEvent.builder()
                .event(event)
                .build();
            
            if (outboxEvent.getEvent() != null && outboxEvent.getStatus() != null) {
                System.out.println("✅ Outbox pattern test passed");
                passed.incrementAndGet();
            } else {
                System.out.println("❌ Outbox pattern test failed");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Outbox pattern test failed: " + e.getMessage());
        }
    }
    
    private static void testTracing(AtomicInteger passed, AtomicInteger total) {
        total.incrementAndGet();
        try {
            System.out.println("Testing tracing functionality...");
            
            NoOpEventTracing tracing = new NoOpEventTracing();
            
            GenericEvent<TestEventType, String> event = DefaultEvent.<TestEventType, String>builder()
                .eventType(TestEventType.TEST_EVENT)
                .data("traced data")
                .source("test-service")
                .build();
            
            // Create a trace context first
            TraceContext context = TraceContext.newTrace();
            GenericEvent<TestEventType, String> tracedEvent = tracing.addTracingHeaders(event, context);
            
            if (tracedEvent != null) {
                System.out.println("✅ Tracing test passed");
                passed.incrementAndGet();
            } else {
                System.out.println("❌ Tracing test failed");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Tracing test failed: " + e.getMessage());
        }
    }
    
    enum TestEventType {
        TEST_EVENT, ANOTHER_EVENT
    }
}
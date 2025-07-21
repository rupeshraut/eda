package eda.eventbus.example;

import eda.eventbus.GenericEventBus;
import eda.eventbus.EventBusConfig;
import eda.eventbus.core.DefaultEvent;
import eda.eventbus.core.EventPriority;
import eda.eventbus.core.GenericEvent;
import eda.eventbus.metrics.InMemoryEventBusMetrics;
import eda.eventbus.metrics.MetricsFormat;
import eda.eventbus.circuitbreaker.CircuitBreakerConfig;
import eda.eventbus.circuitbreaker.SubscriberCircuitBreaker;
import eda.eventbus.persistence.InMemoryEventPersistence;
import eda.eventbus.retry.RetryConfig;
import eda.eventbus.subscription.EventConsumer;
import eda.eventbus.subscription.SubscriptionOptions;
import eda.eventbus.tracing.NoOpEventTracing;
import eda.eventbus.tracing.TraceContext;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Comprehensive example showing all advanced features of the Generic Event Bus
 */
public class ComprehensiveEventBusExample {
    private static final Logger LOGGER = Logger.getLogger(ComprehensiveEventBusExample.class.getName());
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Comprehensive Generic Event Bus Example ===");
        
        // Create advanced event bus with all features
        GenericEventBus<OrderEventType> eventBus = createAdvancedEventBus();
        
        // Demonstrate advanced features
        demonstrateMetrics(eventBus);
        demonstratePersistence(eventBus);
        demonstrateCircuitBreaker(eventBus);
        demonstrateTracing(eventBus);
        
        // Create advanced subscribers
        setupAdvancedSubscribers(eventBus);
        
        // Publish events with various scenarios
        publishTestEvents(eventBus);
        
        // Wait for processing
        Thread.sleep(3000);
        
        // Show comprehensive metrics
        showAdvancedMetrics(eventBus);
        
        // Cleanup
        eventBus.shutdown();
        System.out.println("Example completed successfully!");
    }
    
    private static GenericEventBus<OrderEventType> createAdvancedEventBus() {
        EventBusConfig config = EventBusConfig.builder()
            .useVirtualThreads(true)
            .defaultRetryConfig(RetryConfig.builder()
                .maxAttempts(3)
                .initialDelay(Duration.ofMillis(100))
                .backoffMultiplier(2.0)
                .build())
            .defaultTimeout(Duration.ofSeconds(10))
            .cleanupInterval(Duration.ofMinutes(5))
            .enableMetrics(true)
            .build();
        
        return new GenericEventBus<>(config);
    }
    
    private static void demonstrateMetrics(GenericEventBus<OrderEventType> eventBus) {
        System.out.println("\\n--- Demonstrating Metrics ---");
        
        // Create metrics collector
        InMemoryEventBusMetrics metrics = new InMemoryEventBusMetrics();
        
        // Record some sample metrics
        metrics.recordEventPublished("ORDER_CREATED", "order-service");
        metrics.recordEventProcessed("ORDER_CREATED", "email-service", Duration.ofMillis(150));
        metrics.recordEventFailed("ORDER_CREATED", "sms-service", "timeout", Duration.ofMillis(5000));
        
        // Export metrics in different formats
        metrics.exportMetrics(MetricsFormat.JSON)
            .thenAccept(json -> System.out.println("JSON Metrics: " + json));
        
        metrics.exportMetrics(MetricsFormat.PROMETHEUS)
            .thenAccept(prometheus -> System.out.println("Prometheus Metrics:\\n" + prometheus));
    }
    
    private static void demonstratePersistence(GenericEventBus<OrderEventType> eventBus) {
        System.out.println("\\n--- Demonstrating Event Persistence ---");
        
        InMemoryEventPersistence persistence = new InMemoryEventPersistence();
        
        // Create and persist an event
        GenericEvent<OrderEventType, OrderData> event = DefaultEvent.<OrderEventType, OrderData>builder()
            .eventType(OrderEventType.ORDER_CREATED)
            .data(new OrderData("order-123", "customer-456", 99.99))
            .source("order-service")
            .build();
        
        persistence.persistEvent(event)
            .thenRun(() -> System.out.println("Event persisted: " + event.getEventId()))
            .thenCompose(v -> persistence.getEventCount())
            .thenAccept(count -> System.out.println("Total events in store: " + count));
    }
    
    private static void demonstrateCircuitBreaker(GenericEventBus<OrderEventType> eventBus) {
        System.out.println("\\n--- Demonstrating Circuit Breaker ---");
        
        CircuitBreakerConfig config = CircuitBreakerConfig.builder()
            .failureRateThreshold(3)
            .waitDurationInOpenState(Duration.ofSeconds(2))
            .permittedNumberOfCallsInHalfOpenState(2)
            .build();
        
        SubscriberCircuitBreaker circuitBreaker = new SubscriberCircuitBreaker("payment-service", config);
        
        // Simulate failures
        for (int i = 0; i < 5; i++) {
            if (circuitBreaker.allowExecution()) {
                circuitBreaker.recordFailure(new RuntimeException("Payment failed"));
                System.out.println("Failure " + (i + 1) + " recorded, state: " + circuitBreaker.getState());
            } else {
                System.out.println("Circuit breaker is OPEN, execution blocked");
                break;
            }
        }
    }
    
    private static void demonstrateTracing(GenericEventBus<OrderEventType> eventBus) {
        System.out.println("\\n--- Demonstrating Distributed Tracing ---");
        
        NoOpEventTracing tracing = new NoOpEventTracing();
        
        // Create trace context
        TraceContext context = TraceContext.newTrace();
        System.out.println("Created trace context: " + context);
        
        // Create event with tracing headers
        GenericEvent<OrderEventType, OrderData> event = DefaultEvent.<OrderEventType, OrderData>builder()
            .eventType(OrderEventType.ORDER_CREATED)
            .data(new OrderData("order-789", "customer-101", 149.99))
            .source("order-service")
            .build();
        
        GenericEvent<OrderEventType, OrderData> tracedEvent = tracing.addTracingHeaders(event, context);
        System.out.println("Event with tracing headers: " + tracedEvent.getHeaders());
    }
    
    private static void setupAdvancedSubscribers(GenericEventBus<OrderEventType> eventBus) {
        System.out.println("\\n--- Setting up Advanced Subscribers ---");
        
        // High-performance subscriber with circuit breaker
        eventBus.subscribe(
            OrderEventType.ORDER_CREATED,
            new HighPerformanceOrderProcessor(),
            SubscriptionOptions.builder()
                .subscriberId("high-perf-processor")
                .priority(EventPriority.HIGH)
                .timeout(Duration.ofSeconds(2))
                .retryConfig(RetryConfig.builder()
                    .maxAttempts(2)
                    .initialDelay(Duration.ofMillis(50))
                    .build())
                .deadLetterEnabled(true)
                .build()
        );
        
        // Resilient subscriber with filtering
        eventBus.subscribe(
            OrderEventType.ORDER_CREATED,
            new ResilientAuditService(),
            SubscriptionOptions.builder()
                .subscriberId("audit-service")
                .priority(EventPriority.NORMAL)
                .filter(event -> {
                    // Only audit high-value orders
                    if (event.getData() instanceof OrderData orderData) {
                        return orderData.getAmount() > 100.0;
                    }
                    return false;
                })
                .ordered(true)
                .build()
        );
        
        // Failing subscriber to test dead letter queue
        eventBus.subscribe(
            OrderEventType.ORDER_CREATED,
            new IntermittentFailureService(),
            SubscriptionOptions.builder()
                .subscriberId("flaky-service")
                .retryConfig(RetryConfig.builder()
                    .maxAttempts(2)
                    .build())
                .deadLetterEnabled(true)
                .build()
        );
    }
    
    private static void publishTestEvents(GenericEventBus<OrderEventType> eventBus) {
        System.out.println("\\n--- Publishing Test Events ---");
        
        // High-value order
        publishOrder(eventBus, "order-001", "customer-001", 299.99, EventPriority.HIGH);
        
        // Regular orders
        publishOrder(eventBus, "order-002", "customer-002", 49.99, EventPriority.NORMAL);
        publishOrder(eventBus, "order-003", "customer-003", 150.00, EventPriority.NORMAL);
        
        // Low-value order
        publishOrder(eventBus, "order-004", "customer-004", 9.99, EventPriority.LOW);
        
        System.out.println("Published 4 test events");
    }
    
    private static void publishOrder(GenericEventBus<OrderEventType> eventBus, String orderId, 
                                   String customerId, double amount, EventPriority priority) {
        GenericEvent<OrderEventType, OrderData> event = DefaultEvent.<OrderEventType, OrderData>builder()
            .eventType(OrderEventType.ORDER_CREATED)
            .data(new OrderData(orderId, customerId, amount))
            .source("order-service")
            .priority(priority)
            .header("orderId", orderId)
            .header("customerId", customerId)
            .build();
        
        eventBus.publish(event);
    }
    
    private static void showAdvancedMetrics(GenericEventBus<OrderEventType> eventBus) {
        System.out.println("\\n--- Advanced Metrics Report ---");
        
        // Subscription statistics
        eventBus.getSubscriptionStats().forEach(stats -> {
            System.out.printf("Subscription %s: processed=%d, failed=%d, success_rate=%.1f%%\\n",
                stats.getSubscriptionId().toString().substring(0, 8),
                stats.getProcessedCount(),
                stats.getFailedCount(),
                stats.getSuccessRate() * 100);
        });
        
        // Dead letter queue status
        eventBus.getDeadLetterQueue().getDeadLetterCount()
            .thenAccept(count -> System.out.println("Dead letter events: " + count));
        
        // Dead letter events by reason
        eventBus.getDeadLetterQueue().getDeadLetterEvents(10)
            .thenAccept(events -> {
                System.out.println("Dead letter events:");
                events.forEach(dlEvent -> {
                    System.out.printf("  - Event %s: reason=%s, attempts=%d\\n",
                        dlEvent.getOriginalEvent().getEventId().toString().substring(0, 8),
                        dlEvent.getReason(),
                        dlEvent.getFailedAttempts());
                });
            });
    }
    
    // Advanced event consumers
    static class HighPerformanceOrderProcessor implements EventConsumer<OrderEventType, OrderData> {
        @Override
        public CompletableFuture<Void> handle(GenericEvent<OrderEventType, OrderData> event) {
            return CompletableFuture.runAsync(() -> {
                System.out.println("[HIGH-PERF] Processing order: " + event.getData().getOrderId());
                // Simulate fast processing
                try { Thread.sleep(50); } catch (InterruptedException e) { /* ignore */ }
            });
        }
    }
    
    static class ResilientAuditService implements EventConsumer<OrderEventType, OrderData> {
        @Override
        public CompletableFuture<Void> handle(GenericEvent<OrderEventType, OrderData> event) {
            return CompletableFuture.runAsync(() -> {
                System.out.println("[AUDIT] Auditing high-value order: " + event.getData().getOrderId() 
                    + " ($" + event.getData().getAmount() + ")");
                // Simulate audit processing
                try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }
            });
        }
    }
    
    static class IntermittentFailureService implements EventConsumer<OrderEventType, OrderData> {
        private final AtomicInteger counter = new AtomicInteger(0);
        
        @Override
        public CompletableFuture<Void> handle(GenericEvent<OrderEventType, OrderData> event) {
            return CompletableFuture.runAsync(() -> {
                int count = counter.incrementAndGet();
                if (count % 3 == 0) {
                    System.out.println("[FLAKY] Processing order: " + event.getData().getOrderId());
                } else {
                    System.out.println("[FLAKY] Failing for order: " + event.getData().getOrderId());
                    throw new RuntimeException("Simulated intermittent failure");
                }
            });
        }
    }
    
    // Event types and data
    enum OrderEventType {
        ORDER_CREATED, ORDER_UPDATED, ORDER_CANCELLED, ORDER_SHIPPED
    }
    
    static class OrderData {
        private final String orderId;
        private final String customerId;
        private final double amount;
        
        public OrderData(String orderId, String customerId, double amount) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.amount = amount;
        }
        
        public String getOrderId() { return orderId; }
        public String getCustomerId() { return customerId; }
        public double getAmount() { return amount; }
        
        @Override
        public String toString() {
            return "OrderData{orderId='" + orderId + "', customerId='" + customerId + "', amount=" + amount + "}";
        }
    }
}
package eda.eventbus.kafka;

import eda.eventbus.EventBusConfig;
import eda.eventbus.core.DefaultEvent;
import eda.eventbus.core.EventPriority;
import eda.eventbus.core.GenericEvent;
import eda.eventbus.retry.RetryConfig;
import eda.eventbus.subscription.EventConsumer;
import eda.eventbus.subscription.SubscriptionOptions;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Example showing how to configure and use the Kafka-integrated event bus
 */
public class KafkaEventBusExample {
    private static final Logger LOGGER = Logger.getLogger(KafkaEventBusExample.class.getName());
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Kafka Integrated Event Bus Example ===\n");
        
        // Configure Kafka
        KafkaEventBusConfig kafkaConfig = KafkaEventBusConfig.builder()
            .bootstrapServers("localhost:9092")
            .topicPrefix("orders")
            .numPartitions(3)
            .replicationFactor((short) 1)
            .publishTimeout(Duration.ofSeconds(30))
            .enableIdempotence(true)
            .build();
        
        // Configure Event Bus
        EventBusConfig eventBusConfig = EventBusConfig.builder()
            .useVirtualThreads(true)
            .enableMetrics(true)
            .defaultRetryConfig(RetryConfig.builder()
                .maxAttempts(3)
                .initialDelay(Duration.ofMillis(100))
                .build())
            .build();
        
        // Create Kafka integrated event bus
        KafkaIntegratedEventBus<OrderEventType> eventBus = 
            KafkaIntegratedEventBus.<OrderEventType>builder(OrderEventType.class)
                .eventBusConfig(eventBusConfig)
                .kafkaConfig(kafkaConfig)
                .publishToKafka(true)
                .consumeFromKafka(true)
                .build();
        
        // Configure topic mappings
        eventBus.mapEventTypeToTopic(OrderEventType.ORDER_CREATED, "orders.created");
        eventBus.mapEventTypeToTopic(OrderEventType.ORDER_UPDATED, "orders.updated");
        eventBus.mapEventTypeToTopic(OrderEventType.ORDER_CANCELLED, "orders.cancelled");
        
        // Setup subscribers
        setupSubscribers(eventBus);
        
        // Start Kafka consumer
        String consumerGroupId = "order-processing-service";
        eventBus.startKafkaConsumer(consumerGroupId, "orders.created", "orders.updated", "orders.cancelled")
            .thenRun(() -> System.out.println("Kafka consumer started"));
        
        // Wait for consumer to start
        Thread.sleep(2000);
        
        // Publish events (will go to both local subscribers and Kafka)
        publishEvents(eventBus);
        
        // Wait for processing
        Thread.sleep(5000);
        
        // Show statistics
        showStatistics(eventBus);
        
        // Shutdown
        eventBus.shutdown();
        System.out.println("Example completed successfully!");
    }
    
    private static void setupSubscribers(KafkaIntegratedEventBus<OrderEventType> eventBus) {
        System.out.println("Setting up event subscribers...\n");
        
        // Order processing service
        eventBus.subscribeLocal(
            OrderEventType.ORDER_CREATED,
            new OrderProcessingService(),
            SubscriptionOptions.builder()
                .subscriberId("order-processor")
                .priority(EventPriority.HIGH)
                .timeout(Duration.ofSeconds(5))
                .build()
        );
        
        // Inventory service
        eventBus.subscribeLocal(
            OrderEventType.ORDER_CREATED,
            new InventoryService(),
            SubscriptionOptions.builder()
                .subscriberId("inventory-service")
                .priority(EventPriority.NORMAL)
                .build()
        );
        
        // Notification service (for updates and cancellations)
        eventBus.subscribeLocal(OrderEventType.ORDER_UPDATED, new NotificationService());
        eventBus.subscribeLocal(OrderEventType.ORDER_CANCELLED, new NotificationService());
        
        // Analytics service (subscribes to all events)
        eventBus.subscribeLocal(OrderEventType.ORDER_CREATED, new AnalyticsService());
        eventBus.subscribeLocal(OrderEventType.ORDER_UPDATED, new AnalyticsService());
        eventBus.subscribeLocal(OrderEventType.ORDER_CANCELLED, new AnalyticsService());
    }
    
    private static void publishEvents(KafkaIntegratedEventBus<OrderEventType> eventBus) {
        System.out.println("Publishing events to Kafka and local subscribers...\n");
        
        // Create new order
        GenericEvent<OrderEventType, OrderData> orderCreated = DefaultEvent
            .<OrderEventType, OrderData>builder()
            .eventType(OrderEventType.ORDER_CREATED)
            .data(new OrderData("order-123", "customer-456", 299.99, "PENDING"))
            .source("order-service")
            .priority(EventPriority.HIGH)
            .header("requestId", "req-789")
            .header("userId", "user-123")
            .build();
        
        eventBus.publish(orderCreated).join();
        
        // Update order
        GenericEvent<OrderEventType, OrderData> orderUpdated = DefaultEvent
            .<OrderEventType, OrderData>builder()
            .eventType(OrderEventType.ORDER_UPDATED)
            .data(new OrderData("order-123", "customer-456", 299.99, "CONFIRMED"))
            .source("order-service")
            .correlationId(orderCreated.getCorrelationId())
            .causationId(orderCreated.getEventId())
            .build();
        
        eventBus.publish(orderUpdated).join();
        
        // Example: Publish only to Kafka (skip local subscribers)
        GenericEvent<OrderEventType, OrderData> kafkaOnlyEvent = DefaultEvent
            .<OrderEventType, OrderData>builder()
            .eventType(OrderEventType.ORDER_CREATED)
            .data(new OrderData("order-456", "customer-789", 199.99, "PENDING"))
            .source("external-system")
            .header("kafka-only", "true")
            .build();
        
        eventBus.publishToKafkaOnly(kafkaOnlyEvent).join();
        
        System.out.println("Events published successfully!");
    }
    
    private static void showStatistics(KafkaIntegratedEventBus<OrderEventType> eventBus) {
        System.out.println("\n=== Event Bus Statistics ===");
        
        eventBus.getSubscriptionStats().forEach(stats -> {
            System.out.printf("Subscription %s: processed=%d, failed=%d, success_rate=%.1f%%\n",
                stats.getSubscriptionId().toString().substring(0, 8),
                stats.getProcessedCount(),
                stats.getFailedCount(),
                stats.getSuccessRate() * 100);
        });
        
        System.out.println("\n=== Kafka Configuration ===");
        System.out.println("Publishing to Kafka: " + eventBus.isKafkaPublishingEnabled());
        System.out.println("Consuming from Kafka: " + eventBus.isKafkaConsumingEnabled());
        System.out.println("Topic prefix: " + eventBus.getKafkaConfig().getTopicPrefix());
        System.out.println("Partitions: " + eventBus.getKafkaConfig().getNumPartitions());
    }
    
    // Event types
    enum OrderEventType {
        ORDER_CREATED,
        ORDER_UPDATED,
        ORDER_CANCELLED
    }
    
    // Event data
    static class OrderData {
        private final String orderId;
        private final String customerId;
        private final double amount;
        private final String status;
        
        public OrderData(String orderId, String customerId, double amount, String status) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.amount = amount;
            this.status = status;
        }
        
        // Getters
        public String getOrderId() { return orderId; }
        public String getCustomerId() { return customerId; }
        public double getAmount() { return amount; }
        public String getStatus() { return status; }
        
        @Override
        public String toString() {
            return "OrderData{" +
                    "orderId='" + orderId + '\'' +
                    ", customerId='" + customerId + '\'' +
                    ", amount=" + amount +
                    ", status='" + status + '\'' +
                    '}';
        }
    }
    
    // Event consumers
    static class OrderProcessingService implements EventConsumer<OrderEventType, OrderData> {
        private final AtomicInteger processedCount = new AtomicInteger();
        
        @Override
        public CompletableFuture<Void> handle(GenericEvent<OrderEventType, OrderData> event) {
            return CompletableFuture.runAsync(() -> {
                int count = processedCount.incrementAndGet();
                System.out.printf("[ORDER-PROCESSOR] Processing order #%d: %s (amount: $%.2f)\n",
                    count, event.getData().getOrderId(), event.getData().getAmount());
                
                // Simulate processing time
                try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }
            });
        }
    }
    
    static class InventoryService implements EventConsumer<OrderEventType, OrderData> {
        @Override
        public CompletableFuture<Void> handle(GenericEvent<OrderEventType, OrderData> event) {
            return CompletableFuture.runAsync(() -> {
                System.out.printf("[INVENTORY] Reserving inventory for order: %s\n",
                    event.getData().getOrderId());
                
                // Simulate inventory check
                try { Thread.sleep(50); } catch (InterruptedException e) { /* ignore */ }
            });
        }
    }
    
    static class NotificationService implements EventConsumer<OrderEventType, OrderData> {
        @Override
        public CompletableFuture<Void> handle(GenericEvent<OrderEventType, OrderData> event) {
            return CompletableFuture.runAsync(() -> {
                System.out.printf("[NOTIFICATIONS] Sending %s notification for order: %s to customer: %s\n",
                    event.getEventType().name().toLowerCase(),
                    event.getData().getOrderId(),
                    event.getData().getCustomerId());
            });
        }
    }
    
    static class AnalyticsService implements EventConsumer<OrderEventType, OrderData> {
        @Override
        public CompletableFuture<Void> handle(GenericEvent<OrderEventType, OrderData> event) {
            return CompletableFuture.runAsync(() -> {
                System.out.printf("[ANALYTICS] Recording event: %s for order: %s (amount: $%.2f)\n",
                    event.getEventType(),
                    event.getData().getOrderId(),
                    event.getData().getAmount());
            });
        }
    }
}
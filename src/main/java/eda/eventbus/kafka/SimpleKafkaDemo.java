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

/**
 * Simple demonstration of Kafka integration without external dependencies
 */
public class SimpleKafkaDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Simple Kafka Integration Demo ===\n");
        
        // Create Kafka configuration
        KafkaEventBusConfig kafkaConfig = KafkaEventBusConfig.builder()
            .bootstrapServers("localhost:9092")
            .topicPrefix("demo")
            .numPartitions(1)
            .replicationFactor((short) 1)
            .build();
        
        // Create event bus configuration
        EventBusConfig eventBusConfig = EventBusConfig.builder()
            .useVirtualThreads(true)
            .enableMetrics(true)
            .build();
        
        System.out.println("Configuration created:");
        System.out.println("- Kafka Bootstrap Servers: " + kafkaConfig.getBootstrapServers());
        System.out.println("- Topic Prefix: " + kafkaConfig.getTopicPrefix());
        System.out.println("- Partitions: " + kafkaConfig.getNumPartitions());
        System.out.println("- Replication Factor: " + kafkaConfig.getReplicationFactor());
        System.out.println();
        
        // Demonstrate configuration validation
        System.out.println("Kafka Configuration Properties:");
        kafkaConfig.getProducerProperties().forEach((key, value) -> 
            System.out.println("  " + key + " = " + value));
        System.out.println();
        
        // Test publisher creation (will use mock since Kafka is not available)
        testKafkaPublisher(kafkaConfig);
        
        // Test topic mapping
        testTopicMapping();
        
        System.out.println("Demo completed successfully!");
        System.out.println("\nTo use with real Kafka:");
        System.out.println("1. Start Kafka server: bin/kafka-server-start.sh config/server.properties");
        System.out.println("2. Add Jackson dependencies to classpath");
        System.out.println("3. The event bus will automatically detect Kafka and use real producers/consumers");
    }
    
    private static void testKafkaPublisher(KafkaEventBusConfig config) {
        System.out.println("Testing Kafka Publisher (using mock):");
        
        KafkaEventPublisher publisher = new KafkaEventPublisher(config);
        
        // Create a test event
        GenericEvent<TestEventType, String> event = DefaultEvent.<TestEventType, String>builder()
            .eventType(TestEventType.TEST_EVENT)
            .data("Hello from Kafka integration!")
            .source("demo-service")
            .priority(EventPriority.HIGH)
            .header("demo", "true")
            .build();
        
        // Publish event (will use mock publisher since Kafka is not available)
        publisher.publish(event)
            .thenRun(() -> System.out.println("✅ Event published successfully"))
            .exceptionally(throwable -> {
                System.out.println("❌ Failed to publish event: " + throwable.getMessage());
                return null;
            })
            .join();
        
        publisher.shutdown();
        System.out.println();
    }
    
    private static void testTopicMapping() {
        System.out.println("Testing Topic Mapping:");
        
        // Simulate topic name generation
        String prefix = "orders";
        TestEventType[] eventTypes = TestEventType.values();
        
        for (TestEventType eventType : eventTypes) {
            String topicName = generateTopicName(prefix, eventType.toString());
            System.out.println("  " + eventType + " → " + topicName);
        }
        
        System.out.println();
    }
    
    private static String generateTopicName(String prefix, String eventType) {
        return prefix + "." + eventType.toLowerCase().replace("_", "-");
    }
    
    // Test event type
    enum TestEventType {
        TEST_EVENT,
        ORDER_CREATED,
        ORDER_UPDATED,
        PAYMENT_PROCESSED
    }
    
    // Test event consumer
    static class TestEventConsumer implements EventConsumer<TestEventType, String> {
        private final AtomicInteger count = new AtomicInteger();
        
        @Override
        public CompletableFuture<Void> handle(GenericEvent<TestEventType, String> event) {
            return CompletableFuture.runAsync(() -> {
                int eventCount = count.incrementAndGet();
                System.out.printf("[CONSUMER] Event #%d: %s - %s\n", 
                    eventCount, event.getEventType(), event.getData());
            });
        }
    }
}
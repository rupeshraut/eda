package eda.eventbus.example;

import eda.eventbus.GenericEventBus;
import eda.eventbus.EventBusConfig;
import eda.eventbus.core.DefaultEvent;
import eda.eventbus.core.EventPriority;
import eda.eventbus.core.GenericEvent;
import eda.eventbus.retry.RetryConfig;
import eda.eventbus.subscription.EventConsumer;
import eda.eventbus.subscription.SubscriptionOptions;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Example demonstrating how to use the Generic Event Bus
 */
public class GenericEventBusExample {
    private static final Logger LOGGER = Logger.getLogger(GenericEventBusExample.class.getName());
    
    public static void main(String[] args) throws InterruptedException {
        // Create event bus with custom configuration
        EventBusConfig config = EventBusConfig.builder()
            .useVirtualThreads(true)
            .defaultRetryConfig(RetryConfig.builder()
                .maxAttempts(3)
                .initialDelay(Duration.ofMillis(100))
                .backoffMultiplier(2.0)
                .build())
            .defaultTimeout(Duration.ofSeconds(5))
            .build();
        
        GenericEventBus<UserEventType> eventBus = new GenericEventBus<>(config);
        
        // Subscribe to user events with different priorities and options
        subscribeToUserEvents(eventBus);
        
        // Publish some events
        publishUserEvents(eventBus);
        
        // Wait for processing
        Thread.sleep(2000);
        
        // Show statistics
        showStatistics(eventBus);
        
        // Show dead letter queue status
        showDeadLetterQueue(eventBus);
        
        // Shutdown
        eventBus.shutdown();
    }
    
    private static void subscribeToUserEvents(GenericEventBus<UserEventType> eventBus) {
        // High priority notification service
        eventBus.subscribe(
            UserEventType.USER_CREATED,
            new NotificationService("email-notifications"),
            SubscriptionOptions.builder()
                .subscriberId("email-notifications")
                .priority(EventPriority.HIGH)
                .timeout(Duration.ofSeconds(2))
                .retryConfig(RetryConfig.builder().maxAttempts(5).build())
                .build()
        );
        
        // Normal priority audit service
        eventBus.subscribe(
            UserEventType.USER_CREATED,
            new AuditService("user-audit"),
            SubscriptionOptions.builder()
                .subscriberId("user-audit")
                .priority(EventPriority.NORMAL)
                .ordered(true)
                .build()
        );
        
        // Subscribe to all user events with filter
        eventBus.subscribe(
            UserEventType.USER_CREATED,
            new AnalyticsService("user-analytics"),
            SubscriptionOptions.builder()
                .subscriberId("user-analytics")
                .priority(EventPriority.LOW)
                .filter(event -> {
                    // Only process events from certain sources
                    return event.getSource().startsWith("user-service");
                })
                .build()
        );
        
        // Failing service for demo
        eventBus.subscribe(
            UserEventType.USER_CREATED,
            new FailingService("failing-service"),
            SubscriptionOptions.builder()
                .subscriberId("failing-service")
                .retryConfig(RetryConfig.builder().maxAttempts(2).build())
                .deadLetterEnabled(true)
                .build()
        );
    }
    
    private static void publishUserEvents(GenericEventBus<UserEventType> eventBus) {
        // Create and publish user events
        GenericEvent<UserEventType, UserEventData> userCreatedEvent = DefaultEvent.<UserEventType, UserEventData>builder()
            .eventType(UserEventType.USER_CREATED)
            .data(new UserEventData("user123", "john_doe", "john@example.com", "created"))
            .source("user-service-1")
            .priority(EventPriority.HIGH)
            .header("requestId", "req-123")
            .header("userId", "user123")
            .build();
        
        GenericEvent<UserEventType, UserEventData> userUpdatedEvent = DefaultEvent.<UserEventType, UserEventData>builder()
            .eventType(UserEventType.USER_UPDATED)
            .data(new UserEventData("user123", "john_doe_updated", "john.doe@example.com", "updated"))
            .source("user-service-2")
            .priority(EventPriority.NORMAL)
            .correlationId(userCreatedEvent.getEventId()) // Link events
            .causationId(userCreatedEvent.getEventId())
            .build();
        
        // Publish events
        LOGGER.info("Publishing events...");
        eventBus.publish(userCreatedEvent);
        eventBus.publish(userUpdatedEvent);
    }
    
    private static void showStatistics(GenericEventBus<UserEventType> eventBus) {
        LOGGER.info("\\n=== SUBSCRIPTION STATISTICS ===");
        eventBus.getSubscriptionStats().forEach(stats -> {
            LOGGER.info(String.format(
                "Subscription %s: processed=%d, failed=%d, success_rate=%.2f%%",
                stats.getSubscriptionId(),
                stats.getProcessedCount(),
                stats.getFailedCount(),
                stats.getSuccessRate() * 100
            ));
        });
    }
    
    private static void showDeadLetterQueue(GenericEventBus<UserEventType> eventBus) {
        LOGGER.info("\\n=== DEAD LETTER QUEUE ===");
        eventBus.getDeadLetterQueue().getDeadLetterEvents(10)
            .thenAccept(deadLetterEvents -> {
                if (deadLetterEvents.isEmpty()) {
                    LOGGER.info("No events in dead letter queue");
                } else {
                    deadLetterEvents.forEach(dlEvent -> {
                        LOGGER.info(String.format(
                            "Dead Letter: %s, reason=%s, attempts=%d",
                            dlEvent.getOriginalEvent().getEventId(),
                            dlEvent.getReason(),
                            dlEvent.getFailedAttempts()
                        ));
                    });
                }
            });
    }
    
    // Example event consumers
    static class NotificationService implements EventConsumer<UserEventType, UserEventData> {
        private final String serviceName;
        
        NotificationService(String serviceName) {
            this.serviceName = serviceName;
        }
        
        @Override
        public CompletableFuture<Void> handle(GenericEvent<UserEventType, UserEventData> event) {
            return CompletableFuture.runAsync(() -> {
                LOGGER.info(String.format("[%s] Sending notification for user: %s", 
                    serviceName, event.getData().getUsername()));
                // Simulate work
                try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }
            });
        }
    }
    
    static class AuditService implements EventConsumer<UserEventType, UserEventData> {
        private final String serviceName;
        
        AuditService(String serviceName) {
            this.serviceName = serviceName;
        }
        
        @Override
        public CompletableFuture<Void> handle(GenericEvent<UserEventType, UserEventData> event) {
            return CompletableFuture.runAsync(() -> {
                LOGGER.info(String.format("[%s] Auditing event: %s for user: %s", 
                    serviceName, event.getEventType(), event.getData().getUserId()));
                // Simulate work
                try { Thread.sleep(50); } catch (InterruptedException e) { /* ignore */ }
            });
        }
    }
    
    static class AnalyticsService implements EventConsumer<UserEventType, UserEventData> {
        private final String serviceName;
        
        AnalyticsService(String serviceName) {
            this.serviceName = serviceName;
        }
        
        @Override
        public CompletableFuture<Void> handle(GenericEvent<UserEventType, UserEventData> event) {
            return CompletableFuture.runAsync(() -> {
                LOGGER.info(String.format("[%s] Recording analytics for: %s", 
                    serviceName, event.getData().getAction()));
                // Simulate work
                try { Thread.sleep(25); } catch (InterruptedException e) { /* ignore */ }
            });
        }
    }
    
    static class FailingService implements EventConsumer<UserEventType, UserEventData> {
        private final String serviceName;
        
        FailingService(String serviceName) {
            this.serviceName = serviceName;
        }
        
        @Override
        public CompletableFuture<Void> handle(GenericEvent<UserEventType, UserEventData> event) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Simulated failure in " + serviceName)
            );
        }
    }
}
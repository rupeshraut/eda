package eda.eventbus.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for event bus metrics collection
 */
public interface EventBusMetrics {
    
    /**
     * Record event published
     */
    void recordEventPublished(String eventType, String source);
    
    /**
     * Record event processed successfully
     */
    void recordEventProcessed(String eventType, String subscriberId, Duration processingTime);
    
    /**
     * Record event processing failure
     */
    void recordEventFailed(String eventType, String subscriberId, String errorType, Duration processingTime);
    
    /**
     * Record event sent to dead letter queue
     */
    void recordDeadLetter(String eventType, String subscriberId, String reason);
    
    /**
     * Record subscription created
     */
    void recordSubscriptionCreated(String eventType, String subscriberId);
    
    /**
     * Record subscription removed
     */
    void recordSubscriptionRemoved(String eventType, String subscriberId);
    
    /**
     * Record retry attempt
     */
    void recordRetryAttempt(String eventType, String subscriberId, int attemptNumber);
    
    /**
     * Record timeout
     */
    void recordTimeout(String eventType, String subscriberId);
    
    /**
     * Get metrics snapshot
     */
    CompletableFuture<EventBusMetricsSnapshot> getMetricsSnapshot();
    
    /**
     * Get metrics for specific event type
     */
    CompletableFuture<EventTypeMetrics> getEventTypeMetrics(String eventType);
    
    /**
     * Get metrics for specific subscriber
     */
    CompletableFuture<SubscriberMetrics> getSubscriberMetrics(String subscriberId);
    
    /**
     * Reset all metrics
     */
    void reset();
    
    /**
     * Export metrics in specific format
     */
    CompletableFuture<String> exportMetrics(MetricsFormat format);
}
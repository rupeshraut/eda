package eda.eventbus.subscription;

import eda.eventbus.core.GenericEvent;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an active subscription to events
 */
public class EventSubscription<T, D> {
    private final UUID subscriptionId;
    private final T eventType;
    private final EventConsumer<T, D> consumer;
    private final SubscriptionOptions options;
    private final Instant createdAt;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);
    private volatile Instant lastProcessedAt;
    
    public EventSubscription(T eventType, EventConsumer<T, D> consumer, SubscriptionOptions options) {
        this.subscriptionId = UUID.randomUUID();
        this.eventType = eventType;
        this.consumer = consumer;
        this.options = options;
        this.createdAt = Instant.now();
    }
    
    /**
     * Process an event if the subscription is active and matches filters
     */
    public CompletableFuture<Void> processEvent(GenericEvent<T, D> event) {
        if (!active.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Subscription is not active: " + subscriptionId));
        }
        
        if (!options.shouldProcess(event)) {
            return CompletableFuture.completedFuture(null);
        }
        
        return consumer.handle(event)
            .whenComplete((result, throwable) -> {
                lastProcessedAt = Instant.now();
                if (throwable == null) {
                    processedCount.incrementAndGet();
                } else {
                    failedCount.incrementAndGet();
                }
            });
    }
    
    /**
     * Unsubscribe and deactivate this subscription
     */
    public void unsubscribe() {
        active.set(false);
    }
    
    /**
     * Check if subscription is active
     */
    public boolean isActive() {
        return active.get();
    }
    
    // Getters
    public UUID getSubscriptionId() { return subscriptionId; }
    public T getEventType() { return eventType; }
    public EventConsumer<T, D> getConsumer() { return consumer; }
    public SubscriptionOptions getOptions() { return options; }
    public Instant getCreatedAt() { return createdAt; }
    public long getProcessedCount() { return processedCount.get(); }
    public long getFailedCount() { return failedCount.get(); }
    public Instant getLastProcessedAt() { return lastProcessedAt; }
    
    /**
     * Get subscription statistics
     */
    public SubscriptionStats getStats() {
        return new SubscriptionStats(
            subscriptionId,
            processedCount.get(),
            failedCount.get(),
            createdAt,
            lastProcessedAt,
            active.get()
        );
    }
    
    @Override
    public String toString() {
        return "EventSubscription{" +
                "subscriptionId=" + subscriptionId +
                ", eventType=" + eventType +
                ", subscriberId='" + options.getSubscriberId() + '\'' +
                ", active=" + active.get() +
                ", processed=" + processedCount.get() +
                ", failed=" + failedCount.get() +
                '}';
    }
    
    /**
     * Statistics for a subscription
     */
    public static class SubscriptionStats {
        private final UUID subscriptionId;
        private final long processedCount;
        private final long failedCount;
        private final Instant createdAt;
        private final Instant lastProcessedAt;
        private final boolean active;
        
        public SubscriptionStats(UUID subscriptionId, long processedCount, long failedCount, 
                               Instant createdAt, Instant lastProcessedAt, boolean active) {
            this.subscriptionId = subscriptionId;
            this.processedCount = processedCount;
            this.failedCount = failedCount;
            this.createdAt = createdAt;
            this.lastProcessedAt = lastProcessedAt;
            this.active = active;
        }
        
        // Getters
        public UUID getSubscriptionId() { return subscriptionId; }
        public long getProcessedCount() { return processedCount; }
        public long getFailedCount() { return failedCount; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastProcessedAt() { return lastProcessedAt; }
        public boolean isActive() { return active; }
        
        public double getSuccessRate() {
            long total = processedCount + failedCount;
            return total == 0 ? 0.0 : (double) processedCount / total;
        }
    }
}
package eda.eventbus.subscription;

import eda.eventbus.core.EventPriority;
import eda.eventbus.core.GenericEvent;
import eda.eventbus.retry.RetryConfig;
import java.time.Duration;
import java.util.function.Predicate;

/**
 * Configuration options for event subscriptions
 */
public class SubscriptionOptions {
    private final String subscriberId;
    private final Predicate<GenericEvent<?, ?>> filter;
    private final EventPriority priority;
    private final boolean ordered;
    private final Duration timeout;
    private final RetryConfig retryConfig;
    private final boolean deadLetterEnabled;
    private final int batchSize;
    private final Duration batchTimeout;
    
    private SubscriptionOptions(Builder builder) {
        this.subscriberId = builder.subscriberId;
        this.filter = builder.filter;
        this.priority = builder.priority;
        this.ordered = builder.ordered;
        this.timeout = builder.timeout;
        this.retryConfig = builder.retryConfig;
        this.deadLetterEnabled = builder.deadLetterEnabled;
        this.batchSize = builder.batchSize;
        this.batchTimeout = builder.batchTimeout;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static SubscriptionOptions defaultOptions() {
        return builder().build();
    }
    
    // Getters
    public String getSubscriberId() { return subscriberId; }
    public Predicate<GenericEvent<?, ?>> getFilter() { return filter; }
    public EventPriority getPriority() { return priority; }
    public boolean isOrdered() { return ordered; }
    public Duration getTimeout() { return timeout; }
    public RetryConfig getRetryConfig() { return retryConfig; }
    public boolean isDeadLetterEnabled() { return deadLetterEnabled; }
    public int getBatchSize() { return batchSize; }
    public Duration getBatchTimeout() { return batchTimeout; }
    
    public boolean shouldProcess(GenericEvent<?, ?> event) {
        return filter == null || filter.test(event);
    }
    
    public static class Builder {
        private String subscriberId = "default-subscriber";
        private Predicate<GenericEvent<?, ?>> filter;
        private EventPriority priority = EventPriority.NORMAL;
        private boolean ordered = false;
        private Duration timeout = Duration.ofSeconds(30);
        private RetryConfig retryConfig = RetryConfig.defaultConfig();
        private boolean deadLetterEnabled = true;
        private int batchSize = 1;
        private Duration batchTimeout = Duration.ofSeconds(1);
        
        public Builder subscriberId(String subscriberId) {
            this.subscriberId = subscriberId;
            return this;
        }
        
        public Builder filter(Predicate<GenericEvent<?, ?>> filter) {
            this.filter = filter;
            return this;
        }
        
        public Builder priority(EventPriority priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder ordered(boolean ordered) {
            this.ordered = ordered;
            return this;
        }
        
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder retryConfig(RetryConfig retryConfig) {
            this.retryConfig = retryConfig;
            return this;
        }
        
        public Builder deadLetterEnabled(boolean deadLetterEnabled) {
            this.deadLetterEnabled = deadLetterEnabled;
            return this;
        }
        
        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }
        
        public Builder batchTimeout(Duration batchTimeout) {
            this.batchTimeout = batchTimeout;
            return this;
        }
        
        public SubscriptionOptions build() {
            return new SubscriptionOptions(this);
        }
    }
}
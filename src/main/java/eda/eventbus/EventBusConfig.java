package eda.eventbus;

import eda.eventbus.retry.RetryConfig;
import java.time.Duration;

/**
 * Configuration for the Generic Event Bus
 */
public class EventBusConfig {
    private final boolean useVirtualThreads;
    private final int scheduledThreadPoolSize;
    private final RetryConfig defaultRetryConfig;
    private final Duration cleanupInterval;
    private final Duration defaultTimeout;
    private final boolean enableMetrics;
    
    private EventBusConfig(Builder builder) {
        this.useVirtualThreads = builder.useVirtualThreads;
        this.scheduledThreadPoolSize = builder.scheduledThreadPoolSize;
        this.defaultRetryConfig = builder.defaultRetryConfig;
        this.cleanupInterval = builder.cleanupInterval;
        this.defaultTimeout = builder.defaultTimeout;
        this.enableMetrics = builder.enableMetrics;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static EventBusConfig defaultConfig() {
        return builder().build();
    }
    
    // Getters
    public boolean isUseVirtualThreads() { return useVirtualThreads; }
    public int getScheduledThreadPoolSize() { return scheduledThreadPoolSize; }
    public RetryConfig getDefaultRetryConfig() { return defaultRetryConfig; }
    public Duration getCleanupInterval() { return cleanupInterval; }
    public Duration getDefaultTimeout() { return defaultTimeout; }
    public boolean isEnableMetrics() { return enableMetrics; }
    
    public static class Builder {
        private boolean useVirtualThreads = true;
        private int scheduledThreadPoolSize = 10;
        private RetryConfig defaultRetryConfig = RetryConfig.defaultConfig();
        private Duration cleanupInterval = Duration.ofMinutes(10);
        private Duration defaultTimeout = Duration.ofSeconds(30);
        private boolean enableMetrics = true;
        
        public Builder useVirtualThreads(boolean useVirtualThreads) {
            this.useVirtualThreads = useVirtualThreads;
            return this;
        }
        
        public Builder scheduledThreadPoolSize(int scheduledThreadPoolSize) {
            this.scheduledThreadPoolSize = scheduledThreadPoolSize;
            return this;
        }
        
        public Builder defaultRetryConfig(RetryConfig defaultRetryConfig) {
            this.defaultRetryConfig = defaultRetryConfig;
            return this;
        }
        
        public Builder cleanupInterval(Duration cleanupInterval) {
            this.cleanupInterval = cleanupInterval;
            return this;
        }
        
        public Builder defaultTimeout(Duration defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
            return this;
        }
        
        public Builder enableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
            return this;
        }
        
        public EventBusConfig build() {
            return new EventBusConfig(this);
        }
    }
}
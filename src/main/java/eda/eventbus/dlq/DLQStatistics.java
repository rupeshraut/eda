package eda.eventbus.dlq;

import java.time.Instant;
import java.util.Map;

/**
 * Statistics for Dead Letter Queue
 */
public class DLQStatistics {
    private final long totalEvents;
    private final long failedEvents;
    private final long retryingEvents;
    private final long exhaustedEvents;
    private final long resolvedEvents;
    private final long quarantinedEvents;
    private final Map<String, Long> eventsByType;
    private final Map<String, Long> errorsByType;
    private final double averageRetryCount;
    private final long oldestEventAgeMillis;
    private final Instant timestamp;
    private final DLQHealth health;
    
    private DLQStatistics(Builder builder) {
        this.totalEvents = builder.totalEvents;
        this.failedEvents = builder.failedEvents;
        this.retryingEvents = builder.retryingEvents;
        this.exhaustedEvents = builder.exhaustedEvents;
        this.resolvedEvents = builder.resolvedEvents;
        this.quarantinedEvents = builder.quarantinedEvents;
        this.eventsByType = Map.copyOf(builder.eventsByType);
        this.errorsByType = Map.copyOf(builder.errorsByType);
        this.averageRetryCount = builder.averageRetryCount;
        this.oldestEventAgeMillis = builder.oldestEventAgeMillis;
        this.timestamp = builder.timestamp;
        this.health = builder.health;
    }
    
    public long getTotalEvents() { return totalEvents; }
    public long getFailedEvents() { return failedEvents; }
    public long getRetryingEvents() { return retryingEvents; }
    public long getExhaustedEvents() { return exhaustedEvents; }
    public long getResolvedEvents() { return resolvedEvents; }
    public long getQuarantinedEvents() { return quarantinedEvents; }
    public Map<String, Long> getEventsByType() { return eventsByType; }
    public Map<String, Long> getErrorsByType() { return errorsByType; }
    public double getAverageRetryCount() { return averageRetryCount; }
    public long getOldestEventAgeMillis() { return oldestEventAgeMillis; }
    public Instant getTimestamp() { return timestamp; }
    public DLQHealth getHealth() { return health; }
    
    /**
     * Get active events (failed + retrying)
     */
    public long getActiveEvents() {
        return failedEvents + retryingEvents;
    }
    
    /**
     * Get permanent failures (exhausted + quarantined)
     */
    public long getPermanentFailures() {
        return exhaustedEvents + quarantinedEvents;
    }
    
    /**
     * Get success rate for resolved events
     */
    public double getSuccessRate() {
        if (totalEvents == 0) return 1.0;
        return (double) resolvedEvents / totalEvents;
    }
    
    /**
     * Get failure rate for permanent failures
     */
    public double getFailureRate() {
        if (totalEvents == 0) return 0.0;
        return (double) getPermanentFailures() / totalEvents;
    }
    
    /**
     * Check if DLQ is healthy
     */
    public boolean isHealthy() {
        return health == DLQHealth.HEALTHY;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private long totalEvents = 0;
        private long failedEvents = 0;
        private long retryingEvents = 0;
        private long exhaustedEvents = 0;
        private long resolvedEvents = 0;
        private long quarantinedEvents = 0;
        private Map<String, Long> eventsByType = new java.util.HashMap<>();
        private Map<String, Long> errorsByType = new java.util.HashMap<>();
        private double averageRetryCount = 0.0;
        private long oldestEventAgeMillis = 0;
        private Instant timestamp = Instant.now();
        private DLQHealth health = DLQHealth.HEALTHY;
        
        public Builder totalEvents(long totalEvents) {
            this.totalEvents = totalEvents;
            return this;
        }
        
        public Builder failedEvents(long failedEvents) {
            this.failedEvents = failedEvents;
            return this;
        }
        
        public Builder retryingEvents(long retryingEvents) {
            this.retryingEvents = retryingEvents;
            return this;
        }
        
        public Builder exhaustedEvents(long exhaustedEvents) {
            this.exhaustedEvents = exhaustedEvents;
            return this;
        }
        
        public Builder resolvedEvents(long resolvedEvents) {
            this.resolvedEvents = resolvedEvents;
            return this;
        }
        
        public Builder quarantinedEvents(long quarantinedEvents) {
            this.quarantinedEvents = quarantinedEvents;
            return this;
        }
        
        public Builder eventsByType(Map<String, Long> eventsByType) {
            this.eventsByType = new java.util.HashMap<>(eventsByType);
            return this;
        }
        
        public Builder errorsByType(Map<String, Long> errorsByType) {
            this.errorsByType = new java.util.HashMap<>(errorsByType);
            return this;
        }
        
        public Builder averageRetryCount(double averageRetryCount) {
            this.averageRetryCount = averageRetryCount;
            return this;
        }
        
        public Builder oldestEventAgeMillis(long oldestEventAgeMillis) {
            this.oldestEventAgeMillis = oldestEventAgeMillis;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder health(DLQHealth health) {
            this.health = health;
            return this;
        }
        
        public DLQStatistics build() {
            return new DLQStatistics(this);
        }
    }
    
    /**
     * DLQ Health status
     */
    public enum DLQHealth {
        HEALTHY("DLQ is operating normally"),
        WARNING("DLQ has some issues but is functional"),
        CRITICAL("DLQ has serious issues requiring attention"),
        FAILED("DLQ is not functioning properly");
        
        private final String description;
        
        DLQHealth(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "DLQStatistics{total=%d, active=%d, resolved=%d, failed=%d, health=%s, successRate=%.2f%%}",
            totalEvents, getActiveEvents(), resolvedEvents, getPermanentFailures(), health, getSuccessRate() * 100
        );
    }
}
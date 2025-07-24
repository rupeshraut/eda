package eda.eventbus.dlq;

import eda.eventbus.core.GenericEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents an event that has been moved to the Dead Letter Queue
 */
public class DeadLetterEvent<T> {
    private final String id;
    private final GenericEvent<T, ?> originalEvent;
    private final Instant firstFailureTime;
    private final Instant lastFailureTime;
    private final int retryCount;
    private final int maxRetries;
    private final List<FailureReason> failureHistory;
    private final DLQStatus status;
    private final String eventType;
    private final String eventSource;
    private final Map<String, Object> metadata;
    private final RetryPolicy retryPolicy;
    
    private DeadLetterEvent(Builder<T> builder) {
        this.id = builder.id;
        this.originalEvent = builder.originalEvent;
        this.firstFailureTime = builder.firstFailureTime;
        this.lastFailureTime = builder.lastFailureTime;
        this.retryCount = builder.retryCount;
        this.maxRetries = builder.maxRetries;
        this.failureHistory = List.copyOf(builder.failureHistory);
        this.status = builder.status;
        this.eventType = builder.eventType;
        this.eventSource = builder.eventSource;
        this.metadata = Map.copyOf(builder.metadata);
        this.retryPolicy = builder.retryPolicy;
    }
    
    public String getId() { return id; }
    public GenericEvent<T, ?> getOriginalEvent() { return originalEvent; }
    public Instant getFirstFailureTime() { return firstFailureTime; }
    public Instant getLastFailureTime() { return lastFailureTime; }
    public int getRetryCount() { return retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public List<FailureReason> getFailureHistory() { return failureHistory; }
    public DLQStatus getStatus() { return status; }
    public String getEventType() { return eventType; }
    public String getEventSource() { return eventSource; }
    public Map<String, Object> getMetadata() { return metadata; }
    public RetryPolicy getRetryPolicy() { return retryPolicy; }
    
    /**
     * Check if event has exceeded maximum retry attempts
     */
    public boolean hasExceededMaxRetries() {
        return retryCount >= maxRetries;
    }
    
    /**
     * Check if event is eligible for retry
     */
    public boolean isEligibleForRetry() {
        return status == DLQStatus.FAILED && !hasExceededMaxRetries();
    }
    
    /**
     * Get the most recent failure reason
     */
    public FailureReason getLastFailureReason() {
        return failureHistory.isEmpty() ? null : failureHistory.get(failureHistory.size() - 1);
    }
    
    /**
     * Get total time spent in DLQ
     */
    public long getTimeInDLQMillis() {
        return Instant.now().toEpochMilli() - firstFailureTime.toEpochMilli();
    }
    
    /**
     * Create a new DeadLetterEvent with updated retry information
     */
    public DeadLetterEvent<T> withRetryAttempt(FailureReason newFailure) {
        return DeadLetterEvent.<T>builder()
            .id(this.id)
            .originalEvent(this.originalEvent)
            .firstFailureTime(this.firstFailureTime)
            .lastFailureTime(Instant.now())
            .retryCount(this.retryCount + 1)
            .maxRetries(this.maxRetries)
            .failureHistory(this.failureHistory)
            .addFailureReason(newFailure)
            .status(this.retryCount + 1 >= this.maxRetries ? DLQStatus.EXHAUSTED : DLQStatus.FAILED)
            .eventType(this.eventType)
            .eventSource(this.eventSource)
            .metadata(this.metadata)
            .retryPolicy(this.retryPolicy)
            .build();
    }
    
    /**
     * Create a copy with updated status
     */
    public DeadLetterEvent<T> withStatus(DLQStatus newStatus) {
        return DeadLetterEvent.<T>builder()
            .id(this.id)
            .originalEvent(this.originalEvent)
            .firstFailureTime(this.firstFailureTime)
            .lastFailureTime(this.lastFailureTime)
            .retryCount(this.retryCount)
            .maxRetries(this.maxRetries)
            .failureHistory(this.failureHistory)
            .status(newStatus)
            .eventType(this.eventType)
            .eventSource(this.eventSource)
            .metadata(this.metadata)
            .retryPolicy(this.retryPolicy)
            .build();
    }
    
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    
    public static class Builder<T> {
        private String id;
        private GenericEvent<T, ?> originalEvent;
        private Instant firstFailureTime;
        private Instant lastFailureTime;
        private int retryCount = 0;
        private int maxRetries = 3;
        private List<FailureReason> failureHistory = new java.util.ArrayList<>();
        private DLQStatus status = DLQStatus.FAILED;
        private String eventType;
        private String eventSource;
        private Map<String, Object> metadata = new java.util.HashMap<>();
        private RetryPolicy retryPolicy;
        
        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder<T> originalEvent(GenericEvent<T, ?> originalEvent) {
            this.originalEvent = originalEvent;
            return this;
        }
        
        public Builder<T> firstFailureTime(Instant firstFailureTime) {
            this.firstFailureTime = firstFailureTime;
            return this;
        }
        
        public Builder<T> lastFailureTime(Instant lastFailureTime) {
            this.lastFailureTime = lastFailureTime;
            return this;
        }
        
        public Builder<T> retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }
        
        public Builder<T> maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder<T> failureHistory(List<FailureReason> failureHistory) {
            this.failureHistory = new java.util.ArrayList<>(failureHistory);
            return this;
        }
        
        public Builder<T> addFailureReason(FailureReason failureReason) {
            this.failureHistory.add(failureReason);
            return this;
        }
        
        public Builder<T> status(DLQStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder<T> eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public Builder<T> eventSource(String eventSource) {
            this.eventSource = eventSource;
            return this;
        }
        
        public Builder<T> metadata(Map<String, Object> metadata) {
            this.metadata = new java.util.HashMap<>(metadata);
            return this;
        }
        
        public Builder<T> addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder<T> retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }
        
        public DeadLetterEvent<T> build() {
            if (id == null) {
                id = java.util.UUID.randomUUID().toString();
            }
            if (firstFailureTime == null) {
                firstFailureTime = Instant.now();
            }
            if (lastFailureTime == null) {
                lastFailureTime = firstFailureTime;
            }
            if (eventType == null && originalEvent != null) {
                eventType = originalEvent.getEventType().toString();
            }
            
            return new DeadLetterEvent<>(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "DeadLetterEvent{id='%s', eventType='%s', retryCount=%d/%d, status=%s, firstFailure=%s}",
            id, eventType, retryCount, maxRetries, status, firstFailureTime
        );
    }
}
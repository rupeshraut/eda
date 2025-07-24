package eda.eventbus.dlq;

import eda.eventbus.core.GenericEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Dead Letter Queue interface for handling failed events
 */
public interface DeadLetterQueue<T> {
    
    /**
     * Store a failed event in the DLQ
     */
    CompletableFuture<Void> store(DeadLetterEvent<T> deadLetterEvent);
    
    /**
     * Retrieve events from DLQ with optional filtering
     */
    CompletableFuture<List<DeadLetterEvent<T>>> retrieve(DLQFilter filter);
    
    /**
     * Retrieve all events from DLQ
     */
    CompletableFuture<List<DeadLetterEvent<T>>> retrieveAll();
    
    /**
     * Retry a specific event from DLQ
     */
    CompletableFuture<RetryResult> retry(String eventId);
    
    /**
     * Retry multiple events from DLQ
     */
    CompletableFuture<List<RetryResult>> retryBatch(List<String> eventIds);
    
    /**
     * Remove successfully processed events from DLQ
     */
    CompletableFuture<Void> remove(String eventId);
    
    /**
     * Purge old events from DLQ based on retention policy
     */
    CompletableFuture<Integer> purge(Duration retentionPeriod);
    
    /**
     * Get DLQ statistics
     */
    CompletableFuture<DLQStatistics> getStatistics();
    
    /**
     * Subscribe to DLQ events for monitoring
     */
    void subscribe(Consumer<DLQEvent> eventConsumer);
    
    /**
     * Enable automatic retry with specified policy
     */
    void enableAutoRetry(RetryPolicy retryPolicy);
    
    /**
     * Disable automatic retry
     */
    void disableAutoRetry();
    
    /**
     * Shutdown the DLQ
     */
    void shutdown();
    
    /**
     * DLQ filter for querying events
     */
    class DLQFilter {
        private final Instant fromTime;
        private final Instant toTime;
        private final String eventType;
        private final String errorType;
        private final Integer maxRetries;
        private final DLQStatus status;
        private final int limit;
        
        private DLQFilter(Builder builder) {
            this.fromTime = builder.fromTime;
            this.toTime = builder.toTime;
            this.eventType = builder.eventType;
            this.errorType = builder.errorType;
            this.maxRetries = builder.maxRetries;
            this.status = builder.status;
            this.limit = builder.limit;
        }
        
        public Instant getFromTime() { return fromTime; }
        public Instant getToTime() { return toTime; }
        public String getEventType() { return eventType; }
        public String getErrorType() { return errorType; }
        public Integer getMaxRetries() { return maxRetries; }
        public DLQStatus getStatus() { return status; }
        public int getLimit() { return limit; }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private Instant fromTime;
            private Instant toTime;
            private String eventType;
            private String errorType;
            private Integer maxRetries;
            private DLQStatus status;
            private int limit = 100;
            
            public Builder fromTime(Instant fromTime) {
                this.fromTime = fromTime;
                return this;
            }
            
            public Builder toTime(Instant toTime) {
                this.toTime = toTime;
                return this;
            }
            
            public Builder eventType(String eventType) {
                this.eventType = eventType;
                return this;
            }
            
            public Builder errorType(String errorType) {
                this.errorType = errorType;
                return this;
            }
            
            public Builder maxRetries(Integer maxRetries) {
                this.maxRetries = maxRetries;
                return this;
            }
            
            public Builder status(DLQStatus status) {
                this.status = status;
                return this;
            }
            
            public Builder limit(int limit) {
                this.limit = limit;
                return this;
            }
            
            public DLQFilter build() {
                return new DLQFilter(this);
            }
        }
    }
    
    /**
     * DLQ event types for monitoring
     */
    enum DLQEventType {
        EVENT_STORED,
        EVENT_RETRIED,
        EVENT_REMOVED,
        RETRY_FAILED,
        AUTO_RETRY_TRIGGERED,
        PURGE_COMPLETED
    }
    
    /**
     * DLQ event for monitoring and notifications
     */
    class DLQEvent {
        private final DLQEventType type;
        private final String eventId;
        private final Instant timestamp;
        private final String message;
        private final Exception exception;
        
        public DLQEvent(DLQEventType type, String eventId, String message) {
            this(type, eventId, message, null);
        }
        
        public DLQEvent(DLQEventType type, String eventId, String message, Exception exception) {
            this.type = type;
            this.eventId = eventId;
            this.message = message;
            this.exception = exception;
            this.timestamp = Instant.now();
        }
        
        public DLQEventType getType() { return type; }
        public String getEventId() { return eventId; }
        public Instant getTimestamp() { return timestamp; }
        public String getMessage() { return message; }
        public Exception getException() { return exception; }
        
        @Override
        public String toString() {
            return String.format("DLQEvent{type=%s, eventId='%s', message='%s', timestamp=%s}", 
                               type, eventId, message, timestamp);
        }
    }
}
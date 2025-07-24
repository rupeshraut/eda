package eda.eventbus.dlq;

import java.time.Instant;

/**
 * Result of a retry operation
 */
public class RetryResult {
    private final String eventId;
    private final boolean success;
    private final String message;
    private final Exception exception;
    private final Instant timestamp;
    private final int attemptNumber;
    private final DLQStatus newStatus;
    
    private RetryResult(Builder builder) {
        this.eventId = builder.eventId;
        this.success = builder.success;
        this.message = builder.message;
        this.exception = builder.exception;
        this.timestamp = builder.timestamp;
        this.attemptNumber = builder.attemptNumber;
        this.newStatus = builder.newStatus;
    }
    
    public String getEventId() { return eventId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Exception getException() { return exception; }
    public Instant getTimestamp() { return timestamp; }
    public int getAttemptNumber() { return attemptNumber; }
    public DLQStatus getNewStatus() { return newStatus; }
    
    public boolean isFailure() { return !success; }
    
    /**
     * Create successful retry result
     */
    public static RetryResult success(String eventId, int attemptNumber) {
        return builder()
            .eventId(eventId)
            .success(true)
            .message("Retry successful")
            .attemptNumber(attemptNumber)
            .newStatus(DLQStatus.RESOLVED)
            .build();
    }
    
    /**
     * Create failed retry result
     */
    public static RetryResult failure(String eventId, int attemptNumber, Exception exception) {
        return builder()
            .eventId(eventId)
            .success(false)
            .message("Retry failed: " + exception.getMessage())
            .exception(exception)
            .attemptNumber(attemptNumber)
            .newStatus(DLQStatus.FAILED)
            .build();
    }
    
    /**
     * Create retry result for exhausted attempts
     */
    public static RetryResult exhausted(String eventId, int attemptNumber) {
        return builder()
            .eventId(eventId)
            .success(false)
            .message("Maximum retry attempts exhausted")
            .attemptNumber(attemptNumber)
            .newStatus(DLQStatus.EXHAUSTED)
            .build();
    }
    
    /**
     * Create retry result for poison message
     */
    public static RetryResult poisonMessage(String eventId, int attemptNumber, String reason) {
        return builder()
            .eventId(eventId)
            .success(false)
            .message("Poison message detected: " + reason)
            .attemptNumber(attemptNumber)
            .newStatus(DLQStatus.QUARANTINED)
            .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String eventId;
        private boolean success;
        private String message;
        private Exception exception;
        private Instant timestamp = Instant.now();
        private int attemptNumber;
        private DLQStatus newStatus;
        
        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder exception(Exception exception) {
            this.exception = exception;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder attemptNumber(int attemptNumber) {
            this.attemptNumber = attemptNumber;
            return this;
        }
        
        public Builder newStatus(DLQStatus newStatus) {
            this.newStatus = newStatus;
            return this;
        }
        
        public RetryResult build() {
            return new RetryResult(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "RetryResult{eventId='%s', success=%s, attempt=%d, status=%s, message='%s'}",
            eventId, success, attemptNumber, newStatus, message
        );
    }
}
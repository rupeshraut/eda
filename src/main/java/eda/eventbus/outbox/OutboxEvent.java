package eda.eventbus.outbox;

import eda.eventbus.core.GenericEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents an event stored in the outbox for reliable publishing
 */
public class OutboxEvent {
    private final UUID outboxId;
    private final GenericEvent<?, ?> event;
    private final OutboxStatus status;
    private final Instant createdAt;
    private final Instant scheduledAt;
    private final int retryCount;
    private final String lastError;
    private final Instant lastAttemptAt;
    
    private OutboxEvent(Builder builder) {
        this.outboxId = builder.outboxId;
        this.event = builder.event;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.scheduledAt = builder.scheduledAt;
        this.retryCount = builder.retryCount;
        this.lastError = builder.lastError;
        this.lastAttemptAt = builder.lastAttemptAt;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
            .outboxId(outboxId)
            .event(event)
            .status(status)
            .createdAt(createdAt)
            .scheduledAt(scheduledAt)
            .retryCount(retryCount)
            .lastError(lastError)
            .lastAttemptAt(lastAttemptAt);
    }
    
    // Getters
    public UUID getOutboxId() { return outboxId; }
    public GenericEvent<?, ?> getEvent() { return event; }
    public OutboxStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getScheduledAt() { return scheduledAt; }
    public int getRetryCount() { return retryCount; }
    public String getLastError() { return lastError; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    
    public static class Builder {
        private UUID outboxId = UUID.randomUUID();
        private GenericEvent<?, ?> event;
        private OutboxStatus status = OutboxStatus.PENDING;
        private Instant createdAt = Instant.now();
        private Instant scheduledAt = Instant.now();
        private int retryCount = 0;
        private String lastError;
        private Instant lastAttemptAt;
        
        public Builder outboxId(UUID outboxId) {
            this.outboxId = outboxId;
            return this;
        }
        
        public Builder event(GenericEvent<?, ?> event) {
            this.event = event;
            return this;
        }
        
        public Builder status(OutboxStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder scheduledAt(Instant scheduledAt) {
            this.scheduledAt = scheduledAt;
            return this;
        }
        
        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }
        
        public Builder lastError(String lastError) {
            this.lastError = lastError;
            return this;
        }
        
        public Builder lastAttemptAt(Instant lastAttemptAt) {
            this.lastAttemptAt = lastAttemptAt;
            return this;
        }
        
        public OutboxEvent build() {
            return new OutboxEvent(this);
        }
    }
    
    @Override
    public String toString() {
        return "OutboxEvent{" +
                "outboxId=" + outboxId +
                ", eventId=" + event.getEventId() +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", scheduledAt=" + scheduledAt +
                '}';
    }
}
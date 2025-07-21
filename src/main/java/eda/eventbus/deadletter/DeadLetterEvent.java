package eda.eventbus.deadletter;

import eda.eventbus.core.GenericEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents an event that failed processing and is sent to dead letter queue
 */
public class DeadLetterEvent<T, D> {
    private final UUID deadLetterId;
    private final GenericEvent<T, D> originalEvent;
    private final String failureReason;
    private final Throwable cause;
    private final String subscriberId;
    private final int failedAttempts;
    private final Instant deadLetterTimestamp;
    private final DeadLetterReason reason;
    
    public DeadLetterEvent(GenericEvent<T, D> originalEvent, 
                          String failureReason, 
                          Throwable cause,
                          String subscriberId,
                          int failedAttempts,
                          DeadLetterReason reason) {
        this.deadLetterId = UUID.randomUUID();
        this.originalEvent = originalEvent;
        this.failureReason = failureReason;
        this.cause = cause;
        this.subscriberId = subscriberId;
        this.failedAttempts = failedAttempts;
        this.deadLetterTimestamp = Instant.now();
        this.reason = reason;
    }
    
    // Getters
    public UUID getDeadLetterId() { return deadLetterId; }
    public GenericEvent<T, D> getOriginalEvent() { return originalEvent; }
    public String getFailureReason() { return failureReason; }
    public Throwable getCause() { return cause; }
    public String getSubscriberId() { return subscriberId; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getDeadLetterTimestamp() { return deadLetterTimestamp; }
    public DeadLetterReason getReason() { return reason; }
    
    @Override
    public String toString() {
        return "DeadLetterEvent{" +
                "deadLetterId=" + deadLetterId +
                ", originalEventId=" + originalEvent.getEventId() +
                ", reason=" + reason +
                ", failedAttempts=" + failedAttempts +
                ", subscriberId='" + subscriberId + '\'' +
                ", timestamp=" + deadLetterTimestamp +
                '}';
    }
}
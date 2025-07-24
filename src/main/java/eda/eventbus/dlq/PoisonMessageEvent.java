package eda.eventbus.dlq;

import eda.eventbus.core.GenericEvent;

import java.time.Instant;

/**
 * Event fired when a poison message is detected
 */
public class PoisonMessageEvent<T> {
    private final GenericEvent<T, ?> originalEvent;
    private final Exception lastException;
    private final PoisonMessageHandler.MessageFailureTracker failureTracker;
    private final PoisonMessageAction actionTaken;
    private final Instant timestamp;
    
    public PoisonMessageEvent(GenericEvent<T, ?> originalEvent, Exception lastException, 
                             PoisonMessageHandler.MessageFailureTracker failureTracker, 
                             PoisonMessageAction actionTaken) {
        this.originalEvent = originalEvent;
        this.lastException = lastException;
        this.failureTracker = failureTracker;
        this.actionTaken = actionTaken;
        this.timestamp = Instant.now();
    }
    
    public GenericEvent<T, ?> getOriginalEvent() { return originalEvent; }
    public Exception getLastException() { return lastException; }
    public PoisonMessageHandler.MessageFailureTracker getFailureTracker() { return failureTracker; }
    public PoisonMessageAction getActionTaken() { return actionTaken; }
    public Instant getTimestamp() { return timestamp; }
    
    /**
     * Get event type
     */
    public String getEventType() {
        return originalEvent.getEventType().toString();
    }
    
    /**
     * Get event ID
     */
    public String getEventId() {
        return originalEvent.getEventId().toString();
    }
    
    /**
     * Get total failure count
     */
    public int getTotalFailures() {
        return failureTracker.getTotalFailures();
    }
    
    /**
     * Get consecutive same error count
     */
    public int getConsecutiveSameErrors() {
        return failureTracker.getConsecutiveSameErrors();
    }
    
    /**
     * Get last failure time
     */
    public Instant getLastFailureTime() {
        return failureTracker.getLastFailureTime();
    }
    
    @Override
    public String toString() {
        return String.format(
            "PoisonMessageEvent{eventType='%s', eventId='%s', failures=%d, action=%s, lastError='%s'}",
            getEventType(), getEventId(), getTotalFailures(), actionTaken, 
            lastException.getClass().getSimpleName()
        );
    }
}
package eda.eventbus.outbox;

/**
 * Status of events in the outbox
 */
public enum OutboxStatus {
    /**
     * Event is waiting to be published
     */
    PENDING,
    
    /**
     * Event is currently being processed
     */
    PROCESSING,
    
    /**
     * Event was published successfully
     */
    PUBLISHED,
    
    /**
     * Event failed to publish and will be retried
     */
    FAILED,
    
    /**
     * Event failed permanently and won't be retried
     */
    DEAD_LETTER,
    
    /**
     * Event was cancelled/skipped
     */
    CANCELLED
}
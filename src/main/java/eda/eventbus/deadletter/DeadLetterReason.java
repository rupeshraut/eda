package eda.eventbus.deadletter;

/**
 * Reasons why an event ended up in the dead letter queue
 */
public enum DeadLetterReason {
    /**
     * Maximum retry attempts exceeded
     */
    RETRY_EXHAUSTED,
    
    /**
     * Processing timeout exceeded
     */
    TIMEOUT,
    
    /**
     * Non-retryable exception occurred
     */
    NON_RETRYABLE_ERROR,
    
    /**
     * Subscriber not found or inactive
     */
    SUBSCRIBER_UNAVAILABLE,
    
    /**
     * Event validation failed
     */
    VALIDATION_ERROR,
    
    /**
     * Poison message - consistently failing event
     */
    POISON_MESSAGE,
    
    /**
     * Manual dead lettering
     */
    MANUAL
}
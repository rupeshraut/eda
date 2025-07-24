package eda.eventbus.dlq;

/**
 * Status of events in Dead Letter Queue
 */
public enum DLQStatus {
    /**
     * Event failed processing and is eligible for retry
     */
    FAILED("Failed - eligible for retry"),
    
    /**
     * Event retry is in progress
     */
    RETRYING("Retry in progress"),
    
    /**
     * Event has exhausted all retry attempts
     */
    EXHAUSTED("Retry attempts exhausted"),
    
    /**
     * Event was successfully reprocessed and removed from DLQ
     */
    RESOLVED("Successfully resolved"),
    
    /**
     * Event was manually marked for removal
     */
    DISCARDED("Manually discarded"),
    
    /**
     * Event is quarantined due to poison message detection
     */
    QUARANTINED("Quarantined - poison message"),
    
    /**
     * Event is waiting for manual intervention
     */
    PENDING_MANUAL("Pending manual intervention"),
    
    /**
     * Event processing was cancelled
     */
    CANCELLED("Processing cancelled");
    
    private final String description;
    
    DLQStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if status indicates event can be retried
     */
    public boolean isRetryable() {
        return this == FAILED;
    }
    
    /**
     * Check if status indicates event is permanently failed
     */
    public boolean isPermanentFailure() {
        return this == EXHAUSTED || this == QUARANTINED || this == DISCARDED;
    }
    
    /**
     * Check if status indicates event is resolved
     */
    public boolean isResolved() {
        return this == RESOLVED;
    }
    
    /**
     * Check if status indicates event is being processed
     */
    public boolean isProcessing() {
        return this == RETRYING;
    }
    
    /**
     * Check if status requires manual intervention
     */
    public boolean requiresManualIntervention() {
        return this == PENDING_MANUAL || this == QUARANTINED;
    }
    
    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}
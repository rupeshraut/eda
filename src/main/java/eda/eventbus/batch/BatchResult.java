package eda.eventbus.batch;

import java.util.List;
import java.util.UUID;

/**
 * Result of batch processing
 */
public class BatchResult {
    private final UUID batchId;
    private final int totalEvents;
    private final int processedEvents;
    private final int failedEvents;
    private final List<BatchError> errors;
    private final boolean success;
    
    private BatchResult(UUID batchId, int totalEvents, int processedEvents, 
                       int failedEvents, List<BatchError> errors) {
        this.batchId = batchId;
        this.totalEvents = totalEvents;
        this.processedEvents = processedEvents;
        this.failedEvents = failedEvents;
        this.errors = List.copyOf(errors);
        this.success = failedEvents == 0;
    }
    
    public static BatchResult success(UUID batchId, int processedEvents) {
        return new BatchResult(batchId, processedEvents, processedEvents, 0, List.of());
    }
    
    public static BatchResult partial(UUID batchId, int totalEvents, int processedEvents, 
                                    int failedEvents, List<BatchError> errors) {
        return new BatchResult(batchId, totalEvents, processedEvents, failedEvents, errors);
    }
    
    public static BatchResult failure(UUID batchId, int totalEvents, List<BatchError> errors) {
        return new BatchResult(batchId, totalEvents, 0, totalEvents, errors);
    }
    
    // Getters
    public UUID getBatchId() { return batchId; }
    public int getTotalEvents() { return totalEvents; }
    public int getProcessedEvents() { return processedEvents; }
    public int getFailedEvents() { return failedEvents; }
    public List<BatchError> getErrors() { return errors; }
    public boolean isSuccess() { return success; }
    public boolean isPartialSuccess() { return processedEvents > 0 && failedEvents > 0; }
    public double getSuccessRate() { return totalEvents > 0 ? (double) processedEvents / totalEvents : 0.0; }
    
    @Override
    public String toString() {
        return "BatchResult{" +
                "batchId=" + batchId +
                ", totalEvents=" + totalEvents +
                ", processedEvents=" + processedEvents +
                ", failedEvents=" + failedEvents +
                ", success=" + success +
                '}';
    }
    
    /**
     * Error information for individual events in a batch
     */
    public static class BatchError {
        private final UUID eventId;
        private final String error;
        private final Throwable cause;
        
        public BatchError(UUID eventId, String error, Throwable cause) {
            this.eventId = eventId;
            this.error = error;
            this.cause = cause;
        }
        
        public UUID getEventId() { return eventId; }
        public String getError() { return error; }
        public Throwable getCause() { return cause; }
        
        @Override
        public String toString() {
            return "BatchError{" +
                    "eventId=" + eventId +
                    ", error='" + error + '\'' +
                    '}';
        }
    }
}
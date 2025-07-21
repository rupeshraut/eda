package eda.eventbus.metrics;

/**
 * Metrics for a specific event type
 */
public class EventTypeMetrics {
    private final String eventType;
    private final long published;
    private final long processed;
    private final long failed;
    private final long deadLettered;
    private final long retries;
    private final double averageProcessingTimeMs;
    private final double successRate;
    
    public EventTypeMetrics(String eventType, long published, long processed, long failed, 
                           long deadLettered, long retries, double averageProcessingTimeMs) {
        this.eventType = eventType;
        this.published = published;
        this.processed = processed;
        this.failed = failed;
        this.deadLettered = deadLettered;
        this.retries = retries;
        this.averageProcessingTimeMs = averageProcessingTimeMs;
        this.successRate = (processed + failed) > 0 ? (double) processed / (processed + failed) : 0.0;
    }
    
    // Getters
    public String getEventType() { return eventType; }
    public long getPublished() { return published; }
    public long getProcessed() { return processed; }
    public long getFailed() { return failed; }
    public long getDeadLettered() { return deadLettered; }
    public long getRetries() { return retries; }
    public double getAverageProcessingTimeMs() { return averageProcessingTimeMs; }
    public double getSuccessRate() { return successRate; }
    
    @Override
    public String toString() {
        return "EventTypeMetrics{" +
                "eventType='" + eventType + '\'' +
                ", published=" + published +
                ", processed=" + processed +
                ", failed=" + failed +
                ", successRate=" + String.format("%.2f%%", successRate * 100) +
                '}';
    }
}
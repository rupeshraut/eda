package eda.eventbus.metrics;

/**
 * Metrics for a specific subscriber
 */
public class SubscriberMetrics {
    private final String subscriberId;
    private final long processed;
    private final long failed;
    private final long deadLettered;
    private final long retries;
    private final long timeouts;
    private final double averageProcessingTimeMs;
    private final double successRate;
    
    public SubscriberMetrics(String subscriberId, long processed, long failed, long deadLettered, 
                           long retries, long timeouts, double averageProcessingTimeMs) {
        this.subscriberId = subscriberId;
        this.processed = processed;
        this.failed = failed;
        this.deadLettered = deadLettered;
        this.retries = retries;
        this.timeouts = timeouts;
        this.averageProcessingTimeMs = averageProcessingTimeMs;
        this.successRate = (processed + failed) > 0 ? (double) processed / (processed + failed) : 0.0;
    }
    
    // Getters
    public String getSubscriberId() { return subscriberId; }
    public long getProcessed() { return processed; }
    public long getFailed() { return failed; }
    public long getDeadLettered() { return deadLettered; }
    public long getRetries() { return retries; }
    public long getTimeouts() { return timeouts; }
    public double getAverageProcessingTimeMs() { return averageProcessingTimeMs; }
    public double getSuccessRate() { return successRate; }
    
    @Override
    public String toString() {
        return "SubscriberMetrics{" +
                "subscriberId='" + subscriberId + '\'' +
                ", processed=" + processed +
                ", failed=" + failed +
                ", successRate=" + String.format("%.2f%%", successRate * 100) +
                ", avgProcessingTime=" + String.format("%.2fms", averageProcessingTimeMs) +
                '}';
    }
}
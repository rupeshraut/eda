package eda.eventbus.dlq;

import java.time.Instant;

/**
 * Statistics for poison message handling
 */
public class PoisonMessageStatistics {
    private final long totalPoisonMessages;
    private final long totalQuarantined;
    private final long totalDiscarded;
    private final long totalMovedToDLQ;
    private final long totalManualIntervention;
    private final int activeTrackers;
    private final Instant timestamp;
    
    private PoisonMessageStatistics(Builder builder) {
        this.totalPoisonMessages = builder.totalPoisonMessages;
        this.totalQuarantined = builder.totalQuarantined;
        this.totalDiscarded = builder.totalDiscarded;
        this.totalMovedToDLQ = builder.totalMovedToDLQ;
        this.totalManualIntervention = builder.totalManualIntervention;
        this.activeTrackers = builder.activeTrackers;
        this.timestamp = builder.timestamp;
    }
    
    public long getTotalPoisonMessages() { return totalPoisonMessages; }
    public long getTotalQuarantined() { return totalQuarantined; }
    public long getTotalDiscarded() { return totalDiscarded; }
    public long getTotalMovedToDLQ() { return totalMovedToDLQ; }
    public long getTotalManualIntervention() { return totalManualIntervention; }
    public int getActiveTrackers() { return activeTrackers; }
    public Instant getTimestamp() { return timestamp; }
    
    /**
     * Get poison message detection rate
     */
    public double getPoisonMessageRate(long totalProcessedMessages) {
        if (totalProcessedMessages == 0) return 0.0;
        return (double) totalPoisonMessages / totalProcessedMessages;
    }
    
    /**
     * Check if poison message handling is healthy
     */
    public boolean isHealthy() {
        // Consider healthy if less than 1% of messages are poison
        // and we're not accumulating too many trackers
        return activeTrackers < 10000 && totalPoisonMessages < 100;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private long totalPoisonMessages = 0;
        private long totalQuarantined = 0;
        private long totalDiscarded = 0;
        private long totalMovedToDLQ = 0;
        private long totalManualIntervention = 0;
        private int activeTrackers = 0;
        private Instant timestamp = Instant.now();
        
        public Builder totalPoisonMessages(long totalPoisonMessages) {
            this.totalPoisonMessages = totalPoisonMessages;
            return this;
        }
        
        public Builder totalQuarantined(long totalQuarantined) {
            this.totalQuarantined = totalQuarantined;
            return this;
        }
        
        public Builder totalDiscarded(long totalDiscarded) {
            this.totalDiscarded = totalDiscarded;
            return this;
        }
        
        public Builder totalMovedToDLQ(long totalMovedToDLQ) {
            this.totalMovedToDLQ = totalMovedToDLQ;
            return this;
        }
        
        public Builder totalManualIntervention(long totalManualIntervention) {
            this.totalManualIntervention = totalManualIntervention;
            return this;
        }
        
        public Builder activeTrackers(int activeTrackers) {
            this.activeTrackers = activeTrackers;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public PoisonMessageStatistics build() {
            return new PoisonMessageStatistics(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "PoisonMessageStatistics{total=%d, quarantined=%d, discarded=%d, dlq=%d, manual=%d, trackers=%d}",
            totalPoisonMessages, totalQuarantined, totalDiscarded, totalMovedToDLQ, 
            totalManualIntervention, activeTrackers
        );
    }
}
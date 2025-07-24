package eda.eventbus.tracing;

import java.time.Instant;

/**
 * Statistics for distributed tracing
 */
public class TracingStatistics {
    private final long totalSpans;
    private final long sampledSpans;
    private final int activeSpans;
    private final long totalEvents;
    private final long totalErrors;
    private final double samplingRate;
    private final Instant timestamp;
    
    private TracingStatistics(Builder builder) {
        this.totalSpans = builder.totalSpans;
        this.sampledSpans = builder.sampledSpans;
        this.activeSpans = builder.activeSpans;
        this.totalEvents = builder.totalEvents;
        this.totalErrors = builder.totalErrors;
        this.samplingRate = builder.samplingRate;
        this.timestamp = builder.timestamp;
    }
    
    public long getTotalSpans() { return totalSpans; }
    public long getSampledSpans() { return sampledSpans; }
    public int getActiveSpans() { return activeSpans; }
    public long getTotalEvents() { return totalEvents; }
    public long getTotalErrors() { return totalErrors; }
    public double getSamplingRate() { return samplingRate; }
    public Instant getTimestamp() { return timestamp; }
    
    /**
     * Get actual sampling rate based on sampled vs total spans
     */
    public double getActualSamplingRate() {
        return totalSpans > 0 ? (double) sampledSpans / totalSpans : 0.0;
    }
    
    /**
     * Get error rate
     */
    public double getErrorRate() {
        return totalSpans > 0 ? (double) totalErrors / totalSpans : 0.0;
    }
    
    /**
     * Get average events per span
     */
    public double getAverageEventsPerSpan() {
        return totalSpans > 0 ? (double) totalEvents / totalSpans : 0.0;
    }
    
    /**
     * Check if tracing is healthy
     */
    public boolean isHealthy() {
        return getErrorRate() < 0.1 && activeSpans < 10000; // Less than 10% errors and not too many active spans
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private long totalSpans = 0;
        private long sampledSpans = 0;
        private int activeSpans = 0;
        private long totalEvents = 0;
        private long totalErrors = 0;
        private double samplingRate = 0.0;
        private Instant timestamp = Instant.now();
        
        public Builder totalSpans(long totalSpans) {
            this.totalSpans = totalSpans;
            return this;
        }
        
        public Builder sampledSpans(long sampledSpans) {
            this.sampledSpans = sampledSpans;
            return this;
        }
        
        public Builder activeSpans(int activeSpans) {
            this.activeSpans = activeSpans;
            return this;
        }
        
        public Builder totalEvents(long totalEvents) {
            this.totalEvents = totalEvents;
            return this;
        }
        
        public Builder totalErrors(long totalErrors) {
            this.totalErrors = totalErrors;
            return this;
        }
        
        public Builder samplingRate(double samplingRate) {
            this.samplingRate = samplingRate;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public TracingStatistics build() {
            return new TracingStatistics(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "TracingStatistics{total=%d, sampled=%d, active=%d, events=%d, errors=%d, samplingRate=%.2f%%, errorRate=%.2f%%}",
            totalSpans, sampledSpans, activeSpans, totalEvents, totalErrors, 
            samplingRate * 100, getErrorRate() * 100
        );
    }
}
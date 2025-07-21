package eda.eventbus.circuitbreaker;

import java.time.Instant;

/**
 * Metrics for circuit breaker
 */
public class CircuitBreakerMetrics {
    private final String subscriberId;
    private final CircuitState state;
    private final int failureCount;
    private final int successCount;
    private final Instant lastFailureTime;
    private final Instant lastStateTransition;
    
    public CircuitBreakerMetrics(String subscriberId, CircuitState state, int failureCount, 
                               int successCount, Instant lastFailureTime, Instant lastStateTransition) {
        this.subscriberId = subscriberId;
        this.state = state;
        this.failureCount = failureCount;
        this.successCount = successCount;
        this.lastFailureTime = lastFailureTime;
        this.lastStateTransition = lastStateTransition;
    }
    
    // Getters
    public String getSubscriberId() { return subscriberId; }
    public CircuitState getState() { return state; }
    public int getFailureCount() { return failureCount; }
    public int getSuccessCount() { return successCount; }
    public Instant getLastFailureTime() { return lastFailureTime; }
    public Instant getLastStateTransition() { return lastStateTransition; }
    
    public double getFailureRate() {
        int total = failureCount + successCount;
        return total > 0 ? (double) failureCount / total : 0.0;
    }
    
    @Override
    public String toString() {
        return "CircuitBreakerMetrics{" +
                "subscriberId='" + subscriberId + '\'' +
                ", state=" + state +
                ", failureCount=" + failureCount +
                ", successCount=" + successCount +
                ", failureRate=" + String.format("%.2f%%", getFailureRate() * 100) +
                '}';
    }
}
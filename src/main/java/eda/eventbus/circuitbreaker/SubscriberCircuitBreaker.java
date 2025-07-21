package eda.eventbus.circuitbreaker;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Circuit breaker implementation for event subscribers
 */
public class SubscriberCircuitBreaker {
    private final String subscriberId;
    private final CircuitBreakerConfig config;
    private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong stateTransitionTime = new AtomicLong(System.currentTimeMillis());
    
    public SubscriberCircuitBreaker(String subscriberId, CircuitBreakerConfig config) {
        this.subscriberId = subscriberId;
        this.config = config;
    }
    
    /**
     * Check if the circuit breaker allows execution
     */
    public boolean allowExecution() {
        CircuitState currentState = state.get();
        long currentTime = System.currentTimeMillis();
        
        switch (currentState) {
            case CLOSED:
                return true;
                
            case OPEN:
                if (currentTime - stateTransitionTime.get() >= config.getWaitDurationInOpenState().toMillis()) {
                    // Transition to half-open
                    if (state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                        stateTransitionTime.set(currentTime);
                        resetCounters();
                    }
                    return state.get() == CircuitState.HALF_OPEN;
                }
                return false;
                
            case HALF_OPEN:
                return successCount.get() < config.getPermittedNumberOfCallsInHalfOpenState();
                
            default:
                return false;
        }
    }
    
    /**
     * Record successful execution
     */
    public void recordSuccess() {
        CircuitState currentState = state.get();
        successCount.incrementAndGet();
        
        if (currentState == CircuitState.HALF_OPEN) {
            if (successCount.get() >= config.getPermittedNumberOfCallsInHalfOpenState()) {
                // Transition back to closed
                if (state.compareAndSet(CircuitState.HALF_OPEN, CircuitState.CLOSED)) {
                    stateTransitionTime.set(System.currentTimeMillis());
                    resetCounters();
                }
            }
        } else if (currentState == CircuitState.CLOSED) {
            // Reset failure count on success
            failureCount.set(0);
        }
    }
    
    /**
     * Record failed execution
     */
    public void recordFailure(Throwable throwable) {
        // Check if this failure should be counted
        if (!config.getRecordFailurePredicate().test(throwable)) {
            return;
        }
        
        CircuitState currentState = state.get();
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        if (currentState == CircuitState.CLOSED) {
            if (failures >= config.getFailureRateThreshold()) {
                // Transition to open
                if (state.compareAndSet(CircuitState.CLOSED, CircuitState.OPEN)) {
                    stateTransitionTime.set(System.currentTimeMillis());
                }
            }
        } else if (currentState == CircuitState.HALF_OPEN) {
            // Any failure in half-open state transitions back to open
            if (state.compareAndSet(CircuitState.HALF_OPEN, CircuitState.OPEN)) {
                stateTransitionTime.set(System.currentTimeMillis());
            }
        }
    }
    
    /**
     * Get current circuit breaker state
     */
    public CircuitState getState() {
        return state.get();
    }
    
    /**
     * Get circuit breaker metrics
     */
    public CircuitBreakerMetrics getMetrics() {
        return new CircuitBreakerMetrics(
            subscriberId,
            state.get(),
            failureCount.get(),
            successCount.get(),
            Instant.ofEpochMilli(lastFailureTime.get()),
            Instant.ofEpochMilli(stateTransitionTime.get())
        );
    }
    
    /**
     * Force circuit breaker to open state
     */
    public void forceOpen() {
        state.set(CircuitState.OPEN);
        stateTransitionTime.set(System.currentTimeMillis());
    }
    
    /**
     * Force circuit breaker to closed state
     */
    public void forceClosed() {
        state.set(CircuitState.CLOSED);
        stateTransitionTime.set(System.currentTimeMillis());
        resetCounters();
    }
    
    /**
     * Reset circuit breaker to initial state
     */
    public void reset() {
        state.set(CircuitState.CLOSED);
        stateTransitionTime.set(System.currentTimeMillis());
        resetCounters();
    }
    
    private void resetCounters() {
        failureCount.set(0);
        successCount.set(0);
    }
    
    @Override
    public String toString() {
        return "SubscriberCircuitBreaker{" +
                "subscriberId='" + subscriberId + '\'' +
                ", state=" + state.get() +
                ", failures=" + failureCount.get() +
                ", successes=" + successCount.get() +
                '}';
    }
}
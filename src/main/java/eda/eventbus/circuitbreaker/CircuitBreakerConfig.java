package eda.eventbus.circuitbreaker;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * Configuration for circuit breaker
 */
public class CircuitBreakerConfig {
    private final int failureRateThreshold;
    private final Duration waitDurationInOpenState;
    private final int permittedNumberOfCallsInHalfOpenState;
    private final Predicate<Throwable> recordFailurePredicate;
    private final Duration slowCallDurationThreshold;
    private final int minimumNumberOfCalls;
    
    private CircuitBreakerConfig(Builder builder) {
        this.failureRateThreshold = builder.failureRateThreshold;
        this.waitDurationInOpenState = builder.waitDurationInOpenState;
        this.permittedNumberOfCallsInHalfOpenState = builder.permittedNumberOfCallsInHalfOpenState;
        this.recordFailurePredicate = builder.recordFailurePredicate;
        this.slowCallDurationThreshold = builder.slowCallDurationThreshold;
        this.minimumNumberOfCalls = builder.minimumNumberOfCalls;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static CircuitBreakerConfig defaultConfig() {
        return builder().build();
    }
    
    // Getters
    public int getFailureRateThreshold() { return failureRateThreshold; }
    public Duration getWaitDurationInOpenState() { return waitDurationInOpenState; }
    public int getPermittedNumberOfCallsInHalfOpenState() { return permittedNumberOfCallsInHalfOpenState; }
    public Predicate<Throwable> getRecordFailurePredicate() { return recordFailurePredicate; }
    public Duration getSlowCallDurationThreshold() { return slowCallDurationThreshold; }
    public int getMinimumNumberOfCalls() { return minimumNumberOfCalls; }
    
    public static class Builder {
        private int failureRateThreshold = 5;  // Number of failures
        private Duration waitDurationInOpenState = Duration.ofSeconds(60);
        private int permittedNumberOfCallsInHalfOpenState = 3;
        private Predicate<Throwable> recordFailurePredicate = throwable -> true;
        private Duration slowCallDurationThreshold = Duration.ofSeconds(5);
        private int minimumNumberOfCalls = 5;
        
        public Builder failureRateThreshold(int failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
            return this;
        }
        
        public Builder waitDurationInOpenState(Duration waitDurationInOpenState) {
            this.waitDurationInOpenState = waitDurationInOpenState;
            return this;
        }
        
        public Builder permittedNumberOfCallsInHalfOpenState(int permittedNumberOfCallsInHalfOpenState) {
            this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
            return this;
        }
        
        public Builder recordFailurePredicate(Predicate<Throwable> recordFailurePredicate) {
            this.recordFailurePredicate = recordFailurePredicate;
            return this;
        }
        
        public Builder slowCallDurationThreshold(Duration slowCallDurationThreshold) {
            this.slowCallDurationThreshold = slowCallDurationThreshold;
            return this;
        }
        
        public Builder minimumNumberOfCalls(int minimumNumberOfCalls) {
            this.minimumNumberOfCalls = minimumNumberOfCalls;
            return this;
        }
        
        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(this);
        }
    }
}
package eda.eventbus.retry;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

/**
 * Configuration for retry behavior
 */
public class RetryConfig {
    private final int maxAttempts;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double backoffMultiplier;
    private final List<Class<? extends Throwable>> retryableExceptions;
    private final List<Class<? extends Throwable>> nonRetryableExceptions;
    private final Predicate<Throwable> retryPredicate;
    
    private RetryConfig(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.retryableExceptions = List.copyOf(builder.retryableExceptions);
        this.nonRetryableExceptions = List.copyOf(builder.nonRetryableExceptions);
        this.retryPredicate = builder.retryPredicate;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static RetryConfig defaultConfig() {
        return builder().build();
    }
    
    public static RetryConfig noRetry() {
        return builder().maxAttempts(1).build();
    }
    
    // Getters
    public int getMaxAttempts() { return maxAttempts; }
    public Duration getInitialDelay() { return initialDelay; }
    public Duration getMaxDelay() { return maxDelay; }
    public double getBackoffMultiplier() { return backoffMultiplier; }
    public List<Class<? extends Throwable>> getRetryableExceptions() { return retryableExceptions; }
    public List<Class<? extends Throwable>> getNonRetryableExceptions() { return nonRetryableExceptions; }
    
    public boolean shouldRetry(Throwable throwable, int attemptNumber) {
        if (attemptNumber >= maxAttempts) {
            return false;
        }
        
        // Check non-retryable exceptions first
        for (Class<? extends Throwable> nonRetryable : nonRetryableExceptions) {
            if (nonRetryable.isAssignableFrom(throwable.getClass())) {
                return false;
            }
        }
        
        // Check retryable exceptions
        if (!retryableExceptions.isEmpty()) {
            boolean isRetryable = retryableExceptions.stream()
                .anyMatch(retryable -> retryable.isAssignableFrom(throwable.getClass()));
            if (!isRetryable) {
                return false;
            }
        }
        
        // Apply custom predicate
        return retryPredicate == null || retryPredicate.test(throwable);
    }
    
    public Duration calculateDelay(int attemptNumber) {
        if (attemptNumber <= 1) {
            return initialDelay;
        }
        
        double delay = initialDelay.toMillis() * Math.pow(backoffMultiplier, attemptNumber - 1);
        long delayMs = Math.min((long) delay, maxDelay.toMillis());
        return Duration.ofMillis(delayMs);
    }
    
    public static class Builder {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofMillis(100);
        private Duration maxDelay = Duration.ofSeconds(30);
        private double backoffMultiplier = 2.0;
        private List<Class<? extends Throwable>> retryableExceptions = List.of(RuntimeException.class);
        private List<Class<? extends Throwable>> nonRetryableExceptions = List.of(IllegalArgumentException.class);
        private Predicate<Throwable> retryPredicate;
        
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }
        
        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }
        
        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }
        
        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }
        
        public Builder retryableExceptions(List<Class<? extends Throwable>> retryableExceptions) {
            this.retryableExceptions = List.copyOf(retryableExceptions);
            return this;
        }
        
        public Builder nonRetryableExceptions(List<Class<? extends Throwable>> nonRetryableExceptions) {
            this.nonRetryableExceptions = List.copyOf(nonRetryableExceptions);
            return this;
        }
        
        public Builder retryPredicate(Predicate<Throwable> retryPredicate) {
            this.retryPredicate = retryPredicate;
            return this;
        }
        
        public RetryConfig build() {
            return new RetryConfig(this);
        }
    }
}
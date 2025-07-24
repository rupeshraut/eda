package eda.eventbus.dlq;

import java.time.Duration;
import java.util.function.Function;

/**
 * Policy for retrying failed events
 */
public class RetryPolicy {
    private final int maxRetries;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double backoffMultiplier;
    private final boolean enableJitter;
    private final Function<Exception, Boolean> retryPredicate;
    private final Duration retryTimeout;
    private final boolean stopOnPoisonMessage;
    
    private RetryPolicy(Builder builder) {
        this.maxRetries = builder.maxRetries;
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.enableJitter = builder.enableJitter;
        this.retryPredicate = builder.retryPredicate;
        this.retryTimeout = builder.retryTimeout;
        this.stopOnPoisonMessage = builder.stopOnPoisonMessage;
    }
    
    public int getMaxRetries() { return maxRetries; }
    public Duration getInitialDelay() { return initialDelay; }
    public Duration getMaxDelay() { return maxDelay; }
    public double getBackoffMultiplier() { return backoffMultiplier; }
    public boolean isJitterEnabled() { return enableJitter; }
    public Function<Exception, Boolean> getRetryPredicate() { return retryPredicate; }
    public Duration getRetryTimeout() { return retryTimeout; }
    public boolean shouldStopOnPoisonMessage() { return stopOnPoisonMessage; }
    
    /**
     * Calculate delay for specific retry attempt
     */
    public Duration calculateDelay(int attemptNumber) {
        if (attemptNumber <= 0) {
            return Duration.ZERO;
        }
        
        // Calculate exponential backoff
        long delayMs = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, attemptNumber - 1));
        
        // Apply maximum delay cap
        delayMs = Math.min(delayMs, maxDelay.toMillis());
        
        // Add jitter if enabled (±25% randomization)
        if (enableJitter) {
            double jitterFactor = 0.75 + (Math.random() * 0.5); // 0.75 to 1.25
            delayMs = (long) (delayMs * jitterFactor);
        }
        
        return Duration.ofMillis(delayMs);
    }
    
    /**
     * Check if exception should be retried based on policy
     */
    public boolean shouldRetry(Exception exception, int currentAttempt) {
        if (currentAttempt >= maxRetries) {
            return false;
        }
        
        // Check poison message policy
        if (stopOnPoisonMessage && isPoisonMessage(exception)) {
            return false;
        }
        
        // Apply custom retry predicate if provided
        if (retryPredicate != null) {
            return retryPredicate.apply(exception);
        }
        
        return isRetryableException(exception);
    }
    
    /**
     * Default retry policies
     */
    public static RetryPolicy defaultPolicy() {
        return builder().build();
    }
    
    public static RetryPolicy noRetry() {
        return builder()
            .maxRetries(0)
            .build();
    }
    
    public static RetryPolicy immediateRetry(int maxRetries) {
        return builder()
            .maxRetries(maxRetries)
            .initialDelay(Duration.ZERO)
            .build();
    }
    
    public static RetryPolicy exponentialBackoff(int maxRetries, Duration initialDelay) {
        return builder()
            .maxRetries(maxRetries)
            .initialDelay(initialDelay)
            .backoffMultiplier(2.0)
            .enableJitter(true)
            .build();
    }
    
    public static RetryPolicy fixedDelay(int maxRetries, Duration delay) {
        return builder()
            .maxRetries(maxRetries)
            .initialDelay(delay)
            .backoffMultiplier(1.0)
            .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int maxRetries = 3;
        private Duration initialDelay = Duration.ofSeconds(1);
        private Duration maxDelay = Duration.ofMinutes(10);
        private double backoffMultiplier = 2.0;
        private boolean enableJitter = true;
        private Function<Exception, Boolean> retryPredicate;
        private Duration retryTimeout = Duration.ofMinutes(30);
        private boolean stopOnPoisonMessage = true;
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
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
        
        public Builder enableJitter(boolean enableJitter) {
            this.enableJitter = enableJitter;
            return this;
        }
        
        public Builder retryPredicate(Function<Exception, Boolean> retryPredicate) {
            this.retryPredicate = retryPredicate;
            return this;
        }
        
        public Builder retryTimeout(Duration retryTimeout) {
            this.retryTimeout = retryTimeout;
            return this;
        }
        
        public Builder stopOnPoisonMessage(boolean stopOnPoisonMessage) {
            this.stopOnPoisonMessage = stopOnPoisonMessage;
            return this;
        }
        
        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
    
    private boolean isPoisonMessage(Exception exception) {
        // Same logic as in FailureReason
        String exceptionName = exception.getClass().getSimpleName().toLowerCase();
        return exceptionName.contains("serialization") ||
               exceptionName.contains("deserialization") ||
               exceptionName.contains("parse") ||
               exceptionName.contains("format") ||
               (exception instanceof ClassCastException) ||
               (exception instanceof NumberFormatException);
    }
    
    private boolean isRetryableException(Exception exception) {
        // Same logic as in FailureReason
        if (exception instanceof IllegalArgumentException ||
            exception instanceof IllegalStateException ||
            exception instanceof UnsupportedOperationException ||
            exception instanceof SecurityException) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return String.format(
            "RetryPolicy{maxRetries=%d, initialDelay=%s, maxDelay=%s, backoff=%.1fx, jitter=%s}",
            maxRetries, initialDelay, maxDelay, backoffMultiplier, enableJitter
        );
    }
}
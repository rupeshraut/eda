package eda.integration;

// ================ Resilience Configuration ================

import java.time.Duration;

public class ResilienceConfig {
    private final Duration timeout;
    private final int maxRetryAttempts;
    private final Duration retryDelay;
    private final double circuitBreakerFailureThreshold;
    private final int circuitBreakerMinCalls;
    private final Duration circuitBreakerWaitDuration;
    private final boolean enableCircuitBreaker;
    private final boolean enableRetry;
    private final boolean enableTimeout;

    private ResilienceConfig(Builder builder) {
        this.timeout = builder.timeout;
        this.maxRetryAttempts = builder.maxRetryAttempts;
        this.retryDelay = builder.retryDelay;
        this.circuitBreakerFailureThreshold = builder.circuitBreakerFailureThreshold;
        this.circuitBreakerMinCalls = builder.circuitBreakerMinCalls;
        this.circuitBreakerWaitDuration = builder.circuitBreakerWaitDuration;
        this.enableCircuitBreaker = builder.enableCircuitBreaker;
        this.enableRetry = builder.enableRetry;
        this.enableTimeout = builder.enableTimeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ResilienceConfig defaultConfig() {
        return builder().build();
    }

    // Getters
    public Duration getTimeout() {
        return timeout;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public double getCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }

    public int getCircuitBreakerMinCalls() {
        return circuitBreakerMinCalls;
    }

    public Duration getCircuitBreakerWaitDuration() {
        return circuitBreakerWaitDuration;
    }

    public boolean isEnableCircuitBreaker() {
        return enableCircuitBreaker;
    }

    public boolean isEnableRetry() {
        return enableRetry;
    }

    public boolean isEnableTimeout() {
        return enableTimeout;
    }

    public static class Builder {
        private Duration timeout = Duration.ofSeconds(30);
        private int maxRetryAttempts = 3;
        private Duration retryDelay = Duration.ofSeconds(1);
        private double circuitBreakerFailureThreshold = 0.5;
        private int circuitBreakerMinCalls = 5;
        private Duration circuitBreakerWaitDuration = Duration.ofSeconds(60);
        private boolean enableCircuitBreaker = true;
        private boolean enableRetry = true;
        private boolean enableTimeout = true;

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
            return this;
        }

        public Builder retryDelay(Duration retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        public Builder circuitBreakerFailureThreshold(double threshold) {
            this.circuitBreakerFailureThreshold = threshold;
            return this;
        }

        public Builder circuitBreakerMinCalls(int minCalls) {
            this.circuitBreakerMinCalls = minCalls;
            return this;
        }

        public Builder circuitBreakerWaitDuration(Duration waitDuration) {
            this.circuitBreakerWaitDuration = waitDuration;
            return this;
        }

        public Builder enableCircuitBreaker(boolean enable) {
            this.enableCircuitBreaker = enable;
            return this;
        }

        public Builder enableRetry(boolean enable) {
            this.enableRetry = enable;
            return this;
        }

        public Builder enableTimeout(boolean enable) {
            this.enableTimeout = enable;
            return this;
        }

        public ResilienceConfig build() {
            return new ResilienceConfig(this);
        }
    }
}
package eda.integration;

// ================ Resilience Components Manager ================

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class ResilienceComponentManager {
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final Map<String, Retry> retries = new ConcurrentHashMap<>();
    private final Map<String, TimeLimiter> timeLimiters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public CircuitBreaker getOrCreateCircuitBreaker(String name, ResilienceConfig config) {
        return circuitBreakers.computeIfAbsent(name, k -> {
            CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold((float) (config.getCircuitBreakerFailureThreshold() * 100))
                    .minimumNumberOfCalls(config.getCircuitBreakerMinCalls())
                    .waitDurationInOpenState(config.getCircuitBreakerWaitDuration())
                    .slidingWindowSize(10)
                    .build();

            CircuitBreaker circuitBreaker = CircuitBreaker.of(name, cbConfig);

            // Add event listeners for monitoring
            circuitBreaker.getEventPublisher()
                    .onStateTransition(event ->
                            System.out.println("Circuit Breaker '" + name + "' state transition: " +
                                    event.getStateTransition()));

            return circuitBreaker;
        });
    }

    public Retry getOrCreateRetry(String name, ResilienceConfig config) {
        return retries.computeIfAbsent(name, k -> {
            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(config.getMaxRetryAttempts())
                    .waitDuration(config.getRetryDelay())
                    .retryExceptions(Exception.class)
                    .build();

            Retry retry = Retry.of(name, retryConfig);

            // Add event listeners for monitoring
            retry.getEventPublisher()
                    .onRetry(event ->
                            System.out.println("Retry '" + name + "' attempt " + event.getNumberOfRetryAttempts()));

            return retry;
        });
    }

    public TimeLimiter getOrCreateTimeLimiter(String name, ResilienceConfig config) {
        return timeLimiters.computeIfAbsent(name, k -> {
            TimeLimiterConfig tlConfig = TimeLimiterConfig.custom()
                    .timeoutDuration(config.getTimeout())
                    .build();

            return TimeLimiter.of(name, tlConfig);
        });
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }
}
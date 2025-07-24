package eda.integration;

// ================ Resilience Components Manager ================

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class ResilienceComponentManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResilienceComponentManager.class);
    
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final Map<String, Retry> retries = new ConcurrentHashMap<>();
    private final Map<String, TimeLimiter> timeLimiters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    
    @Autowired(required = false)
    private IntegrationMetrics metrics;

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
                    .onStateTransition(event -> {
                        String fromState = event.getStateTransition().getFromState().toString();
                        String toState = event.getStateTransition().getToState().toString();
                        
                        LOGGER.info("Circuit breaker '{}' state transition: {} -> {}", 
                            name, fromState, toState);
                        
                        if (metrics != null) {
                            metrics.recordCircuitBreakerStateChange(name, fromState, toState);
                        }
                    });

            LOGGER.debug("Created circuit breaker '{}' with config: failure-rate={}%, min-calls={}, wait-duration={}s",
                name, config.getCircuitBreakerFailureThreshold() * 100, 
                config.getCircuitBreakerMinCalls(), 
                config.getCircuitBreakerWaitDuration().toSeconds());

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
                    .onRetry(event -> {
                        int attemptNumber = event.getNumberOfRetryAttempts();
                        LOGGER.warn("Retry '{}' attempt {} due to: {}", 
                            name, attemptNumber, event.getLastThrowable().getMessage());
                        
                        if (metrics != null) {
                            metrics.recordRetryAttempt(name, attemptNumber);
                        }
                    });

            LOGGER.debug("Created retry '{}' with config: max-attempts={}, delay={}ms", 
                name, config.getMaxRetryAttempts(), config.getRetryDelay().toMillis());

            return retry;
        });
    }

    public TimeLimiter getOrCreateTimeLimiter(String name, ResilienceConfig config) {
        return timeLimiters.computeIfAbsent(name, k -> {
            TimeLimiterConfig tlConfig = TimeLimiterConfig.custom()
                    .timeoutDuration(config.getTimeout())
                    .build();

            TimeLimiter timeLimiter = TimeLimiter.of(name, tlConfig);
            
            // Add event listeners for timeout monitoring
            timeLimiter.getEventPublisher()
                    .onTimeout(event -> {
                        LOGGER.warn("TimeLimiter '{}' timeout after {}ms", name, config.getTimeout().toMillis());
                        
                        if (metrics != null) {
                            metrics.recordTimeout(name, config.getTimeout());
                        }
                    });

            LOGGER.debug("Created time limiter '{}' with timeout={}ms", name, config.getTimeout().toMillis());

            return timeLimiter;
        });
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }
    
    /**
     * Get circuit breaker event publisher for external monitoring
     */
    public io.github.resilience4j.circuitbreaker.CircuitBreaker.EventPublisher getCircuitBreakerEventPublisher(String name) {
        CircuitBreaker cb = circuitBreakers.get(name);
        return cb != null ? cb.getEventPublisher() : null;
    }
    
    /**
     * Get retry event publisher for external monitoring
     */
    public io.github.resilience4j.retry.Retry.EventPublisher getRetryEventPublisher(String name) {
        Retry retry = retries.get(name);
        return retry != null ? retry.getEventPublisher() : null;
    }
    
    /**
     * Shutdown hook to clean up resources
     */
    @PreDestroy
    public void shutdown() {
        LOGGER.info("Shutting down ResilienceComponentManager...");
        
        circuitBreakers.clear();
        retries.clear();
        timeLimiters.clear();
        
        scheduler.shutdown();
        
        LOGGER.info("ResilienceComponentManager shutdown complete");
    }
}
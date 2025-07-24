package eda.integration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Metrics collection for Integration Services
 * Provides comprehensive observability for all integration operations
 */
@Component
public class IntegrationMetrics {
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    
    public IntegrationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * Record successful integration operation
     */
    public void recordSuccess(IntegrationType type, String endpoint, Duration duration) {
        getOrCreateTimer("integration.requests", 
            "type", type.name().toLowerCase(),
            "endpoint", sanitizeEndpoint(endpoint),
            "status", "success"
        ).record(duration);
        
        getOrCreateCounter("integration.requests.total",
            "type", type.name().toLowerCase(),
            "endpoint", sanitizeEndpoint(endpoint),
            "status", "success"
        ).increment();
    }
    
    /**
     * Record failed integration operation
     */
    public void recordFailure(IntegrationType type, String endpoint, Duration duration, String errorType) {
        getOrCreateTimer("integration.requests",
            "type", type.name().toLowerCase(),
            "endpoint", sanitizeEndpoint(endpoint),
            "status", "error",
            "error_type", errorType
        ).record(duration);
        
        getOrCreateCounter("integration.requests.total",
            "type", type.name().toLowerCase(),
            "endpoint", sanitizeEndpoint(endpoint),
            "status", "error",
            "error_type", errorType
        ).increment();
    }
    
    /**
     * Record circuit breaker state change
     */
    public void recordCircuitBreakerStateChange(String name, String fromState, String toState) {
        getOrCreateCounter("integration.circuit_breaker.state_changes",
            "name", name,
            "from_state", fromState,
            "to_state", toState
        ).increment();
    }
    
    /**
     * Record retry attempt
     */
    public void recordRetryAttempt(String name, int attemptNumber) {
        getOrCreateCounter("integration.retry.attempts",
            "name", name,
            "attempt", String.valueOf(attemptNumber)
        ).increment();
    }
    
    /**
     * Record timeout event
     */
    public void recordTimeout(String name, Duration timeoutDuration) {
        getOrCreateCounter("integration.timeouts",
            "name", name,
            "timeout_duration", timeoutDuration.toString()
        ).increment();
    }
    
    /**
     * Record request validation failure
     */
    public void recordValidationFailure(IntegrationType type, String validationError) {
        getOrCreateCounter("integration.validation.failures",
            "type", type.name().toLowerCase(),
            "error_type", validationError
        ).increment();
    }
    
    private Timer getOrCreateTimer(String name, String... tags) {
        String key = buildKey(name, tags);
        return timers.computeIfAbsent(key, k -> 
            Timer.builder(name)
                .tags(tags)
                .description("Integration service operation timer")
                .register(meterRegistry)
        );
    }
    
    private Counter getOrCreateCounter(String name, String... tags) {
        String key = buildKey(name, tags);
        return counters.computeIfAbsent(key, k ->
            Counter.builder(name)
                .tags(tags)
                .description("Integration service operation counter")
                .register(meterRegistry)
        );
    }
    
    private String buildKey(String name, String... tags) {
        StringBuilder key = new StringBuilder(name);
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                key.append(":").append(tags[i]).append("=").append(tags[i + 1]);
            }
        }
        return key.toString();
    }
    
    private String sanitizeEndpoint(String endpoint) {
        if (endpoint == null) return "unknown";
        
        // Remove sensitive information and normalize for metrics
        String sanitized = endpoint.replaceAll("https?://", "")
                                  .replaceAll(":\\d+", "") // Remove port numbers
                                  .replaceAll("/[^/]*$", "/*"); // Replace specific IDs with wildcard
        
        // Limit length safely
        return sanitized.substring(0, Math.min(sanitized.length(), 50));
    }
}
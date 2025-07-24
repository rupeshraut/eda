package eda.integration;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation component for integration requests and configurations
 * Provides comprehensive input validation with detailed error messages
 */
@Component
public class IntegrationValidator {
    
    private static final int MAX_CORRELATION_ID_LENGTH = 255;
    private static final int MAX_HEADER_VALUE_LENGTH = 1000;
    private static final Duration MIN_TIMEOUT = Duration.ofMillis(100);
    private static final Duration MAX_TIMEOUT = Duration.ofMinutes(10);
    
    /**
     * Validate integration request
     */
    public ValidationResult validateRequest(IntegrationRequest<?> request) {
        List<String> errors = new ArrayList<>();
        
        // Validate correlation ID
        if (!StringUtils.hasText(request.getCorrelationId())) {
            errors.add("Correlation ID cannot be null or empty");
        } else if (request.getCorrelationId().length() > MAX_CORRELATION_ID_LENGTH) {
            errors.add("Correlation ID exceeds maximum length of " + MAX_CORRELATION_ID_LENGTH);
        }
        
        // Validate integration type
        if (request.getType() == null) {
            errors.add("Integration type cannot be null");
        }
        
        // Validate payload
        if (request.getPayload() == null) {
            errors.add("Request payload cannot be null");
        }
        
        // Validate headers
        if (request.getHeaders() != null) {
            request.getHeaders().forEach((key, value) -> {
                if (!StringUtils.hasText(key)) {
                    errors.add("Header key cannot be null or empty");
                }
                if (value != null && value.length() > MAX_HEADER_VALUE_LENGTH) {
                    errors.add("Header value for '" + key + "' exceeds maximum length of " + MAX_HEADER_VALUE_LENGTH);
                }
            });
        }
        
        // Validate resilience config
        if (request.getResilienceConfig() != null) {
            ValidationResult resilienceValidation = validateResilienceConfig(request.getResilienceConfig());
            errors.addAll(resilienceValidation.getErrors());
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validate integration configuration
     */
    public ValidationResult validateConfig(IntegrationConfig config) {
        List<String> errors = new ArrayList<>();
        
        // Validate endpoint
        if (!StringUtils.hasText(config.getEndpoint())) {
            errors.add("Endpoint cannot be null or empty");
        } else {
            // Validate URL format for REST and GraphQL
            try {
                new URL(config.getEndpoint());
            } catch (MalformedURLException e) {
                // Could be a queue name or topic name, so only warn for non-HTTP endpoints
                if (config.getEndpoint().startsWith("http://") || config.getEndpoint().startsWith("https://")) {
                    errors.add("Invalid URL format: " + e.getMessage());
                }
            }
        }
        
        // Validate properties
        if (config.getProperties() != null) {
            config.getProperties().forEach((key, value) -> {
                if (!StringUtils.hasText(key)) {
                    errors.add("Configuration property key cannot be null or empty");
                }
                // Validate specific known properties
                if ("timeout".equals(key)) {
                    try {
                        long timeoutMs = Long.parseLong(value.toString());
                        if (timeoutMs < 0) {
                            errors.add("Timeout cannot be negative");
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Invalid timeout value: " + value);
                    }
                }
            });
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validate resilience configuration
     */
    public ValidationResult validateResilienceConfig(ResilienceConfig config) {
        List<String> errors = new ArrayList<>();
        
        // Validate timeout
        if (config.getTimeout() == null) {
            errors.add("Timeout duration cannot be null");
        } else {
            if (config.getTimeout().compareTo(MIN_TIMEOUT) < 0) {
                errors.add("Timeout cannot be less than " + MIN_TIMEOUT.toMillis() + "ms");
            }
            if (config.getTimeout().compareTo(MAX_TIMEOUT) > 0) {
                errors.add("Timeout cannot be greater than " + MAX_TIMEOUT.toMinutes() + " minutes");
            }
        }
        
        // Validate retry configuration
        if (config.getMaxRetryAttempts() < 0) {
            errors.add("Max retry attempts cannot be negative");
        }
        if (config.getMaxRetryAttempts() > 10) {
            errors.add("Max retry attempts cannot exceed 10");
        }
        
        if (config.getRetryDelay() == null) {
            errors.add("Retry delay cannot be null");
        } else if (config.getRetryDelay().isNegative()) {
            errors.add("Retry delay cannot be negative");
        }
        
        // Validate circuit breaker configuration
        if (config.getCircuitBreakerFailureThreshold() < 0 || config.getCircuitBreakerFailureThreshold() > 1.0) {
            errors.add("Circuit breaker failure threshold must be between 0.0 and 1.0");
        }
        
        if (config.getCircuitBreakerMinCalls() < 1) {
            errors.add("Circuit breaker minimum calls must be at least 1");
        }
        
        if (config.getCircuitBreakerWaitDuration() == null) {
            errors.add("Circuit breaker wait duration cannot be null");
        } else if (config.getCircuitBreakerWaitDuration().isNegative()) {
            errors.add("Circuit breaker wait duration cannot be negative");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
package eda.integration;

/**
 * Comprehensive exception hierarchy for integration services
 * Provides specific exception types for different failure scenarios
 */
public class IntegrationException extends RuntimeException {
    
    private final String correlationId;
    private final IntegrationType integrationType;
    private final String endpoint;
    
    public IntegrationException(String message, String correlationId, IntegrationType integrationType, String endpoint) {
        super(message);
        this.correlationId = correlationId;
        this.integrationType = integrationType;
        this.endpoint = endpoint;
    }
    
    public IntegrationException(String message, Throwable cause, String correlationId, IntegrationType integrationType, String endpoint) {
        super(message, cause);
        this.correlationId = correlationId;
        this.integrationType = integrationType;
        this.endpoint = endpoint;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public IntegrationType getIntegrationType() {
        return integrationType;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    @Override
    public String toString() {
        return String.format("%s{correlationId='%s', type=%s, endpoint='%s', message='%s'}", 
            getClass().getSimpleName(), correlationId, integrationType, endpoint, getMessage());
    }
    
    /**
     * Thrown when request validation fails
     */
    public static class ValidationException extends IntegrationException {
        public ValidationException(String message, String correlationId, IntegrationType integrationType) {
            super("Validation failed: " + message, correlationId, integrationType, null);
        }
    }
    
    /**
     * Thrown when configuration is invalid
     */
    public static class ConfigurationException extends IntegrationException {
        public ConfigurationException(String message, String correlationId, IntegrationType integrationType, String endpoint) {
            super("Configuration error: " + message, correlationId, integrationType, endpoint);
        }
    }
    
    /**
     * Thrown when network/connectivity issues occur
     */
    public static class ConnectivityException extends IntegrationException {
        public ConnectivityException(String message, Throwable cause, String correlationId, IntegrationType integrationType, String endpoint) {
            super("Connectivity error: " + message, cause, correlationId, integrationType, endpoint);
        }
    }
    
    /**
     * Thrown when operation times out
     */
    public static class TimeoutException extends IntegrationException {
        private final long timeoutMs;
        
        public TimeoutException(String message, long timeoutMs, String correlationId, IntegrationType integrationType, String endpoint) {
            super("Operation timed out after " + timeoutMs + "ms: " + message, correlationId, integrationType, endpoint);
            this.timeoutMs = timeoutMs;
        }
        
        public long getTimeoutMs() {
            return timeoutMs;
        }
    }
    
    /**
     * Thrown when circuit breaker is open
     */
    public static class CircuitBreakerException extends IntegrationException {
        public CircuitBreakerException(String circuitBreakerName, String correlationId, IntegrationType integrationType, String endpoint) {
            super("Circuit breaker '" + circuitBreakerName + "' is open", correlationId, integrationType, endpoint);
        }
    }
    
    /**
     * Thrown when all retry attempts are exhausted
     */
    public static class RetryExhaustedException extends IntegrationException {
        private final int maxAttempts;
        
        public RetryExhaustedException(String message, int maxAttempts, String correlationId, IntegrationType integrationType, String endpoint) {
            super("Retry exhausted after " + maxAttempts + " attempts: " + message, correlationId, integrationType, endpoint);
            this.maxAttempts = maxAttempts;
        }
        
        public int getMaxAttempts() {
            return maxAttempts;
        }
    }
    
    /**
     * Thrown when remote service returns business error
     */
    public static class BusinessException extends IntegrationException {
        private final int statusCode;
        private final String responseBody;
        
        public BusinessException(String message, int statusCode, String responseBody, String correlationId, IntegrationType integrationType, String endpoint) {
            super("Business error (HTTP " + statusCode + "): " + message, correlationId, integrationType, endpoint);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public String getResponseBody() {
            return responseBody;
        }
    }
    
    /**
     * Thrown when serialization/deserialization fails
     */
    public static class SerializationException extends IntegrationException {
        public SerializationException(String message, Throwable cause, String correlationId, IntegrationType integrationType, String endpoint) {
            super("Serialization error: " + message, cause, correlationId, integrationType, endpoint);
        }
    }
}
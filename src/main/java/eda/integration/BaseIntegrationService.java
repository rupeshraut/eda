package eda.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

abstract class BaseIntegrationService implements IntegrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseIntegrationService.class);
    
    protected final ResilienceComponentManager resilienceManager;
    
    @Autowired(required = false)
    private IntegrationValidator validator;
    
    @Autowired(required = false)
    private IntegrationMetrics metrics;
    
    public BaseIntegrationService(ResilienceComponentManager resilienceManager) {
        this.resilienceManager = resilienceManager;
    }
    
    protected <T> CompletableFuture<T> applyResilience(
            Supplier<CompletableFuture<T>> operation,
            String operationName, 
            ResilienceConfig config) {
        
        Supplier<CompletionStage<T>> decoratedSupplier = operation::get;
        
        // Apply timeout if enabled
        if (config.isEnableTimeout()) {
            TimeLimiter timeLimiter = resilienceManager.getOrCreateTimeLimiter(
                operationName + "-timeout", config);
            decoratedSupplier = TimeLimiter.decorateCompletionStage(
                timeLimiter, resilienceManager.getScheduler(), decoratedSupplier);
        }
        
        // Apply retry if enabled
        if (config.isEnableRetry()) {
            Retry retry = resilienceManager.getOrCreateRetry(operationName + "-retry", config);
            decoratedSupplier = Retry.decorateCompletionStage(retry, resilienceManager.getScheduler(), decoratedSupplier);
        }
        
        // Apply circuit breaker if enabled
        if (config.isEnableCircuitBreaker()) {
            CircuitBreaker circuitBreaker = resilienceManager.getOrCreateCircuitBreaker(
                operationName + "-cb", config);
            decoratedSupplier = CircuitBreaker.decorateCompletionStage(circuitBreaker, decoratedSupplier);
        }
        
        return decoratedSupplier.get().toCompletableFuture();
    }
    
    protected abstract <T, R> CompletableFuture<IntegrationResponse<R>> executeInternal(
        IntegrationRequest<T> request, 
        Class<R> responseType,
        IntegrationConfig config
    );
    
    @Override
    public <T, R> CompletableFuture<IntegrationResponse<R>> executeAsync(
            IntegrationRequest<T> request, Class<R> responseType, IntegrationConfig config) {
        
        Instant startTime = Instant.now();
        String operationName = request.getType().name().toLowerCase() + "-" + sanitizeEndpoint(config.getEndpoint());
        
        LOGGER.debug("Starting integration operation: {} with correlation ID: {}", 
            operationName, request.getCorrelationId());
        
        // Validate request and configuration
        try {
            validateInputs(request, config);
        } catch (IntegrationException.ValidationException e) {
            LOGGER.error("Validation failed for operation {}: {}", operationName, e.getMessage());
            
            if (metrics != null) {
                metrics.recordValidationFailure(request.getType(), e.getMessage());
            }
            
            return CompletableFuture.completedFuture(
                GenericIntegrationResponse.error(request.getCorrelationId(), e.getMessage())
            );
        }
        
        return applyResilience(
            () -> executeInternal(request, responseType, config),
            operationName,
            request.getResilienceConfig()
        ).handle((response, throwable) -> {
            Duration duration = Duration.between(startTime, Instant.now());
            
            if (throwable != null) {
                LOGGER.error("Integration operation {} failed after {}ms: {}", 
                    operationName, duration.toMillis(), throwable.getMessage());
                
                if (metrics != null) {
                    metrics.recordFailure(request.getType(), config.getEndpoint(), 
                        duration, throwable.getClass().getSimpleName());
                }
                
                IntegrationResponse<R> errorResponse = GenericIntegrationResponse.error(
                    request.getCorrelationId(), "Operation failed: " + throwable.getMessage());
                errorResponse.getMetadata().put("resilience_error", true);
                errorResponse.getMetadata().put("error_type", throwable.getClass().getSimpleName());
                errorResponse.getMetadata().put("duration_ms", duration.toMillis());
                return errorResponse;
            }
            
            LOGGER.debug("Integration operation {} completed successfully in {}ms", 
                operationName, duration.toMillis());
            
            if (metrics != null) {
                metrics.recordSuccess(request.getType(), config.getEndpoint(), duration);
            }
            
            // Add duration metadata to successful responses
            response.getMetadata().put("duration_ms", duration.toMillis());
            return response;
        });
    }
    
    @Override
    public <T, R> IntegrationResponse<R> executeSync(
            IntegrationRequest<T> request, Class<R> responseType, IntegrationConfig config) {
        
        LOGGER.debug("Executing synchronous integration operation for correlation ID: {}", 
            request.getCorrelationId());
        
        try {
            return executeAsync(request, responseType, config).get();
        } catch (Exception e) {
            LOGGER.error("Synchronous integration operation failed for correlation ID {}: {}", 
                request.getCorrelationId(), e.getMessage());
            
            return GenericIntegrationResponse.error(request.getCorrelationId(), e.getMessage());
        }
    }
    
    /**
     * Validate request and configuration inputs
     */
    private <T> void validateInputs(IntegrationRequest<T> request, IntegrationConfig config) {
        if (validator != null) {
            IntegrationValidator.ValidationResult requestValidation = validator.validateRequest(request);
            if (!requestValidation.isValid()) {
                throw new IntegrationException.ValidationException(
                    requestValidation.getErrorMessage(), 
                    request.getCorrelationId(), 
                    request.getType()
                );
            }
            
            IntegrationValidator.ValidationResult configValidation = validator.validateConfig(config);
            if (!configValidation.isValid()) {
                throw new IntegrationException.ConfigurationException(
                    configValidation.getErrorMessage(),
                    request.getCorrelationId(),
                    request.getType(),
                    config.getEndpoint()
                );
            }
        }
    }
    
    /**
     * Sanitize endpoint for logging and metrics
     */
    private String sanitizeEndpoint(String endpoint) {
        if (endpoint == null) return "unknown";
        
        // Remove sensitive information and normalize
        String sanitized = endpoint.replaceAll("https?://", "")
                                  .replaceAll(":\\d+", "") // Remove port numbers
                                  .replaceAll("/[^/]*$", "/*"); // Replace specific IDs with wildcard
        
        // Limit length safely
        return sanitized.substring(0, Math.min(sanitized.length(), 30));
    }
}
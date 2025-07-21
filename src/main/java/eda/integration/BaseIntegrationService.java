package eda.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

abstract class BaseIntegrationService implements IntegrationService {
    protected final ResilienceComponentManager resilienceManager;
    
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
        
        String operationName = request.getType().name().toLowerCase() + "-" + config.getEndpoint();
        
        return applyResilience(
            () -> executeInternal(request, responseType, config),
            operationName,
            request.getResilienceConfig()
        ).handle((response, throwable) -> {
            if (throwable != null) {
                IntegrationResponse<R> errorResponse = GenericIntegrationResponse.error(
                    request.getCorrelationId(), "Operation failed: " + throwable.getMessage());
                errorResponse.getMetadata().put("resilience_error", true);
                errorResponse.getMetadata().put("error_type", throwable.getClass().getSimpleName());
                return errorResponse;
            }
            return response;
        });
    }
    
    @Override
    public <T, R> IntegrationResponse<R> executeSync(
            IntegrationRequest<T> request, Class<R> responseType, IntegrationConfig config) {
        try {
            return executeAsync(request, responseType, config).get();
        } catch (Exception e) {
            return GenericIntegrationResponse.error(request.getCorrelationId(), e.getMessage());
        }
    }
}
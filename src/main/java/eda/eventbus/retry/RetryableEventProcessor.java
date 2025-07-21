package eda.eventbus.retry;

import eda.eventbus.core.GenericEvent;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles retryable event processing with exponential backoff
 */
public class RetryableEventProcessor {
    private static final Logger LOGGER = Logger.getLogger(RetryableEventProcessor.class.getName());
    
    private final ScheduledExecutorService scheduler;
    private final RetryConfig defaultConfig;
    
    public RetryableEventProcessor(ScheduledExecutorService scheduler, RetryConfig defaultConfig) {
        this.scheduler = scheduler;
        this.defaultConfig = defaultConfig;
    }
    
    /**
     * Process an event with retry logic
     */
    public <T, D> CompletableFuture<Void> processWithRetry(
            GenericEvent<T, D> event,
            Function<GenericEvent<T, D>, CompletableFuture<Void>> processor) {
        return processWithRetry(event, processor, defaultConfig);
    }
    
    /**
     * Process an event with custom retry configuration
     */
    public <T, D> CompletableFuture<Void> processWithRetry(
            GenericEvent<T, D> event,
            Function<GenericEvent<T, D>, CompletableFuture<Void>> processor,
            RetryConfig retryConfig) {
        
        return processWithRetryInternal(event, processor, retryConfig, 1);
    }
    
    private <T, D> CompletableFuture<Void> processWithRetryInternal(
            GenericEvent<T, D> event,
            Function<GenericEvent<T, D>, CompletableFuture<Void>> processor,
            RetryConfig retryConfig,
            int attemptNumber) {
        
        Instant startTime = Instant.now();
        
        return processor.apply(event)
            .whenComplete((result, throwable) -> {
                if (throwable == null) {
                    LOGGER.log(Level.FINE, "Event processed successfully on attempt {0}: {1}", 
                        new Object[]{attemptNumber, event.getEventId()});
                } else {
                    LOGGER.log(Level.WARNING, "Event processing failed on attempt {0}: {1}, error: {2}", 
                        new Object[]{attemptNumber, event.getEventId(), throwable.getMessage()});
                }
            })
            .exceptionallyCompose(throwable -> {
                if (retryConfig.shouldRetry(throwable, attemptNumber)) {
                    return scheduleRetry(event, processor, retryConfig, attemptNumber + 1);
                } else {
                    LOGGER.log(Level.SEVERE, "Event processing failed permanently after {0} attempts: {1}", 
                        new Object[]{attemptNumber, event.getEventId()});
                    return CompletableFuture.failedFuture(
                        new RetryExhaustedException(event, attemptNumber, throwable));
                }
            });
    }
    
    private <T, D> CompletableFuture<Void> scheduleRetry(
            GenericEvent<T, D> event,
            Function<GenericEvent<T, D>, CompletableFuture<Void>> processor,
            RetryConfig retryConfig,
            int nextAttempt) {
        
        var delay = retryConfig.calculateDelay(nextAttempt);
        
        LOGGER.log(Level.INFO, "Scheduling retry {0} for event {1} after {2}ms", 
            new Object[]{nextAttempt, event.getEventId(), delay.toMillis()});
        
        CompletableFuture<Void> retryFuture = new CompletableFuture<>();
        
        scheduler.schedule(() -> {
            processWithRetryInternal(event, processor, retryConfig, nextAttempt)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        retryFuture.completeExceptionally(throwable);
                    } else {
                        retryFuture.complete(null);
                    }
                });
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
        
        return retryFuture;
    }
    
    /**
     * Exception thrown when retry attempts are exhausted
     */
    public static class RetryExhaustedException extends RuntimeException {
        private final GenericEvent<?, ?> event;
        private final int attempts;
        
        public RetryExhaustedException(GenericEvent<?, ?> event, int attempts, Throwable cause) {
            super("Retry exhausted for event " + event.getEventId() + " after " + attempts + " attempts", cause);
            this.event = event;
            this.attempts = attempts;
        }
        
        public GenericEvent<?, ?> getEvent() { return event; }
        public int getAttempts() { return attempts; }
    }
}
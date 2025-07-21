package eda.eventbus.outbox;

import eda.eventbus.core.GenericEvent;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for outbox pattern implementation
 */
public interface OutboxPublisher {
    
    /**
     * Store event in outbox for reliable publishing
     */
    <T, D> CompletableFuture<OutboxEvent> storeForPublishing(GenericEvent<T, D> event);
    
    /**
     * Store event with scheduled publishing time
     */
    <T, D> CompletableFuture<OutboxEvent> scheduleForPublishing(GenericEvent<T, D> event, Instant scheduledAt);
    
    /**
     * Process pending events from outbox
     */
    CompletableFuture<Integer> processPendingEvents();
    
    /**
     * Process events ready for publishing (past their scheduled time)
     */
    CompletableFuture<Integer> processReadyEvents();
    
    /**
     * Retry failed events
     */
    CompletableFuture<Integer> retryFailedEvents();
    
    /**
     * Get events by status
     */
    CompletableFuture<List<OutboxEvent>> getEventsByStatus(OutboxStatus status);
    
    /**
     * Get events scheduled before a certain time
     */
    CompletableFuture<List<OutboxEvent>> getEventsScheduledBefore(Instant timestamp);
    
    /**
     * Mark event as published
     */
    CompletableFuture<Boolean> markAsPublished(OutboxEvent outboxEvent);
    
    /**
     * Mark event as failed
     */
    CompletableFuture<Boolean> markAsFailed(OutboxEvent outboxEvent, String error);
    
    /**
     * Cancel event publishing
     */
    CompletableFuture<Boolean> cancelEvent(OutboxEvent outboxEvent);
    
    /**
     * Clean up old published/dead letter events
     */
    CompletableFuture<Long> cleanupOldEvents(Instant olderThan);
    
    /**
     * Get outbox statistics
     */
    CompletableFuture<OutboxStats> getStats();
    
    /**
     * Start background processing of outbox events
     */
    void startBackgroundProcessing();
    
    /**
     * Stop background processing
     */
    void stopBackgroundProcessing();
}
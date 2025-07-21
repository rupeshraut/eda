package eda.eventbus.deadletter;

import eda.eventbus.core.GenericEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Interface for dead letter queue implementations
 */
public interface DeadLetterQueue {
    
    /**
     * Send an event to the dead letter queue
     */
    <T, D> CompletableFuture<Void> sendToDeadLetter(DeadLetterEvent<T, D> deadLetterEvent);
    
    /**
     * Retrieve events from dead letter queue
     */
    CompletableFuture<List<DeadLetterEvent<?, ?>>> getDeadLetterEvents(int limit);
    
    /**
     * Retrieve events from dead letter queue with filter
     */
    CompletableFuture<List<DeadLetterEvent<?, ?>>> getDeadLetterEvents(
        Predicate<DeadLetterEvent<?, ?>> filter, int limit);
    
    /**
     * Get a specific dead letter event by ID
     */
    CompletableFuture<Optional<DeadLetterEvent<?, ?>>> getDeadLetterEvent(UUID deadLetterId);
    
    /**
     * Requeue an event from dead letter queue for reprocessing
     */
    CompletableFuture<Boolean> requeue(UUID deadLetterId);
    
    /**
     * Remove an event from dead letter queue (acknowledge permanent failure)
     */
    CompletableFuture<Boolean> remove(UUID deadLetterId);
    
    /**
     * Get dead letter events by original event type
     */
    CompletableFuture<List<DeadLetterEvent<?, ?>>> getDeadLetterEventsByType(String eventType);
    
    /**
     * Get dead letter events by failure reason
     */
    CompletableFuture<List<DeadLetterEvent<?, ?>>> getDeadLetterEventsByReason(DeadLetterReason reason);
    
    /**
     * Get dead letter events newer than specified timestamp
     */
    CompletableFuture<List<DeadLetterEvent<?, ?>>> getDeadLetterEventsAfter(Instant timestamp);
    
    /**
     * Count total dead letter events
     */
    CompletableFuture<Long> getDeadLetterCount();
    
    /**
     * Count dead letter events by reason
     */
    CompletableFuture<Long> getDeadLetterCountByReason(DeadLetterReason reason);
}
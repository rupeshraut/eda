package eda.eventbus.persistence;

import eda.eventbus.core.GenericEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Interface for event persistence implementations
 */
public interface EventPersistence {
    
    /**
     * Persist an event
     */
    <T, D> CompletableFuture<Void> persistEvent(GenericEvent<T, D> event);
    
    /**
     * Persist multiple events in a batch
     */
    <T, D> CompletableFuture<Void> persistEvents(List<GenericEvent<T, D>> events);
    
    /**
     * Get event by ID
     */
    CompletableFuture<Optional<GenericEvent<?, ?>>> getEvent(UUID eventId);
    
    /**
     * Get events by type
     */
    CompletableFuture<List<GenericEvent<?, ?>>> getEventsByType(String eventType);
    
    /**
     * Get events by source
     */
    CompletableFuture<List<GenericEvent<?, ?>>> getEventsBySource(String source);
    
    /**
     * Get events after a specific timestamp
     */
    CompletableFuture<Stream<GenericEvent<?, ?>>> getEventsAfter(Instant timestamp);
    
    /**
     * Get events by correlation ID
     */
    CompletableFuture<List<GenericEvent<?, ?>>> getEventsByCorrelationId(UUID correlationId);
    
    /**
     * Get total event count
     */
    CompletableFuture<Long> getEventCount();
    
    /**
     * Get event count by type
     */
    CompletableFuture<Long> getEventCountByType(String eventType);
    
    /**
     * Delete events older than specified timestamp
     */
    CompletableFuture<Long> deleteEventsOlderThan(Instant timestamp);
    
    /**
     * Health check for persistence layer
     */
    CompletableFuture<Boolean> isHealthy();
}
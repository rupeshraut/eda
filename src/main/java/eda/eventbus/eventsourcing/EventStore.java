package eda.eventbus.eventsourcing;

import eda.eventbus.core.GenericEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Event store interface for event sourcing
 */
public interface EventStore {
    
    /**
     * Append events to a stream
     */
    <T, D> CompletableFuture<Void> appendToStream(String streamName, List<GenericEvent<T, D>> events, long expectedVersion);
    
    /**
     * Append single event to a stream
     */
    <T, D> CompletableFuture<Void> appendToStream(String streamName, GenericEvent<T, D> event, long expectedVersion);
    
    /**
     * Read events from a stream
     */
    CompletableFuture<EventStream> readStream(String streamName, long fromVersion, int maxCount);
    
    /**
     * Read all events from a stream
     */
    CompletableFuture<EventStream> readStream(String streamName);
    
    /**
     * Read events from all streams after a specific position
     */
    CompletableFuture<Stream<GenericEvent<?, ?>>> readAllEventsAfter(long position);
    
    /**
     * Read events by event type
     */
    CompletableFuture<List<GenericEvent<?, ?>>> readEventsByType(String eventType, Instant fromTime);
    
    /**
     * Get stream version
     */
    CompletableFuture<Long> getStreamVersion(String streamName);
    
    /**
     * Check if stream exists
     */
    CompletableFuture<Boolean> streamExists(String streamName);
    
    /**
     * Delete stream
     */
    CompletableFuture<Void> deleteStream(String streamName, long expectedVersion);
    
    /**
     * Create snapshot of a stream at specific version
     */
    <T> CompletableFuture<Void> saveSnapshot(String streamName, long version, T snapshot);
    
    /**
     * Load latest snapshot for a stream
     */
    <T> CompletableFuture<Optional<Snapshot<T>>> loadSnapshot(String streamName, Class<T> snapshotType);
    
    /**
     * Get all stream names
     */
    CompletableFuture<List<String>> getAllStreamNames();
    
    /**
     * Get event store statistics
     */
    CompletableFuture<EventStoreStats> getStats();
}
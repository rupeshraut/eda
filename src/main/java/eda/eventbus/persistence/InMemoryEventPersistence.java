package eda.eventbus.persistence;

import eda.eventbus.core.GenericEvent;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import java.util.logging.Logger;

/**
 * In-memory implementation of EventPersistence for development and testing
 */
public class InMemoryEventPersistence implements EventPersistence {
    private static final Logger LOGGER = Logger.getLogger(InMemoryEventPersistence.class.getName());
    
    private final Map<UUID, GenericEvent<?, ?>> eventStore = new ConcurrentHashMap<>();
    private final Map<String, List<UUID>> typeIndex = new ConcurrentHashMap<>();
    private final Map<String, List<UUID>> sourceIndex = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> correlationIndex = new ConcurrentHashMap<>();
    private final List<UUID> timeOrderedEvents = new CopyOnWriteArrayList<>();
    
    @Override
    public <T, D> CompletableFuture<Void> persistEvent(GenericEvent<T, D> event) {
        return CompletableFuture.runAsync(() -> {
            // Store event
            eventStore.put(event.getEventId(), event);
            
            // Update indexes
            typeIndex.computeIfAbsent(event.getEventType().toString(), k -> new CopyOnWriteArrayList<>())
                    .add(event.getEventId());
            
            sourceIndex.computeIfAbsent(event.getSource(), k -> new CopyOnWriteArrayList<>())
                     .add(event.getEventId());
            
            if (event.getCorrelationId() != null) {
                correlationIndex.computeIfAbsent(event.getCorrelationId(), k -> new CopyOnWriteArrayList<>())
                               .add(event.getEventId());
            }
            
            // Add to time-ordered list (maintain order)
            insertInTimeOrder(event.getEventId(), event.getTimestamp());
            
            LOGGER.fine("Persisted event: " + event.getEventId());
        });
    }
    
    @Override
    public <T, D> CompletableFuture<Void> persistEvents(List<GenericEvent<T, D>> events) {
        return CompletableFuture.allOf(
            events.stream()
                .map(this::persistEvent)
                .toArray(CompletableFuture[]::new)
        );
    }
    
    @Override
    public CompletableFuture<Optional<GenericEvent<?, ?>>> getEvent(UUID eventId) {
        return CompletableFuture.supplyAsync(() -> 
            Optional.ofNullable(eventStore.get(eventId))
        );
    }
    
    @Override
    public CompletableFuture<List<GenericEvent<?, ?>>> getEventsByType(String eventType) {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> eventIds = typeIndex.getOrDefault(eventType, List.of());
            return eventIds.stream()
                .map(eventStore::get)
                .filter(Objects::nonNull)
                .sorted((e1, e2) -> e1.getTimestamp().compareTo(e2.getTimestamp()))
                .toList();
        });
    }
    
    @Override
    public CompletableFuture<List<GenericEvent<?, ?>>> getEventsBySource(String source) {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> eventIds = sourceIndex.getOrDefault(source, List.of());
            return eventIds.stream()
                .map(eventStore::get)
                .filter(Objects::nonNull)
                .sorted((e1, e2) -> e1.getTimestamp().compareTo(e2.getTimestamp()))
                .toList();
        });
    }
    
    @Override
    public CompletableFuture<Stream<GenericEvent<?, ?>>> getEventsAfter(Instant timestamp) {
        return CompletableFuture.supplyAsync(() -> 
            timeOrderedEvents.stream()
                .map(eventStore::get)
                .filter(Objects::nonNull)
                .filter(event -> event.getTimestamp().isAfter(timestamp))
        );
    }
    
    @Override
    public CompletableFuture<List<GenericEvent<?, ?>>> getEventsByCorrelationId(UUID correlationId) {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> eventIds = correlationIndex.getOrDefault(correlationId, List.of());
            return eventIds.stream()
                .map(eventStore::get)
                .filter(Objects::nonNull)
                .sorted((e1, e2) -> e1.getTimestamp().compareTo(e2.getTimestamp()))
                .toList();
        });
    }
    
    @Override
    public CompletableFuture<Long> getEventCount() {
        return CompletableFuture.supplyAsync(() -> (long) eventStore.size());
    }
    
    @Override
    public CompletableFuture<Long> getEventCountByType(String eventType) {
        return CompletableFuture.supplyAsync(() -> 
            (long) typeIndex.getOrDefault(eventType, List.of()).size()
        );
    }
    
    @Override
    public CompletableFuture<Long> deleteEventsOlderThan(Instant timestamp) {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> toDelete = timeOrderedEvents.stream()
                .map(eventStore::get)
                .filter(Objects::nonNull)
                .filter(event -> event.getTimestamp().isBefore(timestamp))
                .map(GenericEvent::getEventId)
                .toList();
            
            toDelete.forEach(this::removeEvent);
            
            LOGGER.info("Deleted " + toDelete.size() + " events older than " + timestamp);
            return (long) toDelete.size();
        });
    }
    
    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return CompletableFuture.completedFuture(true);
    }
    
    /**
     * Clear all events (for testing)
     */
    public void clear() {
        eventStore.clear();
        typeIndex.clear();
        sourceIndex.clear();
        correlationIndex.clear();
        timeOrderedEvents.clear();
        LOGGER.info("Event store cleared");
    }
    
    /**
     * Get current size of event store
     */
    public int size() {
        return eventStore.size();
    }
    
    private void insertInTimeOrder(UUID eventId, Instant timestamp) {
        // Simple insertion - for production use a more efficient data structure
        int insertIndex = 0;
        for (int i = timeOrderedEvents.size() - 1; i >= 0; i--) {
            GenericEvent<?, ?> existingEvent = eventStore.get(timeOrderedEvents.get(i));
            if (existingEvent != null && existingEvent.getTimestamp().isBefore(timestamp)) {
                insertIndex = i + 1;
                break;
            }
        }
        timeOrderedEvents.add(insertIndex, eventId);
    }
    
    private void removeEvent(UUID eventId) {
        GenericEvent<?, ?> event = eventStore.remove(eventId);
        if (event != null) {
            // Remove from indexes
            typeIndex.getOrDefault(event.getEventType().toString(), List.of()).remove(eventId);
            sourceIndex.getOrDefault(event.getSource(), List.of()).remove(eventId);
            if (event.getCorrelationId() != null) {
                correlationIndex.getOrDefault(event.getCorrelationId(), List.of()).remove(eventId);
            }
            timeOrderedEvents.remove(eventId);
        }
    }
}
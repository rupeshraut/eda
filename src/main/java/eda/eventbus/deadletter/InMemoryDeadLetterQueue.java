package eda.eventbus.deadletter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory implementation of DeadLetterQueue for development and testing
 */
public class InMemoryDeadLetterQueue implements DeadLetterQueue {
    private static final Logger LOGGER = Logger.getLogger(InMemoryDeadLetterQueue.class.getName());
    
    private final Map<UUID, DeadLetterEvent<?, ?>> deadLetterEvents = new ConcurrentHashMap<>();
    private final List<DeadLetterEventListener> listeners = new CopyOnWriteArrayList<>();
    
    @Override
    public <T, D> CompletableFuture<Void> sendToDeadLetter(DeadLetterEvent<T, D> deadLetterEvent) {
        return CompletableFuture.runAsync(() -> {
            deadLetterEvents.put(deadLetterEvent.getDeadLetterId(), deadLetterEvent);
            
            LOGGER.log(Level.WARNING, "Event sent to dead letter queue: {0}", deadLetterEvent);
            
            // Notify listeners
            listeners.forEach(listener -> {
                try {
                    listener.onDeadLetterEvent(deadLetterEvent);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error notifying dead letter listener", e);
                }
            });
        });
    }
    
    @Override
    public CompletableFuture<List<DeadLetterEvent<?, ?>>> getDeadLetterEvents(int limit) {
        return CompletableFuture.supplyAsync(() -> 
            deadLetterEvents.values().stream()
                .sorted((e1, e2) -> e2.getDeadLetterTimestamp().compareTo(e1.getDeadLetterTimestamp()))
                .limit(limit)
                .toList()
        );
    }
    
    @Override
    public CompletableFuture<List<DeadLetterEvent<?, ?>>> getDeadLetterEvents(
            Predicate<DeadLetterEvent<?, ?>> filter, int limit) {
        return CompletableFuture.supplyAsync(() -> 
            deadLetterEvents.values().stream()
                .filter(filter)
                .sorted((e1, e2) -> e2.getDeadLetterTimestamp().compareTo(e1.getDeadLetterTimestamp()))
                .limit(limit)
                .toList()
        );
    }
    
    @Override
    public CompletableFuture<Optional<DeadLetterEvent<?, ?>>> getDeadLetterEvent(UUID deadLetterId) {
        return CompletableFuture.supplyAsync(() -> 
            Optional.ofNullable(deadLetterEvents.get(deadLetterId))
        );
    }
    
    @Override
    public CompletableFuture<Boolean> requeue(UUID deadLetterId) {
        return CompletableFuture.supplyAsync(() -> {
            DeadLetterEvent<?, ?> event = deadLetterEvents.remove(deadLetterId);
            if (event != null) {
                LOGGER.log(Level.INFO, "Requeuing dead letter event: {0}", deadLetterId);
                
                // Notify listeners about requeue
                listeners.forEach(listener -> {
                    try {
                        listener.onRequeue(event);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error notifying requeue listener", e);
                    }
                });
                return true;
            }
            return false;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> remove(UUID deadLetterId) {
        return CompletableFuture.supplyAsync(() -> {
            boolean removed = deadLetterEvents.remove(deadLetterId) != null;
            if (removed) {
                LOGGER.log(Level.INFO, "Removed dead letter event: {0}", deadLetterId);
            }
            return removed;
        });
    }
    
    @Override
    public CompletableFuture<List<DeadLetterEvent<?, ?>>> getDeadLetterEventsByType(String eventType) {
        return CompletableFuture.supplyAsync(() -> 
            deadLetterEvents.values().stream()
                .filter(event -> event.getOriginalEvent().getEventType().toString().equals(eventType))
                .sorted((e1, e2) -> e2.getDeadLetterTimestamp().compareTo(e1.getDeadLetterTimestamp()))
                .toList()
        );
    }
    
    @Override
    public CompletableFuture<List<DeadLetterEvent<?, ?>>> getDeadLetterEventsByReason(DeadLetterReason reason) {
        return CompletableFuture.supplyAsync(() -> 
            deadLetterEvents.values().stream()
                .filter(event -> event.getReason() == reason)
                .sorted((e1, e2) -> e2.getDeadLetterTimestamp().compareTo(e1.getDeadLetterTimestamp()))
                .toList()
        );
    }
    
    @Override
    public CompletableFuture<List<DeadLetterEvent<?, ?>>> getDeadLetterEventsAfter(Instant timestamp) {
        return CompletableFuture.supplyAsync(() -> 
            deadLetterEvents.values().stream()
                .filter(event -> event.getDeadLetterTimestamp().isAfter(timestamp))
                .sorted((e1, e2) -> e2.getDeadLetterTimestamp().compareTo(e1.getDeadLetterTimestamp()))
                .toList()
        );
    }
    
    @Override
    public CompletableFuture<Long> getDeadLetterCount() {
        return CompletableFuture.supplyAsync(() -> (long) deadLetterEvents.size());
    }
    
    @Override
    public CompletableFuture<Long> getDeadLetterCountByReason(DeadLetterReason reason) {
        return CompletableFuture.supplyAsync(() -> 
            deadLetterEvents.values().stream()
                .filter(event -> event.getReason() == reason)
                .count()
        );
    }
    
    /**
     * Add a listener for dead letter events
     */
    public void addListener(DeadLetterEventListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener
     */
    public void removeListener(DeadLetterEventListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Clear all dead letter events (for testing)
     */
    public void clear() {
        deadLetterEvents.clear();
        LOGGER.info("Dead letter queue cleared");
    }
    
    /**
     * Interface for listening to dead letter events
     */
    public interface DeadLetterEventListener {
        void onDeadLetterEvent(DeadLetterEvent<?, ?> event);
        
        default void onRequeue(DeadLetterEvent<?, ?> event) {
            // Default empty implementation
        }
    }
}
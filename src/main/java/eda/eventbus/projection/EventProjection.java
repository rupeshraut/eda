package eda.eventbus.projection;

import eda.eventbus.core.GenericEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for event projections
 */
public interface EventProjection {
    
    /**
     * Get projection name
     */
    String getProjectionName();
    
    /**
     * Get event types this projection handles
     */
    List<String> getHandledEventTypes();
    
    /**
     * Process an event
     */
    CompletableFuture<Void> handleEvent(GenericEvent<?, ?> event);
    
    /**
     * Reset projection to initial state
     */
    CompletableFuture<Void> reset();
    
    /**
     * Get current projection position
     */
    CompletableFuture<Long> getCurrentPosition();
    
    /**
     * Set projection position
     */
    CompletableFuture<Void> setPosition(long position);
    
    /**
     * Check if projection can handle an event type
     */
    default boolean canHandle(String eventType) {
        return getHandledEventTypes().contains(eventType);
    }
    
    /**
     * Get projection state
     */
    CompletableFuture<ProjectionState> getState();
    
    /**
     * Start projection processing
     */
    CompletableFuture<Void> start();
    
    /**
     * Stop projection processing
     */
    CompletableFuture<Void> stop();
}
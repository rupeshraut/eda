package eda.eventbus.projection;

import eda.eventbus.core.GenericEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Manager for event projections
 */
public interface ProjectionManager {
    
    /**
     * Register a projection
     */
    CompletableFuture<Void> registerProjection(EventProjection projection);
    
    /**
     * Unregister a projection
     */
    CompletableFuture<Boolean> unregisterProjection(String projectionName);
    
    /**
     * Get projection by name
     */
    Optional<EventProjection> getProjection(String projectionName);
    
    /**
     * Get all registered projections
     */
    List<EventProjection> getAllProjections();
    
    /**
     * Start a projection
     */
    CompletableFuture<Void> startProjection(String projectionName);
    
    /**
     * Stop a projection
     */
    CompletableFuture<Void> stopProjection(String projectionName);
    
    /**
     * Restart a projection
     */
    CompletableFuture<Void> restartProjection(String projectionName);
    
    /**
     * Start all projections
     */
    CompletableFuture<Void> startAll();
    
    /**
     * Stop all projections
     */
    CompletableFuture<Void> stopAll();
    
    /**
     * Rebuild a projection from the beginning
     */
    CompletableFuture<Void> rebuildProjection(String projectionName);
    
    /**
     * Handle an event by routing it to appropriate projections
     */
    CompletableFuture<Void> handleEvent(GenericEvent<?, ?> event);
    
    /**
     * Get projection statistics
     */
    CompletableFuture<List<ProjectionStats>> getProjectionStats();
    
    /**
     * Check health of all projections
     */
    CompletableFuture<ProjectionHealthStatus> checkHealth();
}
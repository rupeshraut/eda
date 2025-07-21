package eda.eventbus.projection;

/**
 * State of an event projection
 */
public enum ProjectionState {
    /**
     * Projection is stopped
     */
    STOPPED,
    
    /**
     * Projection is starting up
     */
    STARTING,
    
    /**
     * Projection is running and processing events
     */
    RUNNING,
    
    /**
     * Projection is stopping
     */
    STOPPING,
    
    /**
     * Projection has failed
     */
    FAILED,
    
    /**
     * Projection is rebuilding from start
     */
    REBUILDING
}
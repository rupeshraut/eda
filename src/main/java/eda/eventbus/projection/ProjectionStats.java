package eda.eventbus.projection;

import java.time.Instant;

/**
 * Statistics for a projection
 */
public class ProjectionStats {
    private final String projectionName;
    private final ProjectionState state;
    private final long currentPosition;
    private final long eventsProcessed;
    private final long eventsFailed;
    private final Instant lastEventTime;
    private final Instant lastErrorTime;
    private final String lastError;
    private final double averageProcessingTimeMs;
    
    public ProjectionStats(String projectionName, ProjectionState state, long currentPosition,
                          long eventsProcessed, long eventsFailed, Instant lastEventTime,
                          Instant lastErrorTime, String lastError, double averageProcessingTimeMs) {
        this.projectionName = projectionName;
        this.state = state;
        this.currentPosition = currentPosition;
        this.eventsProcessed = eventsProcessed;
        this.eventsFailed = eventsFailed;
        this.lastEventTime = lastEventTime;
        this.lastErrorTime = lastErrorTime;
        this.lastError = lastError;
        this.averageProcessingTimeMs = averageProcessingTimeMs;
    }
    
    // Getters
    public String getProjectionName() { return projectionName; }
    public ProjectionState getState() { return state; }
    public long getCurrentPosition() { return currentPosition; }
    public long getEventsProcessed() { return eventsProcessed; }
    public long getEventsFailed() { return eventsFailed; }
    public Instant getLastEventTime() { return lastEventTime; }
    public Instant getLastErrorTime() { return lastErrorTime; }
    public String getLastError() { return lastError; }
    public double getAverageProcessingTimeMs() { return averageProcessingTimeMs; }
    
    public double getSuccessRate() {
        long total = eventsProcessed + eventsFailed;
        return total > 0 ? (double) eventsProcessed / total : 0.0;
    }
    
    public boolean isHealthy() {
        return state == ProjectionState.RUNNING && getSuccessRate() > 0.95;
    }
    
    @Override
    public String toString() {
        return "ProjectionStats{" +
                "projectionName='" + projectionName + '\'' +
                ", state=" + state +
                ", position=" + currentPosition +
                ", processed=" + eventsProcessed +
                ", failed=" + eventsFailed +
                ", successRate=" + String.format("%.2f%%", getSuccessRate() * 100) +
                '}';
    }
}
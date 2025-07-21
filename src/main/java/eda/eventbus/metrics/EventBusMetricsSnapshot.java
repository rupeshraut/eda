package eda.eventbus.metrics;

import java.time.Instant;
import java.util.Map;

/**
 * Snapshot of event bus metrics at a point in time
 */
public class EventBusMetricsSnapshot {
    private final Instant timestamp;
    private final long totalEventsPublished;
    private final long totalEventsProcessed;
    private final long totalEventsFailed;
    private final long totalDeadLettered;
    private final long totalRetries;
    private final long totalTimeouts;
    private final long totalSubscriptions;
    private final double averageProcessingTimeMs;
    private final double successRate;
    private final Map<String, EventTypeMetrics> eventTypeMetrics;
    private final Map<String, SubscriberMetrics> subscriberMetrics;
    
    public EventBusMetricsSnapshot(
            Instant timestamp,
            long totalEventsPublished,
            long totalEventsProcessed,
            long totalEventsFailed,
            long totalDeadLettered,
            long totalRetries,
            long totalTimeouts,
            long totalSubscriptions,
            double averageProcessingTimeMs,
            double successRate,
            Map<String, EventTypeMetrics> eventTypeMetrics,
            Map<String, SubscriberMetrics> subscriberMetrics) {
        this.timestamp = timestamp;
        this.totalEventsPublished = totalEventsPublished;
        this.totalEventsProcessed = totalEventsProcessed;
        this.totalEventsFailed = totalEventsFailed;
        this.totalDeadLettered = totalDeadLettered;
        this.totalRetries = totalRetries;
        this.totalTimeouts = totalTimeouts;
        this.totalSubscriptions = totalSubscriptions;
        this.averageProcessingTimeMs = averageProcessingTimeMs;
        this.successRate = successRate;
        this.eventTypeMetrics = Map.copyOf(eventTypeMetrics);
        this.subscriberMetrics = Map.copyOf(subscriberMetrics);
    }
    
    // Getters
    public Instant getTimestamp() { return timestamp; }
    public long getTotalEventsPublished() { return totalEventsPublished; }
    public long getTotalEventsProcessed() { return totalEventsProcessed; }
    public long getTotalEventsFailed() { return totalEventsFailed; }
    public long getTotalDeadLettered() { return totalDeadLettered; }
    public long getTotalRetries() { return totalRetries; }
    public long getTotalTimeouts() { return totalTimeouts; }
    public long getTotalSubscriptions() { return totalSubscriptions; }
    public double getAverageProcessingTimeMs() { return averageProcessingTimeMs; }
    public double getSuccessRate() { return successRate; }
    public Map<String, EventTypeMetrics> getEventTypeMetrics() { return eventTypeMetrics; }
    public Map<String, SubscriberMetrics> getSubscriberMetrics() { return subscriberMetrics; }
    
    @Override
    public String toString() {
        return "EventBusMetricsSnapshot{" +
                "timestamp=" + timestamp +
                ", totalEventsPublished=" + totalEventsPublished +
                ", totalEventsProcessed=" + totalEventsProcessed +
                ", totalEventsFailed=" + totalEventsFailed +
                ", successRate=" + String.format("%.2f%%", successRate * 100) +
                ", averageProcessingTimeMs=" + String.format("%.2f", averageProcessingTimeMs) +
                '}';
    }
}
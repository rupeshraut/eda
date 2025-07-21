package eda.eventbus.outbox;

import java.time.Instant;

/**
 * Statistics for outbox publisher
 */
public class OutboxStats {
    private final long pendingEvents;
    private final long processingEvents;
    private final long publishedEvents;
    private final long failedEvents;
    private final long deadLetterEvents;
    private final long cancelledEvents;
    private final Instant oldestPendingEvent;
    private final double publishSuccessRate;
    
    public OutboxStats(long pendingEvents, long processingEvents, long publishedEvents, 
                      long failedEvents, long deadLetterEvents, long cancelledEvents,
                      Instant oldestPendingEvent) {
        this.pendingEvents = pendingEvents;
        this.processingEvents = processingEvents;
        this.publishedEvents = publishedEvents;
        this.failedEvents = failedEvents;
        this.deadLetterEvents = deadLetterEvents;
        this.cancelledEvents = cancelledEvents;
        this.oldestPendingEvent = oldestPendingEvent;
        
        long totalAttempted = publishedEvents + failedEvents + deadLetterEvents;
        this.publishSuccessRate = totalAttempted > 0 ? (double) publishedEvents / totalAttempted : 0.0;
    }
    
    // Getters
    public long getPendingEvents() { return pendingEvents; }
    public long getProcessingEvents() { return processingEvents; }
    public long getPublishedEvents() { return publishedEvents; }
    public long getFailedEvents() { return failedEvents; }
    public long getDeadLetterEvents() { return deadLetterEvents; }
    public long getCancelledEvents() { return cancelledEvents; }
    public Instant getOldestPendingEvent() { return oldestPendingEvent; }
    public double getPublishSuccessRate() { return publishSuccessRate; }
    
    public long getTotalEvents() {
        return pendingEvents + processingEvents + publishedEvents + 
               failedEvents + deadLetterEvents + cancelledEvents;
    }
    
    @Override
    public String toString() {
        return "OutboxStats{" +
                "pending=" + pendingEvents +
                ", processing=" + processingEvents +
                ", published=" + publishedEvents +
                ", failed=" + failedEvents +
                ", deadLetter=" + deadLetterEvents +
                ", successRate=" + String.format("%.2f%%", publishSuccessRate * 100) +
                '}';
    }
}
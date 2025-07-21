package eda.eventbus.eventsourcing;

import java.time.Instant;

/**
 * Statistics for the event store
 */
public class EventStoreStats {
    private final long totalEvents;
    private final long totalStreams;
    private final long totalSnapshots;
    private final Instant oldestEvent;
    private final Instant newestEvent;
    private final long storageSize;
    
    public EventStoreStats(long totalEvents, long totalStreams, long totalSnapshots,
                          Instant oldestEvent, Instant newestEvent, long storageSize) {
        this.totalEvents = totalEvents;
        this.totalStreams = totalStreams;
        this.totalSnapshots = totalSnapshots;
        this.oldestEvent = oldestEvent;
        this.newestEvent = newestEvent;
        this.storageSize = storageSize;
    }
    
    // Getters
    public long getTotalEvents() { return totalEvents; }
    public long getTotalStreams() { return totalStreams; }
    public long getTotalSnapshots() { return totalSnapshots; }
    public Instant getOldestEvent() { return oldestEvent; }
    public Instant getNewestEvent() { return newestEvent; }
    public long getStorageSize() { return storageSize; }
    
    @Override
    public String toString() {
        return "EventStoreStats{" +
                "totalEvents=" + totalEvents +
                ", totalStreams=" + totalStreams +
                ", totalSnapshots=" + totalSnapshots +
                ", storageSize=" + storageSize + " bytes" +
                '}';
    }
}
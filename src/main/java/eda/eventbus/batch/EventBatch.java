package eda.eventbus.batch;

import eda.eventbus.core.GenericEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents a batch of events for processing
 */
public class EventBatch<T, D> {
    private final UUID batchId;
    private final List<GenericEvent<T, D>> events;
    private final Instant createdAt;
    private final String subscriberId;
    private final BatchMetadata metadata;
    
    public EventBatch(List<GenericEvent<T, D>> events, String subscriberId, BatchMetadata metadata) {
        this.batchId = UUID.randomUUID();
        this.events = List.copyOf(events);
        this.createdAt = Instant.now();
        this.subscriberId = subscriberId;
        this.metadata = metadata;
    }
    
    // Getters
    public UUID getBatchId() { return batchId; }
    public List<GenericEvent<T, D>> getEvents() { return events; }
    public Instant getCreatedAt() { return createdAt; }
    public String getSubscriberId() { return subscriberId; }
    public BatchMetadata getMetadata() { return metadata; }
    
    public int size() {
        return events.size();
    }
    
    public boolean isEmpty() {
        return events.isEmpty();
    }
    
    @Override
    public String toString() {
        return "EventBatch{" +
                "batchId=" + batchId +
                ", size=" + events.size() +
                ", subscriberId='" + subscriberId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
package eda.eventbus.eventsourcing;

import java.time.Instant;

/**
 * Represents a snapshot of aggregate state
 */
public class Snapshot<T> {
    private final String streamName;
    private final long version;
    private final T data;
    private final Instant timestamp;
    
    public Snapshot(String streamName, long version, T data, Instant timestamp) {
        this.streamName = streamName;
        this.version = version;
        this.data = data;
        this.timestamp = timestamp;
    }
    
    public Snapshot(String streamName, long version, T data) {
        this(streamName, version, data, Instant.now());
    }
    
    // Getters
    public String getStreamName() { return streamName; }
    public long getVersion() { return version; }
    public T getData() { return data; }
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return "Snapshot{" +
                "streamName='" + streamName + '\'' +
                ", version=" + version +
                ", timestamp=" + timestamp +
                '}';
    }
}
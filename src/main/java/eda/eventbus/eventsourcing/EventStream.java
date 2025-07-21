package eda.eventbus.eventsourcing;

import eda.eventbus.core.GenericEvent;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a stream of events
 */
public class EventStream implements Iterable<GenericEvent<?, ?>> {
    private final String streamName;
    private final long version;
    private final List<GenericEvent<?, ?>> events;
    private final boolean isEndOfStream;
    
    public EventStream(String streamName, long version, List<GenericEvent<?, ?>> events, boolean isEndOfStream) {
        this.streamName = streamName;
        this.version = version;
        this.events = List.copyOf(events);
        this.isEndOfStream = isEndOfStream;
    }
    
    public static EventStream empty(String streamName) {
        return new EventStream(streamName, -1, List.of(), true);
    }
    
    public static EventStream of(String streamName, long version, List<GenericEvent<?, ?>> events) {
        return new EventStream(streamName, version, events, true);
    }
    
    // Getters
    public String getStreamName() { return streamName; }
    public long getVersion() { return version; }
    public List<GenericEvent<?, ?>> getEvents() { return events; }
    public boolean isEndOfStream() { return isEndOfStream; }
    public boolean isEmpty() { return events.isEmpty(); }
    public int size() { return events.size(); }
    
    @Override
    public Iterator<GenericEvent<?, ?>> iterator() {
        return events.iterator();
    }
    
    /**
     * Get last event version in this stream
     */
    public long getLastEventVersion() {
        if (events.isEmpty()) {
            return version;
        }
        return version + events.size() - 1;
    }
    
    @Override
    public String toString() {
        return "EventStream{" +
                "streamName='" + streamName + '\'' +
                ", version=" + version +
                ", eventCount=" + events.size() +
                ", isEndOfStream=" + isEndOfStream +
                '}';
    }
}
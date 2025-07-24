package eda.eventbus.tracing;

import java.time.Instant;
import java.util.Map;

/**
 * Event within a trace span
 */
public class SpanEvent {
    private final String name;
    private final Instant timestamp;
    private final Map<String, String> attributes;
    
    public SpanEvent(String name, Instant timestamp, Map<String, String> attributes) {
        this.name = name;
        this.timestamp = timestamp;
        this.attributes = Map.copyOf(attributes);
    }
    
    public String getName() { return name; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, String> getAttributes() { return attributes; }
    
    /**
     * Get attribute value
     */
    public String getAttribute(String key) {
        return attributes.get(key);
    }
    
    /**
     * Check if attribute exists
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    
    /**
     * Create exception event
     */
    public static SpanEvent exception(Throwable throwable) {
        return new SpanEvent(
            "exception",
            Instant.now(),
            Map.of(
                "exception.type", throwable.getClass().getSimpleName(),
                "exception.message", throwable.getMessage() != null ? throwable.getMessage() : ""
            )
        );
    }
    
    /**
     * Create log event
     */
    public static SpanEvent log(String level, String message) {
        return new SpanEvent(
            "log",
            Instant.now(),
            Map.of(
                "log.level", level,
                "log.message", message
            )
        );
    }
    
    /**
     * Create custom event
     */
    public static SpanEvent custom(String name, Map<String, String> attributes) {
        return new SpanEvent(name, Instant.now(), attributes);
    }
    
    @Override
    public String toString() {
        return String.format(
            "SpanEvent{name='%s', timestamp=%s, attributes=%s}",
            name, timestamp, attributes
        );
    }
}
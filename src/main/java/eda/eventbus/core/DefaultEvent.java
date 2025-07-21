package eda.eventbus.core;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Default immutable implementation of GenericEvent
 * @param <T> The event type enum
 * @param <D> The event data type
 */
public final class DefaultEvent<T, D> implements GenericEvent<T, D> {
    private final UUID eventId;
    private final T eventType;
    private final D data;
    private final Map<String, String> headers;
    private final String source;
    private final UUID correlationId;
    private final UUID causationId;
    private final Instant timestamp;
    private final EventPriority priority;
    private final String version;
    
    private DefaultEvent(Builder<T, D> builder) {
        this.eventId = Objects.requireNonNull(builder.eventId, "eventId cannot be null");
        this.eventType = Objects.requireNonNull(builder.eventType, "eventType cannot be null");
        this.data = builder.data;
        this.headers = Map.copyOf(builder.headers);
        this.source = Objects.requireNonNull(builder.source, "source cannot be null");
        this.correlationId = builder.correlationId;
        this.causationId = builder.causationId;
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp cannot be null");
        this.priority = Objects.requireNonNull(builder.priority, "priority cannot be null");
        this.version = Objects.requireNonNull(builder.version, "version cannot be null");
    }
    
    public static <T, D> Builder<T, D> builder() {
        return new Builder<>();
    }
    
    @Override
    public UUID getEventId() { return eventId; }
    
    @Override
    public T getEventType() { return eventType; }
    
    @Override
    public D getData() { return data; }
    
    @Override
    public Map<String, String> getHeaders() { return headers; }
    
    @Override
    public String getSource() { return source; }
    
    @Override
    public UUID getCorrelationId() { return correlationId; }
    
    @Override
    public UUID getCausationId() { return causationId; }
    
    @Override
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public EventPriority getPriority() { return priority; }
    
    @Override
    public String getVersion() { return version; }
    
    @Override
    public GenericEvent<T, D> withCorrelation(UUID correlationId, UUID causationId) {
        return this.<T, D>toBuilder()
            .correlationId(correlationId)
            .causationId(causationId)
            .build();
    }
    
    @Override
    public GenericEvent<T, D> withHeaders(Map<String, String> additionalHeaders) {
        Map<String, String> newHeaders = new HashMap<>(this.headers);
        newHeaders.putAll(additionalHeaders);
        return this.<T, D>toBuilder()
            .headers(newHeaders)
            .build();
    }
    
    public Builder<T, D> toBuilder() {
        return new Builder<T, D>()
            .eventId(eventId)
            .eventType(eventType)
            .data(data)
            .headers(headers)
            .source(source)
            .correlationId(correlationId)
            .causationId(causationId)
            .timestamp(timestamp)
            .priority(priority)
            .version(version);
    }
    
    public static class Builder<T, D> {
        private UUID eventId = UUID.randomUUID();
        private T eventType;
        private D data;
        private Map<String, String> headers = new HashMap<>();
        private String source;
        private UUID correlationId;
        private UUID causationId;
        private Instant timestamp = Instant.now();
        private EventPriority priority = EventPriority.NORMAL;
        private String version = "1.0";
        
        public Builder<T, D> eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }
        
        public Builder<T, D> eventType(T eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public Builder<T, D> data(D data) {
            this.data = data;
            return this;
        }
        
        public Builder<T, D> headers(Map<String, String> headers) {
            this.headers = new HashMap<>(headers);
            return this;
        }
        
        public Builder<T, D> header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }
        
        public Builder<T, D> source(String source) {
            this.source = source;
            return this;
        }
        
        public Builder<T, D> correlationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder<T, D> causationId(UUID causationId) {
            this.causationId = causationId;
            return this;
        }
        
        public Builder<T, D> timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder<T, D> priority(EventPriority priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder<T, D> version(String version) {
            this.version = version;
            return this;
        }
        
        public DefaultEvent<T, D> build() {
            return new DefaultEvent<>(this);
        }
    }
    
    @Override
    public String toString() {
        return "DefaultEvent{" +
                "eventId=" + eventId +
                ", eventType=" + eventType +
                ", source='" + source + '\'' +
                ", priority=" + priority +
                ", timestamp=" + timestamp +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultEvent<?, ?> that = (DefaultEvent<?, ?>) o;
        return Objects.equals(eventId, that.eventId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
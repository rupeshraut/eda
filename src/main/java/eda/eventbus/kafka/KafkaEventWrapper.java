package eda.eventbus.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Wrapper for serializing events to Kafka
 */
public class KafkaEventWrapper {
    private final String eventId;
    private final String eventType;
    private final Object data;
    private final String source;
    private final Instant timestamp;
    private final String version;
    private final String correlationId;
    private final String causationId;
    private final Map<String, String> headers;
    
    @JsonCreator
    public KafkaEventWrapper(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("data") Object data,
            @JsonProperty("source") String source,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("version") String version,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("causationId") String causationId,
            @JsonProperty("headers") Map<String, String> headers) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.data = data;
        this.source = source;
        this.timestamp = timestamp;
        this.version = version;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.headers = headers;
    }
    
    // Getters
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Object getData() { return data; }
    public String getSource() { return source; }
    public Instant getTimestamp() { return timestamp; }
    public String getVersion() { return version; }
    public String getCorrelationId() { return correlationId; }
    public String getCausationId() { return causationId; }
    public Map<String, String> getHeaders() { return headers; }
    
    @Override
    public String toString() {
        return "KafkaEventWrapper{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                ", version=" + version +
                '}';
    }
}
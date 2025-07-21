package eda.eventbus.core;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Generic event interface that can be extended for any domain
 * @param <T> The event type enum
 * @param <D> The event data type
 */
public interface GenericEvent<T, D> {
    UUID getEventId();
    T getEventType();
    D getData();
    Map<String, String> getHeaders();
    String getSource();
    UUID getCorrelationId();
    UUID getCausationId();
    Instant getTimestamp();
    EventPriority getPriority();
    String getVersion();
    
    /**
     * Create a new event with correlation tracking
     */
    GenericEvent<T, D> withCorrelation(UUID correlationId, UUID causationId);
    
    /**
     * Create a new event with additional headers
     */
    GenericEvent<T, D> withHeaders(Map<String, String> additionalHeaders);
}
package eda.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * The type Event.
 */
// Base event record using Java Record feature (immutable data class)
public record Event(
        UUID eventId,
        EventType eventType,
        Map<String, Object> data,
        String source,
        Instant timestamp
) {
    /**
     * Of event.
     *
     * @param eventType the event type
     * @param data      the data
     * @param source    the source
     * @return the event
     */
// Factory method for creating new events
    public static Event of(EventType eventType, Map<String, Object> data, String source) {
        return new Event(
                UUID.randomUUID(),
                eventType,
                Map.copyOf(data),  // Defensive copy for immutability
                source,
                Instant.now()
        );
    }

    @Override
    public String toString() {
        return """
                Event[
                    id=%s,
                    type=%s,
                    source=%s,
                    timestamp=%s
                ]
                """.formatted(eventId, eventType, source, timestamp);
    }
}
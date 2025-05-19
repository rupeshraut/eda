package eda.saga;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

// Base saga event record
public record SagaEvent(
        UUID sagaId,
        UUID eventId,
        SagaEventType eventType,
        Map<String, Object> data,
        String source,
        Instant timestamp
) {
    public static SagaEvent of(UUID sagaId, SagaEventType eventType, Map<String, Object> data, String source) {
        return new SagaEvent(
                sagaId,
                UUID.randomUUID(),
                eventType,
                Map.copyOf(data),
                source,
                Instant.now()
        );
    }
}
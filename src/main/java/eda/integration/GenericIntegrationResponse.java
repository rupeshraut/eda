package eda.integration;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class GenericIntegrationResponse<T> implements IntegrationResponse<T> {
    private final String correlationId;
    private final T data;
    private final boolean success;
    private final String errorMessage;
    private final Map<String, Object> metadata;

    public GenericIntegrationResponse(String correlationId, T data, boolean success, String errorMessage) {
        this.correlationId = correlationId;
        this.data = data;
        this.success = success;
        this.errorMessage = errorMessage;
        this.metadata = new HashMap<>();
        this.metadata.put("timestamp", LocalDateTime.now());
    }

    public static <T> GenericIntegrationResponse<T> success(String correlationId, T data) {
        return new GenericIntegrationResponse<>(correlationId, data, true, null);
    }

    public static <T> GenericIntegrationResponse<T> error(String correlationId, String errorMessage) {
        return new GenericIntegrationResponse<>(correlationId, null, false, errorMessage);
    }

    // Getters
    public String getCorrelationId() {
        return correlationId;
    }

    public T getData() {
        return data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public GenericIntegrationResponse<T> withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
}
package eda.integration;// ================ Request/Response Models ================

import java.util.HashMap;
import java.util.Map;

class GenericIntegrationRequest<T> implements IntegrationRequest<T> {
    private final String correlationId;
    private final T payload;
    private final Map<String, String> headers;
    private final IntegrationType type;
    private final ResilienceConfig resilienceConfig;

    public GenericIntegrationRequest(String correlationId, T payload, IntegrationType type, ResilienceConfig resilienceConfig) {
        this.correlationId = correlationId;
        this.payload = payload;
        this.type = type;
        this.headers = new HashMap<>();
        this.resilienceConfig = resilienceConfig != null ? resilienceConfig : ResilienceConfig.defaultConfig();
    }

    public GenericIntegrationRequest(String correlationId, T payload, IntegrationType type) {
        this(correlationId, payload, type, ResilienceConfig.defaultConfig());
    }

    // Getters
    public String getCorrelationId() {
        return correlationId;
    }

    public T getPayload() {
        return payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public IntegrationType getType() {
        return type;
    }

    public ResilienceConfig getResilienceConfig() {
        return resilienceConfig;
    }

    public GenericIntegrationRequest<T> withHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }
}
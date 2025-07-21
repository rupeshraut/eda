package eda.integration;

import java.util.Map;

public interface IntegrationRequest<T> {
    String getCorrelationId();
    T getPayload();
    Map<String, String> getHeaders();
    IntegrationType getType();
    ResilienceConfig getResilienceConfig();
}
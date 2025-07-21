package eda.integration;

import java.util.Map;

public interface IntegrationResponse<T> {
    String getCorrelationId();
    T getData();
    boolean isSuccess();
    String getErrorMessage();
    Map<String, Object> getMetadata();
}
package eda.integration;// ================ Core Integration Interface ================

import java.util.concurrent.CompletableFuture;

interface IntegrationService {
    <T, R> CompletableFuture<IntegrationResponse<R>> executeAsync(
            IntegrationRequest<T> request,
            Class<R> responseType,
            IntegrationConfig config
    );

    <T, R> IntegrationResponse<R> executeSync(
            IntegrationRequest<T> request,
            Class<R> responseType,
            IntegrationConfig config
    );
}
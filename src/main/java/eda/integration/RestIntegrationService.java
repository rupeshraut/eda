package eda.integration;

// ================ REST Integration Service ================

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;

@Service
class RestIntegrationService extends BaseIntegrationService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public RestIntegrationService(WebClient webClient, ObjectMapper objectMapper, 
                                ResilienceComponentManager resilienceManager) {
        super(resilienceManager);
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }
    
    @Override
    protected <T, R> CompletableFuture<IntegrationResponse<R>> executeInternal(
            IntegrationRequest<T> request, Class<R> responseType, IntegrationConfig config) {
        
        return webClient.post()
            .uri(config.getEndpoint())
            .headers(headers -> request.getHeaders().forEach(headers::add))
            .bodyValue(request.getPayload())
            .retrieve()
            .bodyToMono(responseType)
            .map(response -> {
                IntegrationResponse<R> result = GenericIntegrationResponse.success(request.getCorrelationId(), response);
                result.getMetadata().put("integration_type", "REST");
                result.getMetadata().put("endpoint", config.getEndpoint());
                return result;
            })
            .onErrorMap(throwable -> new RuntimeException("REST call failed", throwable))
            .toFuture();
    }
}
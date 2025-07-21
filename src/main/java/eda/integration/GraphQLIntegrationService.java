package eda.integration;

// ================ GraphQL Integration Service ================

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;

@Service
class GraphQLIntegrationService extends BaseIntegrationService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public GraphQLIntegrationService(WebClient webClient, ObjectMapper objectMapper,
                                   ResilienceComponentManager resilienceManager) {
        super(resilienceManager);
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }
    
    @Override
    protected <T, R> CompletableFuture<IntegrationResponse<R>> executeInternal(
            IntegrationRequest<T> request, Class<R> responseType, IntegrationConfig config) {
        
        GraphQLRequest graphQLRequest = new GraphQLRequest(
            config.getProperties().get("query"),
            request.getPayload()
        );
        
        return webClient.post()
            .uri(config.getEndpoint())
            .headers(headers -> request.getHeaders().forEach(headers::add))
            .bodyValue(graphQLRequest)
            .retrieve()
            .bodyToMono(GraphQLResponse.class)
            .map(response -> {
                if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                    throw new RuntimeException("GraphQL errors: " + response.getErrors());
                }
                R data = objectMapper.convertValue(response.getData(), responseType);
                IntegrationResponse<R> result = GenericIntegrationResponse.success(request.getCorrelationId(), data);
                result.getMetadata().put("integration_type", "GRAPHQL");
                result.getMetadata().put("endpoint", config.getEndpoint());
                return result;
            })
            .onErrorMap(throwable -> new RuntimeException("GraphQL call failed", throwable))
            .toFuture();
    }
    
    static class GraphQLRequest {
        private final String query;
        private final Object variables;
        
        public GraphQLRequest(String query, Object variables) {
            this.query = query;
            this.variables = variables;
        }
        
        public String getQuery() { return query; }
        public Object getVariables() { return variables; }
    }
    
    static class GraphQLResponse {
        private Object data;
        private java.util.List<Object> errors;
        
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        public java.util.List<Object> getErrors() { return errors; }
        public void setErrors(java.util.List<Object> errors) { this.errors = errors; }
    }
}
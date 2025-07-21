package eda.integration;

// ================ JMS Integration Service ================

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
class JmsIntegrationService extends BaseIntegrationService {
    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    
    public JmsIntegrationService(JmsTemplate jmsTemplate, ObjectMapper objectMapper,
                               ResilienceComponentManager resilienceManager) {
        super(resilienceManager);
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    protected <T, R> CompletableFuture<IntegrationResponse<R>> executeInternal(
            IntegrationRequest<T> request, Class<R> responseType, IntegrationConfig config) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonPayload = objectMapper.writeValueAsString(request.getPayload());
                
                jmsTemplate.convertAndSend(config.getEndpoint(), jsonPayload, message -> {
                    message.setJMSCorrelationID(request.getCorrelationId());
                    request.getHeaders().forEach((key, value) -> {
                        try {
                            message.setStringProperty(key, value);
                        } catch (Exception e) {
                            System.err.println("Failed to set JMS property: " + key);
                        }
                    });
                    return message;
                });
                
                IntegrationResponse<R> result = GenericIntegrationResponse.success(request.getCorrelationId(), 
                    objectMapper.convertValue(Map.of("status", "sent", "queue", config.getEndpoint()), responseType));
                result.getMetadata().put("integration_type", "JMS");
                result.getMetadata().put("queue", config.getEndpoint());
                return result;
                
            } catch (Exception e) {
                throw new RuntimeException("JMS send failed", e);
            }
        });
    }
}
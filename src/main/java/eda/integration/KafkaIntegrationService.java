package eda.integration;

// ================ Kafka Integration Service ================

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
class KafkaIntegrationService extends BaseIntegrationService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public KafkaIntegrationService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                                 ResilienceComponentManager resilienceManager) {
        super(resilienceManager);
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    protected <T, R> CompletableFuture<IntegrationResponse<R>> executeInternal(
            IntegrationRequest<T> request, Class<R> responseType, IntegrationConfig config) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonPayload = objectMapper.writeValueAsString(request.getPayload());
                String topic = config.getEndpoint();
                
                kafkaTemplate.send(topic, request.getCorrelationId(), jsonPayload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            System.out.println("Kafka message sent successfully to topic: " + topic);
                        } else {
                            System.err.println("Kafka message send failed: " + ex);
                        }
                    });
                
                IntegrationResponse<R> result = GenericIntegrationResponse.success(request.getCorrelationId(), 
                    objectMapper.convertValue(Map.of("status", "sent", "topic", topic), responseType));
                result.getMetadata().put("integration_type", "KAFKA");
                result.getMetadata().put("topic", topic);
                return result;
                
            } catch (Exception e) {
                throw new RuntimeException("Kafka send failed", e);
            }
        });
    }
}
package eda.eventbus.kafka;

import eda.eventbus.core.GenericEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Kafka event publisher that publishes events to Kafka topics
 */
public class KafkaEventPublisher {
    private static final Logger LOGGER = Logger.getLogger(KafkaEventPublisher.class.getName());
    
    private final KafkaEventBusConfig config;
    private final Object producer; // KafkaProducer - using Object to avoid Kafka dependency
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, String> topicCache = new ConcurrentHashMap<>();
    private volatile boolean isRunning = true;
    
    public KafkaEventPublisher(KafkaEventBusConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Create Kafka producer with reflection to avoid hard dependency
        this.producer = createKafkaProducer(config.getProducerProperties());
        
        LOGGER.info("Kafka event publisher initialized");
    }
    
    /**
     * Publish an event to Kafka
     */
    public <T, D> CompletableFuture<Void> publish(GenericEvent<T, D> event) {
        if (!isRunning) {
            return CompletableFuture.failedFuture(new IllegalStateException("Publisher is shutdown"));
        }
        
        try {
            String topic = getTopicName(event.getEventType().toString());
            String key = event.getEventId().toString();
            String value = serializeEvent(event);
            
            return sendToKafka(topic, key, value)
                .thenRun(() -> LOGGER.fine("Event published to Kafka: " + event.getEventId()))
                .exceptionally(throwable -> {
                    LOGGER.severe("Failed to publish event to Kafka: " + throwable.getMessage());
                    throw new RuntimeException("Kafka publish failed", throwable);
                });
                
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Publish multiple events as a batch
     */
    public <T, D> CompletableFuture<Void> publishBatch(Iterable<GenericEvent<T, D>> events) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (GenericEvent<T, D> event : events) {
            futures.add(publish(event));
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    /**
     * Shutdown the publisher
     */
    public void shutdown() {
        isRunning = false;
        if (producer != null) {
            try {
                // Close Kafka producer using reflection
                producer.getClass().getMethod("close").invoke(producer);
                LOGGER.info("Kafka producer closed");
            } catch (Exception e) {
                LOGGER.warning("Error closing Kafka producer: " + e.getMessage());
            }
        }
    }
    
    private String getTopicName(String eventType) {
        return topicCache.computeIfAbsent(eventType, 
            type -> config.getTopicPrefix() + "." + type.toLowerCase().replace("_", "-"));
    }
    
    private <T, D> String serializeEvent(GenericEvent<T, D> event) throws JsonProcessingException {
        KafkaEventWrapper wrapper = new KafkaEventWrapper(
            event.getEventId().toString(),
            event.getEventType().toString(),
            event.getData(),
            event.getSource(),
            event.getTimestamp(),
            event.getVersion(),
            event.getCorrelationId() != null ? event.getCorrelationId().toString() : null,
            event.getCausationId() != null ? event.getCausationId().toString() : null,
            event.getHeaders()
        );
        return objectMapper.writeValueAsString(wrapper);
    }
    
    private Object createKafkaProducer(Properties properties) {
        try {
            // Try to create KafkaProducer using reflection to avoid hard dependency
            Class<?> producerClass = Class.forName("org.apache.kafka.clients.producer.KafkaProducer");
            return producerClass.getConstructor(Properties.class).newInstance(properties);
        } catch (Exception e) {
            LOGGER.warning("Kafka not available, using mock producer: " + e.getMessage());
            return new MockKafkaProducer();
        }
    }
    
    private CompletableFuture<Void> sendToKafka(String topic, String key, String value) {
        if (producer instanceof MockKafkaProducer) {
            return ((MockKafkaProducer) producer).send(topic, key, value);
        }
        
        try {
            // Use reflection to send to real Kafka
            Class<?> recordClass = Class.forName("org.apache.kafka.clients.producer.ProducerRecord");
            Object record = recordClass.getConstructor(String.class, String.class, String.class)
                .newInstance(topic, key, value);
            
            Future<?> future = (Future<?>) producer.getClass()
                .getMethod("send", recordClass)
                .invoke(producer, record);
            
            return CompletableFuture.runAsync(() -> {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    
    /**
     * Mock Kafka producer for testing when Kafka is not available
     */
    private static class MockKafkaProducer {
        public CompletableFuture<Void> send(String topic, String key, String value) {
            LOGGER.info("Mock Kafka send - Topic: " + topic + ", Key: " + key + ", Value length: " + value.length());
            return CompletableFuture.completedFuture(null);
        }
    }
}
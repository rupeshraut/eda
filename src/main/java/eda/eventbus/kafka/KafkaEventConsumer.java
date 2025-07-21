package eda.eventbus.kafka;

import eda.eventbus.GenericEventBus;
import eda.eventbus.core.DefaultEvent;
import eda.eventbus.core.GenericEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Kafka event consumer that consumes events from Kafka topics and forwards them to the event bus
 */
public class KafkaEventConsumer<T> {
    private static final Logger LOGGER = Logger.getLogger(KafkaEventConsumer.class.getName());
    
    private final KafkaEventBusConfig config;
    private final GenericEventBus<T> eventBus;
    private final Class<T> eventTypeClass;
    private final ObjectMapper objectMapper;
    private final ExecutorService consumerExecutor;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Set<String> subscribedTopics = new HashSet<>();
    private Object kafkaConsumer; // KafkaConsumer - using Object to avoid Kafka dependency
    
    public KafkaEventConsumer(KafkaEventBusConfig config, GenericEventBus<T> eventBus, Class<T> eventTypeClass) {
        this.config = config;
        this.eventBus = eventBus;
        this.eventTypeClass = eventTypeClass;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.consumerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "kafka-consumer-" + eventTypeClass.getSimpleName());
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Start consuming events from Kafka
     */
    public CompletableFuture<Void> start(String groupId, String... topics) {
        if (isRunning.compareAndSet(false, true)) {
            return CompletableFuture.runAsync(() -> {
                try {
                    kafkaConsumer = createKafkaConsumer(groupId);
                    subscribedTopics.addAll(Arrays.asList(topics));
                    
                    subscribeToTopics(kafkaConsumer, Arrays.asList(topics));
                    
                    LOGGER.info("Started Kafka consumer for topics: " + Arrays.toString(topics));
                    
                    // Start polling loop
                    pollLoop();
                    
                } catch (Exception e) {
                    LOGGER.severe("Failed to start Kafka consumer: " + e.getMessage());
                    isRunning.set(false);
                    throw new RuntimeException(e);
                }
            }, consumerExecutor);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Stop consuming events
     */
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            isRunning.set(false);
            
            if (kafkaConsumer != null) {
                try {
                    // Close Kafka consumer using reflection
                    kafkaConsumer.getClass().getMethod("close").invoke(kafkaConsumer);
                    LOGGER.info("Kafka consumer closed");
                } catch (Exception e) {
                    LOGGER.warning("Error closing Kafka consumer: " + e.getMessage());
                }
            }
            
            consumerExecutor.shutdown();
        });
    }
    
    private void pollLoop() {
        while (isRunning.get()) {
            try {
                Object records = pollRecords(kafkaConsumer, Duration.ofMillis(1000));
                processRecords(records);
                commitOffsets(kafkaConsumer);
            } catch (Exception e) {
                if (isRunning.get()) {
                    LOGGER.severe("Error in poll loop: " + e.getMessage());
                    // Wait before retrying
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
    
    private void processRecords(Object records) {
        if (records instanceof MockConsumerRecords) {
            // Process mock records
            ((MockConsumerRecords) records).forEach(record -> processRecord(record.topic(), record.value()));
        } else {
            // Process real Kafka records using reflection
            try {
                Iterable<?> recordsIterable = (Iterable<?>) records;
                for (Object record : recordsIterable) {
                    String topic = (String) record.getClass().getMethod("topic").invoke(record);
                    String value = (String) record.getClass().getMethod("value").invoke(record);
                    processRecord(topic, value);
                }
            } catch (Exception e) {
                LOGGER.warning("Error processing Kafka records: " + e.getMessage());
            }
        }
    }
    
    private void processRecord(String topic, String value) {
        try {
            KafkaEventWrapper wrapper = objectMapper.readValue(value, KafkaEventWrapper.class);
            GenericEvent<T, Object> event = convertToGenericEvent(wrapper);
            
            // Forward to event bus
            eventBus.publish(event).exceptionally(throwable -> {
                LOGGER.warning("Failed to forward Kafka event to event bus: " + throwable.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            LOGGER.warning("Failed to process Kafka record from topic " + topic + ": " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    private GenericEvent<T, Object> convertToGenericEvent(KafkaEventWrapper wrapper) {
        // Convert string event type back to enum
        T eventType = convertStringToEventType(wrapper.getEventType());
        
        return DefaultEvent.<T, Object>builder()
            .eventId(UUID.fromString(wrapper.getEventId()))
            .eventType(eventType)
            .data(wrapper.getData())
            .source(wrapper.getSource())
            .timestamp(wrapper.getTimestamp())
            .version(wrapper.getVersion())
            .correlationId(wrapper.getCorrelationId() != null ? UUID.fromString(wrapper.getCorrelationId()) : null)
            .causationId(wrapper.getCausationId() != null ? UUID.fromString(wrapper.getCausationId()) : null)
            .headers(wrapper.getHeaders())
            .build();
    }
    
    @SuppressWarnings("unchecked")
    private T convertStringToEventType(String eventTypeString) {
        if (eventTypeClass.isEnum()) {
            for (T enumConstant : eventTypeClass.getEnumConstants()) {
                if (enumConstant.toString().equals(eventTypeString)) {
                    return enumConstant;
                }
            }
            throw new IllegalArgumentException("Unknown event type: " + eventTypeString);
        } else {
            // For non-enum types, try to create instance (simplified)
            try {
                return (T) eventTypeString;
            } catch (Exception e) {
                throw new RuntimeException("Cannot convert string to event type: " + eventTypeString, e);
            }
        }
    }
    
    private Object createKafkaConsumer(String groupId) {
        try {
            Properties props = config.getConsumerProperties();
            props.put("group.id", groupId);
            
            // Try to create KafkaConsumer using reflection
            Class<?> consumerClass = Class.forName("org.apache.kafka.clients.consumer.KafkaConsumer");
            return consumerClass.getConstructor(Properties.class).newInstance(props);
        } catch (Exception e) {
            LOGGER.warning("Kafka not available, using mock consumer: " + e.getMessage());
            return new MockKafkaConsumer();
        }
    }
    
    private void subscribeToTopics(Object consumer, List<String> topics) {
        if (consumer instanceof MockKafkaConsumer) {
            ((MockKafkaConsumer) consumer).subscribe(topics);
        } else {
            try {
                consumer.getClass().getMethod("subscribe", Collection.class).invoke(consumer, topics);
            } catch (Exception e) {
                throw new RuntimeException("Failed to subscribe to topics", e);
            }
        }
    }
    
    private Object pollRecords(Object consumer, Duration timeout) {
        if (consumer instanceof MockKafkaConsumer) {
            return ((MockKafkaConsumer) consumer).poll(timeout);
        } else {
            try {
                Class<?> durationClass = Class.forName("java.time.Duration");
                return consumer.getClass().getMethod("poll", durationClass).invoke(consumer, timeout);
            } catch (Exception e) {
                throw new RuntimeException("Failed to poll records", e);
            }
        }
    }
    
    private void commitOffsets(Object consumer) {
        if (consumer instanceof MockKafkaConsumer) {
            ((MockKafkaConsumer) consumer).commitSync();
        } else {
            try {
                consumer.getClass().getMethod("commitSync").invoke(consumer);
            } catch (Exception e) {
                LOGGER.warning("Failed to commit offsets: " + e.getMessage());
            }
        }
    }
    
    /**
     * Mock Kafka consumer for testing when Kafka is not available
     */
    private static class MockKafkaConsumer {
        public void subscribe(Collection<String> topics) {
            LOGGER.info("Mock Kafka consumer subscribed to: " + topics);
        }
        
        public MockConsumerRecords poll(Duration timeout) {
            // Return empty records for mock
            return new MockConsumerRecords();
        }
        
        public void commitSync() {
            // Mock commit
        }
    }
    
    private static class MockConsumerRecords implements Iterable<MockConsumerRecord> {
        private final List<MockConsumerRecord> records = new ArrayList<>();
        
        @Override
        public Iterator<MockConsumerRecord> iterator() {
            return records.iterator();
        }
    }
    
    private static class MockConsumerRecord {
        private final String topic;
        private final String value;
        
        public MockConsumerRecord(String topic, String value) {
            this.topic = topic;
            this.value = value;
        }
        
        public String topic() { return topic; }
        public String value() { return value; }
    }
}
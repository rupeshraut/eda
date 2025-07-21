package eda.eventbus.kafka;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration for Kafka-integrated event bus
 */
public class KafkaEventBusConfig {
    private final Properties producerProperties;
    private final Properties consumerProperties;
    private final String topicPrefix;
    private final int numPartitions;
    private final short replicationFactor;
    private final Duration publishTimeout;
    private final boolean enableIdempotence;
    private final String serializerClass;
    private final String deserializerClass;
    private final Map<String, Object> additionalProperties;
    
    private KafkaEventBusConfig(Builder builder) {
        this.producerProperties = new Properties();
        this.producerProperties.putAll(builder.producerProperties);
        
        this.consumerProperties = new Properties();
        this.consumerProperties.putAll(builder.consumerProperties);
        
        this.topicPrefix = builder.topicPrefix;
        this.numPartitions = builder.numPartitions;
        this.replicationFactor = builder.replicationFactor;
        this.publishTimeout = builder.publishTimeout;
        this.enableIdempotence = builder.enableIdempotence;
        this.serializerClass = builder.serializerClass;
        this.deserializerClass = builder.deserializerClass;
        this.additionalProperties = Map.copyOf(builder.additionalProperties);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public Properties getProducerProperties() { return new Properties(producerProperties); }
    public Properties getConsumerProperties() { return new Properties(consumerProperties); }
    public String getTopicPrefix() { return topicPrefix; }
    public int getNumPartitions() { return numPartitions; }
    public short getReplicationFactor() { return replicationFactor; }
    public Duration getPublishTimeout() { return publishTimeout; }
    public boolean isIdempotenceEnabled() { return enableIdempotence; }
    public String getSerializerClass() { return serializerClass; }
    public String getDeserializerClass() { return deserializerClass; }
    public Map<String, Object> getAdditionalProperties() { return additionalProperties; }
    
    // Convenience method
    public String getBootstrapServers() {
        return producerProperties.getProperty("bootstrap.servers", "localhost:9092");
    }
    
    public static class Builder {
        private Properties producerProperties = new Properties();
        private Properties consumerProperties = new Properties();
        private String topicPrefix = "eventbus";
        private int numPartitions = 3;
        private short replicationFactor = 1;
        private Duration publishTimeout = Duration.ofSeconds(30);
        private boolean enableIdempotence = true;
        private String serializerClass = "org.apache.kafka.common.serialization.StringSerializer";
        private String deserializerClass = "org.apache.kafka.common.serialization.StringDeserializer";
        private Map<String, Object> additionalProperties = Map.of();
        
        public Builder bootstrapServers(String bootstrapServers) {
            producerProperties.put("bootstrap.servers", bootstrapServers);
            consumerProperties.put("bootstrap.servers", bootstrapServers);
            return this;
        }
        
        public Builder producerProperty(String key, Object value) {
            producerProperties.put(key, value);
            return this;
        }
        
        public Builder consumerProperty(String key, Object value) {
            consumerProperties.put(key, value);
            return this;
        }
        
        public Builder topicPrefix(String topicPrefix) {
            this.topicPrefix = topicPrefix;
            return this;
        }
        
        public Builder numPartitions(int numPartitions) {
            this.numPartitions = numPartitions;
            return this;
        }
        
        public Builder replicationFactor(short replicationFactor) {
            this.replicationFactor = replicationFactor;
            return this;
        }
        
        public Builder publishTimeout(Duration publishTimeout) {
            this.publishTimeout = publishTimeout;
            return this;
        }
        
        public Builder enableIdempotence(boolean enableIdempotence) {
            this.enableIdempotence = enableIdempotence;
            return this;
        }
        
        public Builder serializer(String serializerClass) {
            this.serializerClass = serializerClass;
            return this;
        }
        
        public Builder deserializer(String deserializerClass) {
            this.deserializerClass = deserializerClass;
            return this;
        }
        
        public Builder additionalProperties(Map<String, Object> properties) {
            this.additionalProperties = Map.copyOf(properties);
            return this;
        }
        
        public Builder configureDefaults() {
            // Producer defaults
            producerProperty("key.serializer", serializerClass);
            producerProperty("value.serializer", serializerClass);
            producerProperty("acks", "all");
            producerProperty("retries", Integer.MAX_VALUE);
            producerProperty("max.in.flight.requests.per.connection", 5);
            producerProperty("enable.idempotence", enableIdempotence);
            producerProperty("compression.type", "snappy");
            producerProperty("linger.ms", 5);
            producerProperty("batch.size", 16384);
            
            // Consumer defaults
            consumerProperty("key.deserializer", deserializerClass);
            consumerProperty("value.deserializer", deserializerClass);
            consumerProperty("auto.offset.reset", "earliest");
            consumerProperty("enable.auto.commit", false);
            consumerProperty("max.poll.records", 500);
            consumerProperty("fetch.min.bytes", 1024);
            consumerProperty("fetch.max.wait.ms", 500);
            
            return this;
        }
        
        public KafkaEventBusConfig build() {
            configureDefaults();
            return new KafkaEventBusConfig(this);
        }
    }
}
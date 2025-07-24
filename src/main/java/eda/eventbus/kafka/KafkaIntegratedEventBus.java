package eda.eventbus.kafka;

import eda.eventbus.EventBusConfig;
import eda.eventbus.GenericEventBus;
import eda.eventbus.core.GenericEvent;
import eda.eventbus.kafka.monitoring.KafkaMetricsCollector;
import eda.eventbus.kafka.monitoring.AlertManager;
import eda.eventbus.kafka.monitoring.KafkaHealthStatus;
import eda.eventbus.kafka.monitoring.KafkaMetricsSnapshot;
import eda.eventbus.subscription.EventConsumer;
import eda.eventbus.subscription.EventSubscription;
import eda.eventbus.subscription.SubscriptionOptions;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Event bus that integrates with Kafka for distributed event processing
 */
public class KafkaIntegratedEventBus<T> extends GenericEventBus<T> {
    private static final Logger LOGGER = Logger.getLogger(KafkaIntegratedEventBus.class.getName());
    
    private final KafkaEventBusConfig kafkaConfig;
    private final KafkaEventPublisher kafkaPublisher;
    private final KafkaEventConsumer<T> kafkaConsumer;
    private final KafkaMetricsCollector metricsCollector;
    private final ConcurrentHashMap<String, String> topicMappings = new ConcurrentHashMap<>();
    private final boolean publishToKafka;
    private final boolean consumeFromKafka;
    
    public KafkaIntegratedEventBus(EventBusConfig eventBusConfig, KafkaEventBusConfig kafkaConfig, 
                                  Class<T> eventTypeClass) {
        this(eventBusConfig, kafkaConfig, eventTypeClass, true, true);
    }
    
    public KafkaIntegratedEventBus(EventBusConfig eventBusConfig, KafkaEventBusConfig kafkaConfig, 
                                  Class<T> eventTypeClass, boolean publishToKafka, boolean consumeFromKafka) {
        super(eventBusConfig);
        this.kafkaConfig = kafkaConfig;
        this.publishToKafka = publishToKafka;
        this.consumeFromKafka = consumeFromKafka;
        
        // Initialize monitoring
        this.metricsCollector = new KafkaMetricsCollector()
            .enableMetrics()
            .enableLagMonitoring()
            .enableThroughputTracking()
            .enableErrorTracking();
        
        this.kafkaPublisher = publishToKafka ? new KafkaEventPublisher(kafkaConfig) : null;
        this.kafkaConsumer = consumeFromKafka ? new KafkaEventConsumer<>(kafkaConfig, this, eventTypeClass) : null;
        
        LOGGER.info("Kafka integrated event bus initialized with monitoring - Publish: " + publishToKafka + ", Consume: " + consumeFromKafka);
    }
    
    /**
     * Start Kafka consumer with specified group ID and topics
     */
    public CompletableFuture<Void> startKafkaConsumer(String groupId, String... topics) {
        if (kafkaConsumer != null) {
            return kafkaConsumer.start(groupId, topics);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Stop Kafka consumer
     */
    public CompletableFuture<Void> stopKafkaConsumer() {
        if (kafkaConsumer != null) {
            return kafkaConsumer.stop();
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Map an event type to a specific Kafka topic
     */
    public void mapEventTypeToTopic(T eventType, String topicName) {
        topicMappings.put(eventType.toString(), topicName);
        LOGGER.info("Mapped event type " + eventType + " to Kafka topic: " + topicName);
    }
    
    /**
     * Publish event to both local subscribers and Kafka
     */
    @Override
    public <D> CompletableFuture<Void> publish(GenericEvent<T, D> event) {
        // Publish to local subscribers
        CompletableFuture<Void> localPublish = super.publish(event);
        
        // Publish to Kafka if enabled
        if (publishToKafka && kafkaPublisher != null) {
            CompletableFuture<Void> kafkaPublish = kafkaPublisher.publish(event)
                .exceptionally(throwable -> {
                    LOGGER.warning("Failed to publish to Kafka: " + throwable.getMessage());
                    return null;
                });
            
            return CompletableFuture.allOf(localPublish, kafkaPublish);
        }
        
        return localPublish;
    }
    
    /**
     * Subscribe to events locally (does not affect Kafka consumption)
     */
    public <D> EventSubscription<T, D> subscribeLocal(T eventType, EventConsumer<T, D> consumer, SubscriptionOptions options) {
        return super.subscribe(eventType, consumer, options);
    }
    
    /**
     * Subscribe to events locally with default options
     */
    public <D> EventSubscription<T, D> subscribeLocal(T eventType, EventConsumer<T, D> consumer) {
        return super.subscribe(eventType, consumer);
    }
    
    /**
     * Publish only to Kafka (skip local subscribers)
     */
    public <D> CompletableFuture<Void> publishToKafkaOnly(GenericEvent<T, D> event) {
        if (publishToKafka && kafkaPublisher != null) {
            return kafkaPublisher.publish(event);
        }
        return CompletableFuture.failedFuture(new IllegalStateException("Kafka publishing not enabled"));
    }
    
    /**
     * Publish only to local subscribers (skip Kafka)
     */
    public <D> CompletableFuture<Void> publishLocalOnly(GenericEvent<T, D> event) {
        return super.publish(event);
    }
    
    /**
     * Get Kafka configuration
     */
    public KafkaEventBusConfig getKafkaConfig() {
        return kafkaConfig;
    }
    
    /**
     * Check if Kafka publishing is enabled
     */
    public boolean isKafkaPublishingEnabled() {
        return publishToKafka && kafkaPublisher != null;
    }
    
    /**
     * Check if Kafka consuming is enabled
     */
    public boolean isKafkaConsumingEnabled() {
        return consumeFromKafka && kafkaConsumer != null;
    }
    
    /**
     * Get metrics collector for monitoring
     */
    public KafkaMetricsCollector getMetricsCollector() {
        return metricsCollector;
    }
    
    /**
     * Get alert manager for alert configuration
     */
    public AlertManager getAlertManager() {
        return metricsCollector.getAlertManager();
    }
    
    /**
     * Get current Kafka metrics snapshot
     */
    public CompletableFuture<KafkaMetricsSnapshot> getMetricsSnapshot() {
        return metricsCollector.getMetricsSnapshot();
    }
    
    /**
     * Get Kafka health status
     */
    public CompletableFuture<KafkaHealthStatus> getHealthStatus() {
        return metricsCollector.getHealthStatus();
    }
    
    /**
     * Export metrics in Prometheus format
     */
    public CompletableFuture<String> exportPrometheusMetrics() {
        return metricsCollector.exportPrometheusMetrics();
    }
    
    /**
     * Export metrics in JSON format
     */
    public CompletableFuture<String> exportJsonMetrics() {
        return metricsCollector.exportJsonMetrics();
    }
    
    /**
     * Record connection status for monitoring
     */
    public void recordConnectionStatus(String brokerId, boolean connected) {
        metricsCollector.recordConnectionStatus(brokerId, connected);
    }
    
    /**
     * Configure lag alert monitoring
     */
    public KafkaIntegratedEventBus<T> configureConsumerLagAlert(Duration threshold, 
            java.util.function.Consumer<eda.eventbus.kafka.monitoring.AlertTypes.ConsumerLagAlert> handler) {
        metricsCollector.onConsumerLag(threshold, handler);
        return this;
    }
    
    /**
     * Configure error rate alert monitoring
     */
    public KafkaIntegratedEventBus<T> configureErrorRateAlert(double threshold, 
            java.util.function.Consumer<eda.eventbus.kafka.monitoring.AlertTypes.ErrorRateAlert> handler) {
        metricsCollector.onErrorRate(threshold, handler);
        return this;
    }
    
    /**
     * Configure throughput change alert monitoring
     */
    public KafkaIntegratedEventBus<T> configureThroughputAlert(double thresholdPercent, 
            java.util.function.Consumer<eda.eventbus.kafka.monitoring.AlertTypes.ThroughputAlert> handler) {
        metricsCollector.onThroughputChange(thresholdPercent, handler);
        return this;
    }
    
    /**
     * Graceful shutdown including Kafka components
     */
    @Override
    public void shutdown() {
        LOGGER.info("Shutting down Kafka integrated event bus...");
        
        // Stop Kafka consumer
        if (kafkaConsumer != null) {
            kafkaConsumer.stop().join();
        }
        
        // Shutdown Kafka publisher
        if (kafkaPublisher != null) {
            kafkaPublisher.shutdown();
        }
        
        // Shutdown metrics collector
        if (metricsCollector != null) {
            metricsCollector.shutdown();
        }
        
        // Shutdown local event bus
        super.shutdown();
        
        LOGGER.info("Kafka integrated event bus shutdown complete");
    }
    
    /**
     * Create a builder for Kafka integrated event bus
     */
    public static <T> Builder<T> builder(Class<T> eventTypeClass) {
        return new Builder<>(eventTypeClass);
    }
    
    public static class Builder<T> {
        private final Class<T> eventTypeClass;
        private EventBusConfig eventBusConfig = EventBusConfig.builder().build();
        private KafkaEventBusConfig kafkaConfig;
        private boolean publishToKafka = true;
        private boolean consumeFromKafka = true;
        
        public Builder(Class<T> eventTypeClass) {
            this.eventTypeClass = eventTypeClass;
        }
        
        public Builder<T> eventBusConfig(EventBusConfig config) {
            this.eventBusConfig = config;
            return this;
        }
        
        public Builder<T> kafkaConfig(KafkaEventBusConfig config) {
            this.kafkaConfig = config;
            return this;
        }
        
        public Builder<T> publishToKafka(boolean publishToKafka) {
            this.publishToKafka = publishToKafka;
            return this;
        }
        
        public Builder<T> consumeFromKafka(boolean consumeFromKafka) {
            this.consumeFromKafka = consumeFromKafka;
            return this;
        }
        
        public KafkaIntegratedEventBus<T> build() {
            if (kafkaConfig == null) {
                throw new IllegalArgumentException("Kafka config must be provided");
            }
            return new KafkaIntegratedEventBus<>(eventBusConfig, kafkaConfig, eventTypeClass, publishToKafka, consumeFromKafka);
        }
    }
}
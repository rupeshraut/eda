package eda.eventbus.kafka.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Comprehensive Kafka metrics collector for monitoring and observability
 */
public class KafkaMetricsCollector {
    private static final Logger LOGGER = Logger.getLogger(KafkaMetricsCollector.class.getName());
    
    private final Map<String, TopicMetrics> topicMetrics = new ConcurrentHashMap<>();
    private final Map<String, ConsumerGroupMetrics> consumerGroupMetrics = new ConcurrentHashMap<>();
    private final Map<String, ProducerMetrics> producerMetrics = new ConcurrentHashMap<>();
    private final ConnectionMetrics connectionMetrics = new ConnectionMetrics();
    private final ScheduledExecutorService metricsScheduler = Executors.newScheduledThreadPool(2);
    
    private final AlertManager alertManager = new AlertManager();
    private final Map<String, Consumer<AlertTypes.ConsumerLagAlert>> lagAlertHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<AlertTypes.ThroughputAlert>> throughputAlertHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<AlertTypes.ErrorRateAlert>> errorAlertHandlers = new ConcurrentHashMap<>();
    
    private volatile boolean metricsEnabled = false;
    private volatile boolean lagMonitoringEnabled = false;
    private volatile boolean throughputTrackingEnabled = false;
    private volatile boolean errorTrackingEnabled = false;
    
    /**
     * Enable comprehensive metrics collection
     */
    public KafkaMetricsCollector enableMetrics() {
        if (!metricsEnabled) {
            metricsEnabled = true;
            startMetricsCollection();
            LOGGER.info("Kafka metrics collection enabled");
        }
        return this;
    }
    
    /**
     * Enable consumer lag monitoring with configurable thresholds
     */
    public KafkaMetricsCollector enableLagMonitoring() {
        lagMonitoringEnabled = true;
        startLagMonitoring();
        LOGGER.info("Kafka consumer lag monitoring enabled");
        return this;
    }
    
    /**
     * Enable throughput tracking and monitoring
     */
    public KafkaMetricsCollector enableThroughputTracking() {
        throughputTrackingEnabled = true;
        startThroughputMonitoring();
        LOGGER.info("Kafka throughput tracking enabled");
        return this;
    }
    
    /**
     * Enable error rate tracking
     */
    public KafkaMetricsCollector enableErrorTracking() {
        errorTrackingEnabled = true;
        LOGGER.info("Kafka error rate tracking enabled");
        return this;
    }
    
    /**
     * Record message published to topic
     */
    public void recordMessagePublished(String topic, int messageSize, Duration publishTime) {
        if (!metricsEnabled) return;
        
        TopicMetrics metrics = topicMetrics.computeIfAbsent(topic, k -> new TopicMetrics(topic));
        metrics.recordPublished(messageSize, publishTime);
    }
    
    /**
     * Record message consumed from topic
     */
    public void recordMessageConsumed(String topic, String consumerGroup, int messageSize, Duration processTime) {
        if (!metricsEnabled) return;
        
        TopicMetrics topicMetrics = this.topicMetrics.computeIfAbsent(topic, k -> new TopicMetrics(topic));
        topicMetrics.recordConsumed(messageSize, processTime);
        
        ConsumerGroupMetrics consumerMetrics = consumerGroupMetrics.computeIfAbsent(
            consumerGroup, k -> new ConsumerGroupMetrics(consumerGroup));
        consumerMetrics.recordConsumed(topic, messageSize, processTime);
    }
    
    /**
     * Record consumer lag for a topic partition
     */
    public void recordConsumerLag(String consumerGroup, String topic, int partition, long lag) {
        if (!lagMonitoringEnabled) return;
        
        ConsumerGroupMetrics metrics = consumerGroupMetrics.computeIfAbsent(
            consumerGroup, k -> new ConsumerGroupMetrics(consumerGroup));
        metrics.recordLag(topic, partition, lag);
        
        // Check for lag alerts
        checkLagAlerts(consumerGroup, topic, partition, lag);
    }
    
    /**
     * Record producer error
     */
    public void recordProducerError(String topic, String errorType, Throwable error) {
        if (!errorTrackingEnabled) return;
        
        TopicMetrics metrics = topicMetrics.computeIfAbsent(topic, k -> new TopicMetrics(topic));
        metrics.recordError(errorType);
        
        // Check for error rate alerts
        checkErrorRateAlerts(topic, errorType, error);
    }
    
    /**
     * Record consumer error
     */
    public void recordConsumerError(String consumerGroup, String topic, String errorType, Throwable error) {
        if (!errorTrackingEnabled) return;
        
        ConsumerGroupMetrics metrics = consumerGroupMetrics.computeIfAbsent(
            consumerGroup, k -> new ConsumerGroupMetrics(consumerGroup));
        metrics.recordError(topic, errorType);
        
        // Check for error rate alerts
        checkErrorRateAlerts(consumerGroup + ":" + topic, errorType, error);
    }
    
    /**
     * Record connection status
     */
    public void recordConnectionStatus(String brokerId, boolean connected) {
        connectionMetrics.recordConnectionStatus(brokerId, connected);
    }
    
    /**
     * Get current metrics snapshot
     */
    public CompletableFuture<KafkaMetricsSnapshot> getMetricsSnapshot() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, TopicMetrics> topicSnapshot = Map.copyOf(topicMetrics);
            Map<String, ConsumerGroupMetrics> consumerSnapshot = Map.copyOf(consumerGroupMetrics);
            Map<String, ProducerMetrics> producerSnapshot = Map.copyOf(producerMetrics);
            
            return new KafkaMetricsSnapshot(
                topicSnapshot,
                consumerSnapshot,
                producerSnapshot,
                connectionMetrics.getSnapshot(),
                Instant.now()
            );
        });
    }
    
    /**
     * Export metrics in Prometheus format
     */
    public CompletableFuture<String> exportPrometheusMetrics() {
        return getMetricsSnapshot().thenApply(this::formatPrometheusMetrics);
    }
    
    /**
     * Export metrics in JSON format
     */
    public CompletableFuture<String> exportJsonMetrics() {
        return getMetricsSnapshot().thenApply(this::formatJsonMetrics);
    }
    
    /**
     * Register consumer lag alert handler
     */
    public KafkaMetricsCollector onConsumerLag(Duration threshold, Consumer<AlertTypes.ConsumerLagAlert> handler) {
        String key = threshold.toString();
        lagAlertHandlers.put(key, handler);
        return this;
    }
    
    /**
     * Register throughput alert handler
     */
    public KafkaMetricsCollector onThroughputChange(double thresholdPercent, Consumer<AlertTypes.ThroughputAlert> handler) {
        String key = String.valueOf(thresholdPercent);
        throughputAlertHandlers.put(key, handler);
        return this;
    }
    
    /**
     * Register error rate alert handler
     */
    public KafkaMetricsCollector onErrorRate(double threshold, Consumer<AlertTypes.ErrorRateAlert> handler) {
        String key = String.valueOf(threshold);
        errorAlertHandlers.put(key, handler);
        return this;
    }
    
    /**
     * Get health status of Kafka integration
     */
    public CompletableFuture<KafkaHealthStatus> getHealthStatus() {
        return getMetricsSnapshot().thenApply(snapshot -> {
            Map<String, KafkaHealthStatus.ComponentHealth> componentHealth = new ConcurrentHashMap<>();
            List<String> healthIssues = new ArrayList<>();
            
            // Check connection health
            KafkaHealthStatus.HealthLevel connectionLevel = connectionMetrics.isHealthy() 
                ? KafkaHealthStatus.HealthLevel.HEALTHY 
                : KafkaHealthStatus.HealthLevel.UNHEALTHY;
            componentHealth.put("connections", new KafkaHealthStatus.ComponentHealth(
                "connections", connectionLevel, 
                String.format("Connected brokers: %d/%d", connectionMetrics.getConnectedBrokerCount(), connectionMetrics.getTotalBrokerCount()),
                connectionLevel == KafkaHealthStatus.HealthLevel.UNHEALTHY ? List.of("Connection issues detected") : List.of(),
                Map.of("connectedBrokers", connectionMetrics.getConnectedBrokerCount(), "totalBrokers", connectionMetrics.getTotalBrokerCount())
            ));
            
            // Check consumer group health
            long healthyGroups = snapshot.getHealthyConsumerGroupCount();
            long totalGroups = snapshot.getConsumerGroupMetrics().size();
            KafkaHealthStatus.HealthLevel consumerLevel = (healthyGroups == totalGroups) 
                ? KafkaHealthStatus.HealthLevel.HEALTHY 
                : (healthyGroups > totalGroups / 2) 
                    ? KafkaHealthStatus.HealthLevel.DEGRADED 
                    : KafkaHealthStatus.HealthLevel.UNHEALTHY;
            
            componentHealth.put("consumers", new KafkaHealthStatus.ComponentHealth(
                "consumers", consumerLevel,
                String.format("Healthy consumer groups: %d/%d", healthyGroups, totalGroups),
                consumerLevel != KafkaHealthStatus.HealthLevel.HEALTHY ? List.of("Some consumer groups have issues") : List.of(),
                Map.of("healthyGroups", healthyGroups, "totalGroups", totalGroups, "totalLag", snapshot.getTotalConsumerLag())
            ));
            
            // Check topic health
            long topicsWithErrors = snapshot.getTopicsWithHighErrorRate();
            long totalTopics = snapshot.getTopicMetrics().size();
            KafkaHealthStatus.HealthLevel topicLevel = (topicsWithErrors == 0) 
                ? KafkaHealthStatus.HealthLevel.HEALTHY 
                : (topicsWithErrors < totalTopics / 2) 
                    ? KafkaHealthStatus.HealthLevel.DEGRADED 
                    : KafkaHealthStatus.HealthLevel.UNHEALTHY;
                    
            componentHealth.put("topics", new KafkaHealthStatus.ComponentHealth(
                "topics", topicLevel,
                String.format("Topics with high error rates: %d/%d", topicsWithErrors, totalTopics),
                topicsWithErrors > 0 ? List.of("Some topics have high error rates") : List.of(),
                Map.of("topicsWithErrors", topicsWithErrors, "totalTopics", totalTopics, "totalErrors", snapshot.getTotalErrorCount())
            ));
            
            // Determine overall health
            KafkaHealthStatus.HealthLevel overallHealth = determineOverallHealth(componentHealth.values());
            
            // Collect health issues
            componentHealth.values().forEach(health -> healthIssues.addAll(health.getIssues()));
            
            return new KafkaHealthStatus(overallHealth, componentHealth, healthIssues, snapshot);
        });
    }
    
    /**
     * Get alert manager for advanced alert configuration
     */
    public AlertManager getAlertManager() {
        return alertManager;
    }
    
    /**
     * Shutdown metrics collection
     */
    public void shutdown() {
        metricsEnabled = false;
        lagMonitoringEnabled = false;
        throughputTrackingEnabled = false;
        errorTrackingEnabled = false;
        
        alertManager.shutdown();
        
        metricsScheduler.shutdown();
        try {
            if (!metricsScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                metricsScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            metricsScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOGGER.info("Kafka metrics collector shutdown complete");
    }
    
    private void startMetricsCollection() {
        metricsScheduler.scheduleAtFixedRate(this::collectMetrics, 0, 30, TimeUnit.SECONDS);
    }
    
    private void startLagMonitoring() {
        metricsScheduler.scheduleAtFixedRate(this::monitorConsumerLag, 0, 10, TimeUnit.SECONDS);
    }
    
    private void startThroughputMonitoring() {
        metricsScheduler.scheduleAtFixedRate(this::monitorThroughput, 0, 60, TimeUnit.SECONDS);
    }
    
    private void collectMetrics() {
        // Update metrics from Kafka JMX or admin client
        // This would integrate with actual Kafka metrics APIs
        LOGGER.fine("Collecting Kafka metrics...");
    }
    
    private void monitorConsumerLag() {
        // Monitor consumer lag and trigger alerts
        LOGGER.fine("Monitoring consumer lag...");
    }
    
    private void monitorThroughput() {
        // Monitor throughput changes and trigger alerts
        LOGGER.fine("Monitoring throughput...");
    }
    
    private void checkLagAlerts(String consumerGroup, String topic, int partition, long lag) {
        Duration lagDuration = Duration.ofMillis(lag);
        
        // Create and process lag alert
        AlertTypes.ConsumerLagAlert lagAlert = new AlertTypes.ConsumerLagAlert(
            consumerGroup, topic, partition, lagDuration, Instant.now());
        
        // Send to alert manager
        alertManager.processConsumerLagAlert(lagAlert);
        
        // Call custom handlers
        lagAlertHandlers.forEach((threshold, handler) -> {
            Duration thresholdDuration = Duration.parse(threshold);
            if (lagDuration.compareTo(thresholdDuration) > 0) {
                handler.accept(lagAlert);
            }
        });
    }
    
    private void checkErrorRateAlerts(String identifier, String errorType, Throwable error) {
        // Calculate error rate from metrics
        double errorRate = 0.0;
        long totalErrors = 0;
        long totalMessages = 0;
        
        if (identifier.contains(":")) {
            // Consumer group:topic format
            String[] parts = identifier.split(":");
            String consumerGroup = parts[0];
            String topic = parts[1];
            
            ConsumerGroupMetrics metrics = consumerGroupMetrics.get(consumerGroup);
            if (metrics != null) {
                totalErrors = metrics.getErrorCount(topic, errorType);
                totalMessages = metrics.getConsumedCount(topic);
                if (totalMessages > 0) {
                    errorRate = (double) totalErrors / totalMessages;
                }
            }
        } else {
            // Topic format
            TopicMetrics metrics = topicMetrics.get(identifier);
            if (metrics != null) {
                totalErrors = metrics.getErrorCount(errorType);
                totalMessages = metrics.getPublishedCount();
                if (totalMessages > 0) {
                    errorRate = (double) totalErrors / totalMessages;
                }
            }
        }
        
        // Create and process error rate alert if threshold exceeded
        if (errorRate > 0.05) { // 5% error rate threshold
            AlertTypes.ErrorRateAlert errorAlert = new AlertTypes.ErrorRateAlert(
                identifier, errorType, errorRate, totalErrors, totalMessages, error, Instant.now());
            
            // Send to alert manager
            alertManager.processErrorRateAlert(errorAlert);
            
            // Call custom handlers
            final double finalErrorRate = errorRate;
            errorAlertHandlers.forEach((threshold, handler) -> {
                double thresholdValue = Double.parseDouble(threshold);
                if (finalErrorRate > thresholdValue) {
                    handler.accept(errorAlert);
                }
            });
        }
    }
    
    private KafkaHealthStatus.HealthLevel determineOverallHealth(Collection<KafkaHealthStatus.ComponentHealth> components) {
        if (components.isEmpty()) {
            return KafkaHealthStatus.HealthLevel.HEALTHY;
        }
        
        long healthyCount = components.stream()
            .mapToLong(health -> health.getLevel() == KafkaHealthStatus.HealthLevel.HEALTHY ? 1 : 0)
            .sum();
        long unhealthyCount = components.stream()
            .mapToLong(health -> health.getLevel() == KafkaHealthStatus.HealthLevel.UNHEALTHY ? 1 : 0)
            .sum();
        
        if (unhealthyCount > 0) {
            return KafkaHealthStatus.HealthLevel.UNHEALTHY;
        } else if (healthyCount == components.size()) {
            return KafkaHealthStatus.HealthLevel.HEALTHY;
        } else {
            return KafkaHealthStatus.HealthLevel.DEGRADED;
        }
    }
    
    private String formatPrometheusMetrics(KafkaMetricsSnapshot snapshot) {
        StringBuilder prometheus = new StringBuilder();
        
        // Topic metrics
        prometheus.append("# HELP kafka_topic_messages_published_total Total messages published to topic\n");
        prometheus.append("# TYPE kafka_topic_messages_published_total counter\n");
        snapshot.getTopicMetrics().forEach((topic, metrics) -> {
            prometheus.append("kafka_topic_messages_published_total{topic=\"").append(topic).append("\"} ")
                     .append(metrics.getPublishedCount()).append("\n");
        });
        
        // Consumer group metrics
        prometheus.append("# HELP kafka_consumer_lag_messages Consumer lag in messages\n");
        prometheus.append("# TYPE kafka_consumer_lag_messages gauge\n");
        snapshot.getConsumerGroupMetrics().forEach((group, metrics) -> {
            metrics.getTopicLags().forEach((topic, lag) -> {
                prometheus.append("kafka_consumer_lag_messages{consumer_group=\"").append(group)
                         .append("\",topic=\"").append(topic).append("\"} ").append(lag).append("\n");
            });
        });
        
        return prometheus.toString();
    }
    
    private String formatJsonMetrics(KafkaMetricsSnapshot snapshot) {
        // Simple JSON formatting - in production would use Jackson
        return String.format("""
            {
              "timestamp": "%s",
              "topics": %d,
              "consumer_groups": %d,
              "connection_healthy": %s
            }
            """,
            snapshot.getTimestamp(),
            snapshot.getTopicMetrics().size(),
            snapshot.getConsumerGroupMetrics().size(),
            snapshot.getConnectionMetrics().isHealthy()
        );
    }
}
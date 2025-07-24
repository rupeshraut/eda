package eda.eventbus.kafka.monitoring;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable snapshot of Kafka metrics at a specific point in time
 */
public class KafkaMetricsSnapshot {
    private final Map<String, TopicMetrics> topicMetrics;
    private final Map<String, ConsumerGroupMetrics> consumerGroupMetrics;
    private final Map<String, ProducerMetrics> producerMetrics;
    private final ConnectionMetrics connectionMetrics;
    private final Instant timestamp;
    
    public KafkaMetricsSnapshot(
            Map<String, TopicMetrics> topicMetrics,
            Map<String, ConsumerGroupMetrics> consumerGroupMetrics,
            Map<String, ProducerMetrics> producerMetrics,
            ConnectionMetrics connectionMetrics,
            Instant timestamp) {
        this.topicMetrics = Map.copyOf(topicMetrics);
        this.consumerGroupMetrics = Map.copyOf(consumerGroupMetrics);
        this.producerMetrics = Map.copyOf(producerMetrics);
        this.connectionMetrics = connectionMetrics;
        this.timestamp = timestamp;
    }
    
    /**
     * Get topic metrics
     */
    public Map<String, TopicMetrics> getTopicMetrics() {
        return topicMetrics;
    }
    
    /**
     * Get consumer group metrics
     */
    public Map<String, ConsumerGroupMetrics> getConsumerGroupMetrics() {
        return consumerGroupMetrics;
    }
    
    /**
     * Get producer metrics
     */
    public Map<String, ProducerMetrics> getProducerMetrics() {
        return producerMetrics;
    }
    
    /**
     * Get connection metrics
     */
    public ConnectionMetrics getConnectionMetrics() {
        return connectionMetrics;
    }
    
    /**
     * Get snapshot timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get total messages published across all topics
     */
    public long getTotalPublishedMessages() {
        return topicMetrics.values().stream()
            .mapToLong(TopicMetrics::getPublishedCount)
            .sum();
    }
    
    /**
     * Get total messages consumed across all consumer groups
     */
    public long getTotalConsumedMessages() {
        return consumerGroupMetrics.values().stream()
            .mapToLong(ConsumerGroupMetrics::getTotalConsumedCount)
            .sum();
    }
    
    /**
     * Get total bytes published across all topics
     */
    public long getTotalPublishedBytes() {
        return topicMetrics.values().stream()
            .mapToLong(TopicMetrics::getPublishedBytes)
            .sum();
    }
    
    /**
     * Get total bytes consumed across all consumer groups
     */
    public long getTotalConsumedBytes() {
        return consumerGroupMetrics.values().stream()
            .mapToLong(ConsumerGroupMetrics::getTotalConsumedBytes)
            .sum();
    }
    
    /**
     * Get total error count across all topics and consumer groups
     */
    public long getTotalErrorCount() {
        long topicErrors = topicMetrics.values().stream()
            .mapToLong(TopicMetrics::getTotalErrorCount)
            .sum();
        
        long consumerErrors = consumerGroupMetrics.values().stream()
            .mapToLong(ConsumerGroupMetrics::getTotalErrorCount)
            .sum();
        
        return topicErrors + consumerErrors;
    }
    
    /**
     * Get total consumer lag across all consumer groups
     */
    public long getTotalConsumerLag() {
        return consumerGroupMetrics.values().stream()
            .mapToLong(ConsumerGroupMetrics::getTotalLag)
            .sum();
    }
    
    /**
     * Get overall success rate
     */
    public double getOverallSuccessRate() {
        long totalMessages = getTotalPublishedMessages() + getTotalConsumedMessages();
        long totalErrors = getTotalErrorCount();
        
        if (totalMessages > 0) {
            return 1.0 - ((double) totalErrors / totalMessages);
        }
        return 1.0;
    }
    
    /**
     * Get number of healthy consumer groups
     */
    public long getHealthyConsumerGroupCount() {
        return consumerGroupMetrics.values().stream()
            .mapToLong(metrics -> metrics.isHealthy() ? 1 : 0)
            .sum();
    }
    
    /**
     * Get number of topics with high error rates
     */
    public long getTopicsWithHighErrorRate() {
        return topicMetrics.values().stream()
            .mapToLong(metrics -> metrics.hasHighErrorRate() ? 1 : 0)
            .sum();
    }
    
    /**
     * Get number of consumer groups with high lag
     */
    public long getConsumerGroupsWithHighLag() {
        return consumerGroupMetrics.values().stream()
            .mapToLong(metrics -> metrics.hasHighLag() ? 1 : 0)
            .sum();
    }
    
    /**
     * Check if overall system is healthy
     */
    public boolean isSystemHealthy() {
        return connectionMetrics.isHealthy() &&
               getTopicsWithHighErrorRate() == 0 &&
               getConsumerGroupsWithHighLag() == 0 &&
               getOverallSuccessRate() > 0.95;
    }
    
    /**
     * Get summary statistics
     */
    public MetricsSummary getSummary() {
        return new MetricsSummary(
            topicMetrics.size(),
            consumerGroupMetrics.size(),
            getTotalPublishedMessages(),
            getTotalConsumedMessages(),
            getTotalErrorCount(),
            getTotalConsumerLag(),
            getOverallSuccessRate(),
            isSystemHealthy(),
            timestamp
        );
    }
    
    @Override
    public String toString() {
        return String.format(
            "KafkaMetricsSnapshot{timestamp=%s, topics=%d, consumerGroups=%d, published=%d, consumed=%d, errors=%d, healthy=%s}",
            timestamp, topicMetrics.size(), consumerGroupMetrics.size(),
            getTotalPublishedMessages(), getTotalConsumedMessages(), getTotalErrorCount(), isSystemHealthy()
        );
    }
    
    /**
     * Summary statistics for the metrics snapshot
     */
    public static class MetricsSummary {
        private final int topicCount;
        private final int consumerGroupCount;
        private final long totalPublished;
        private final long totalConsumed;
        private final long totalErrors;
        private final long totalLag;
        private final double successRate;
        private final boolean healthy;
        private final Instant timestamp;
        
        public MetricsSummary(int topicCount, int consumerGroupCount, long totalPublished,
                            long totalConsumed, long totalErrors, long totalLag,
                            double successRate, boolean healthy, Instant timestamp) {
            this.topicCount = topicCount;
            this.consumerGroupCount = consumerGroupCount;
            this.totalPublished = totalPublished;
            this.totalConsumed = totalConsumed;
            this.totalErrors = totalErrors;
            this.totalLag = totalLag;
            this.successRate = successRate;
            this.healthy = healthy;
            this.timestamp = timestamp;
        }
        
        // Getters
        public int getTopicCount() { return topicCount; }
        public int getConsumerGroupCount() { return consumerGroupCount; }
        public long getTotalPublished() { return totalPublished; }
        public long getTotalConsumed() { return totalConsumed; }
        public long getTotalErrors() { return totalErrors; }
        public long getTotalLag() { return totalLag; }
        public double getSuccessRate() { return successRate; }
        public boolean isHealthy() { return healthy; }
        public Instant getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format(
                "MetricsSummary{topics=%d, consumerGroups=%d, published=%d, consumed=%d, " +
                "errors=%d, lag=%d, successRate=%.2f%%, healthy=%s}",
                topicCount, consumerGroupCount, totalPublished, totalConsumed,
                totalErrors, totalLag, successRate * 100, healthy
            );
        }
    }
}
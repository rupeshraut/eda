package eda.eventbus.kafka.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;

/**
 * Metrics for a specific Kafka consumer group
 */
public class ConsumerGroupMetrics {
    private final String consumerGroup;
    private final Instant createdAt;
    
    // Consumption metrics per topic
    private final ConcurrentHashMap<String, LongAdder> consumedCountPerTopic = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> consumedBytesPerTopic = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> processingTimePerTopic = new ConcurrentHashMap<>();
    
    // Lag metrics per topic/partition
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, AtomicLong>> lagPerTopicPartition = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> maxLagPerTopic = new ConcurrentHashMap<>();
    
    // Error metrics per topic
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, LongAdder>> errorCountsPerTopic = new ConcurrentHashMap<>();
    
    // Rebalancing metrics
    private final AtomicLong rebalanceCount = new AtomicLong();
    private final AtomicLong lastRebalanceTime = new AtomicLong();
    private final AtomicLong totalRebalanceTime = new AtomicLong();
    
    // Consumer health
    private final AtomicLong lastHeartbeat = new AtomicLong();
    private final AtomicLong heartbeatCount = new AtomicLong();
    
    public ConsumerGroupMetrics(String consumerGroup) {
        this.consumerGroup = consumerGroup;
        this.createdAt = Instant.now();
        this.lastHeartbeat.set(System.currentTimeMillis());
    }
    
    /**
     * Record a message consumed from a topic
     */
    public void recordConsumed(String topic, int messageSize, Duration processTime) {
        consumedCountPerTopic.computeIfAbsent(topic, k -> new LongAdder()).increment();
        consumedBytesPerTopic.computeIfAbsent(topic, k -> new LongAdder()).add(messageSize);
        processingTimePerTopic.computeIfAbsent(topic, k -> new AtomicLong()).addAndGet(processTime.toMillis());
    }
    
    /**
     * Record consumer lag for a topic partition
     */
    public void recordLag(String topic, int partition, long lag) {
        lagPerTopicPartition
            .computeIfAbsent(topic, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(partition, k -> new AtomicLong())
            .set(lag);
        
        // Update max lag for topic
        maxLagPerTopic.computeIfAbsent(topic, k -> new AtomicLong())
            .updateAndGet(current -> Math.max(current, lag));
    }
    
    /**
     * Record an error for a topic
     */
    public void recordError(String topic, String errorType) {
        errorCountsPerTopic
            .computeIfAbsent(topic, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(errorType, k -> new LongAdder())
            .increment();
    }
    
    /**
     * Record a rebalance event
     */
    public void recordRebalance(Duration rebalanceTime) {
        rebalanceCount.incrementAndGet();
        lastRebalanceTime.set(System.currentTimeMillis());
        totalRebalanceTime.addAndGet(rebalanceTime.toMillis());
    }
    
    /**
     * Record heartbeat
     */
    public void recordHeartbeat() {
        lastHeartbeat.set(System.currentTimeMillis());
        heartbeatCount.incrementAndGet();
    }
    
    /**
     * Get consumer group name
     */
    public String getConsumerGroup() {
        return consumerGroup;
    }
    
    /**
     * Get total messages consumed across all topics
     */
    public long getTotalConsumedCount() {
        return consumedCountPerTopic.values().stream().mapToLong(LongAdder::sum).sum();
    }
    
    /**
     * Get messages consumed for a specific topic
     */
    public long getConsumedCount(String topic) {
        LongAdder counter = consumedCountPerTopic.get(topic);
        return counter != null ? counter.sum() : 0;
    }
    
    /**
     * Get total bytes consumed across all topics
     */
    public long getTotalConsumedBytes() {
        return consumedBytesPerTopic.values().stream().mapToLong(LongAdder::sum).sum();
    }
    
    /**
     * Get bytes consumed for a specific topic
     */
    public long getConsumedBytes(String topic) {
        LongAdder counter = consumedBytesPerTopic.get(topic);
        return counter != null ? counter.sum() : 0;
    }
    
    /**
     * Get average processing time for a topic
     */
    public double getAverageProcessingTime(String topic) {
        AtomicLong totalTime = processingTimePerTopic.get(topic);
        long count = getConsumedCount(topic);
        
        if (totalTime != null && count > 0) {
            return (double) totalTime.get() / count;
        }
        return 0.0;
    }
    
    /**
     * Get current lag for a topic partition
     */
    public long getLag(String topic, int partition) {
        ConcurrentHashMap<Integer, AtomicLong> topicLags = lagPerTopicPartition.get(topic);
        if (topicLags != null) {
            AtomicLong lag = topicLags.get(partition);
            return lag != null ? lag.get() : 0;
        }
        return 0;
    }
    
    /**
     * Get maximum lag for a topic across all partitions
     */
    public long getMaxLag(String topic) {
        AtomicLong maxLag = maxLagPerTopic.get(topic);
        return maxLag != null ? maxLag.get() : 0;
    }
    
    /**
     * Get total lag across all topics and partitions
     */
    public long getTotalLag() {
        return lagPerTopicPartition.values().stream()
            .flatMap(partitionLags -> partitionLags.values().stream())
            .mapToLong(AtomicLong::get)
            .sum();
    }
    
    /**
     * Get lag information for all topics
     */
    public Map<String, Long> getTopicLags() {
        Map<String, Long> lags = new ConcurrentHashMap<>();
        maxLagPerTopic.forEach((topic, lag) -> lags.put(topic, lag.get()));
        return lags;
    }
    
    /**
     * Check if consumer group has high lag (>1000 messages)
     */
    public boolean hasHighLag() {
        return getTotalLag() > 1000;
    }
    
    /**
     * Check if consumer group has high lag for a specific topic
     */
    public boolean hasHighLag(String topic, long threshold) {
        return getMaxLag(topic) > threshold;
    }
    
    /**
     * Get total error count across all topics
     */
    public long getTotalErrorCount() {
        return errorCountsPerTopic.values().stream()
            .flatMap(topicErrors -> topicErrors.values().stream())
            .mapToLong(LongAdder::sum)
            .sum();
    }
    
    /**
     * Get error count for a specific topic and error type
     */
    public long getErrorCount(String topic, String errorType) {
        ConcurrentHashMap<String, LongAdder> topicErrors = errorCountsPerTopic.get(topic);
        if (topicErrors != null) {
            LongAdder counter = topicErrors.get(errorType);
            return counter != null ? counter.sum() : 0;
        }
        return 0;
    }
    
    /**
     * Get rebalance count
     */
    public long getRebalanceCount() {
        return rebalanceCount.get();
    }
    
    /**
     * Get last rebalance time
     */
    public Instant getLastRebalanceTime() {
        long timestamp = lastRebalanceTime.get();
        return timestamp > 0 ? Instant.ofEpochMilli(timestamp) : null;
    }
    
    /**
     * Get average rebalance time
     */
    public double getAverageRebalanceTime() {
        long count = getRebalanceCount();
        return count > 0 ? (double) totalRebalanceTime.get() / count : 0.0;
    }
    
    /**
     * Check if consumer is healthy (recent heartbeat)
     */
    public boolean isHealthy() {
        long now = System.currentTimeMillis();
        long lastHeartbeatTime = lastHeartbeat.get();
        
        // Consider healthy if heartbeat within last 30 seconds
        return (now - lastHeartbeatTime) < 30000;
    }
    
    /**
     * Get time since last heartbeat
     */
    public Duration getTimeSinceLastHeartbeat() {
        long now = System.currentTimeMillis();
        long lastHeartbeatTime = lastHeartbeat.get();
        return Duration.ofMillis(now - lastHeartbeatTime);
    }
    
    /**
     * Get creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Get consumer throughput (messages per second)
     */
    public double getThroughput() {
        long now = System.currentTimeMillis();
        long elapsedMs = now - createdAt.toEpochMilli();
        
        if (elapsedMs > 0) {
            return (double) getTotalConsumedCount() * 1000 / elapsedMs;
        }
        return 0.0;
    }
    
    /**
     * Reset metrics
     */
    public void reset() {
        consumedCountPerTopic.clear();
        consumedBytesPerTopic.clear();
        processingTimePerTopic.clear();
        lagPerTopicPartition.clear();
        maxLagPerTopic.clear();
        errorCountsPerTopic.clear();
        
        rebalanceCount.set(0);
        lastRebalanceTime.set(0);
        totalRebalanceTime.set(0);
        
        lastHeartbeat.set(System.currentTimeMillis());
        heartbeatCount.set(0);
    }
    
    @Override
    public String toString() {
        return String.format(
            "ConsumerGroupMetrics{group='%s', consumed=%d, totalLag=%d, errors=%d, healthy=%s}",
            consumerGroup, getTotalConsumedCount(), getTotalLag(), getTotalErrorCount(), isHealthy()
        );
    }
}
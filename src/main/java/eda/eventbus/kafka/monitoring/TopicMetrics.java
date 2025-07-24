package eda.eventbus.kafka.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics for a specific Kafka topic
 */
public class TopicMetrics {
    private final String topic;
    private final Instant createdAt;
    
    // Publishing metrics
    private final LongAdder publishedCount = new LongAdder();
    private final LongAdder publishedBytes = new LongAdder();
    private final AtomicLong totalPublishTime = new AtomicLong();
    private final AtomicLong maxPublishTime = new AtomicLong();
    private final AtomicLong minPublishTime = new AtomicLong(Long.MAX_VALUE);
    
    // Consuming metrics
    private final LongAdder consumedCount = new LongAdder();
    private final LongAdder consumedBytes = new LongAdder();
    private final AtomicLong totalConsumeTime = new AtomicLong();
    private final AtomicLong maxConsumeTime = new AtomicLong();
    private final AtomicLong minConsumeTime = new AtomicLong(Long.MAX_VALUE);
    
    // Error metrics
    private final ConcurrentHashMap<String, LongAdder> errorCounts = new ConcurrentHashMap<>();
    private final AtomicLong lastErrorTime = new AtomicLong();
    
    // Throughput tracking
    private final AtomicLong lastThroughputCheck = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastPublishedCount = new AtomicLong();
    private final AtomicLong lastConsumedCount = new AtomicLong();
    
    public TopicMetrics(String topic) {
        this.topic = topic;
        this.createdAt = Instant.now();
    }
    
    /**
     * Record a message published to this topic
     */
    public void recordPublished(int messageSize, Duration publishTime) {
        publishedCount.increment();
        publishedBytes.add(messageSize);
        
        long publishTimeMs = publishTime.toMillis();
        totalPublishTime.addAndGet(publishTimeMs);
        
        // Update min/max publish times
        updateMinTime(minPublishTime, publishTimeMs);
        updateMaxTime(maxPublishTime, publishTimeMs);
    }
    
    /**
     * Record a message consumed from this topic
     */
    public void recordConsumed(int messageSize, Duration consumeTime) {
        consumedCount.increment();
        consumedBytes.add(messageSize);
        
        long consumeTimeMs = consumeTime.toMillis();
        totalConsumeTime.addAndGet(consumeTimeMs);
        
        // Update min/max consume times
        updateMinTime(minConsumeTime, consumeTimeMs);
        updateMaxTime(maxConsumeTime, consumeTimeMs);
    }
    
    /**
     * Record an error for this topic
     */
    public void recordError(String errorType) {
        errorCounts.computeIfAbsent(errorType, k -> new LongAdder()).increment();
        lastErrorTime.set(System.currentTimeMillis());
    }
    
    /**
     * Get total number of messages published
     */
    public long getPublishedCount() {
        return publishedCount.sum();
    }
    
    /**
     * Get total bytes published
     */
    public long getPublishedBytes() {
        return publishedBytes.sum();
    }
    
    /**
     * Get average publish time in milliseconds
     */
    public double getAveragePublishTime() {
        long count = getPublishedCount();
        return count > 0 ? (double) totalPublishTime.get() / count : 0.0;
    }
    
    /**
     * Get maximum publish time in milliseconds
     */
    public long getMaxPublishTime() {
        return maxPublishTime.get();
    }
    
    /**
     * Get minimum publish time in milliseconds
     */
    public long getMinPublishTime() {
        long min = minPublishTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }
    
    /**
     * Get total number of messages consumed
     */
    public long getConsumedCount() {
        return consumedCount.sum();
    }
    
    /**
     * Get total bytes consumed
     */
    public long getConsumedBytes() {
        return consumedBytes.sum();
    }
    
    /**
     * Get average consume time in milliseconds
     */
    public double getAverageConsumeTime() {
        long count = getConsumedCount();
        return count > 0 ? (double) totalConsumeTime.get() / count : 0.0;
    }
    
    /**
     * Get maximum consume time in milliseconds
     */
    public long getMaxConsumeTime() {
        return maxConsumeTime.get();
    }
    
    /**
     * Get minimum consume time in milliseconds
     */
    public long getMinConsumeTime() {
        long min = minConsumeTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }
    
    /**
     * Get total error count
     */
    public long getTotalErrorCount() {
        return errorCounts.values().stream().mapToLong(LongAdder::sum).sum();
    }
    
    /**
     * Get error count by type
     */
    public long getErrorCount(String errorType) {
        LongAdder counter = errorCounts.get(errorType);
        return counter != null ? counter.sum() : 0;
    }
    
    /**
     * Get error rate (errors per message)
     */
    public double getErrorRate() {
        long totalMessages = getPublishedCount() + getConsumedCount();
        return totalMessages > 0 ? (double) getTotalErrorCount() / totalMessages : 0.0;
    }
    
    /**
     * Check if topic has high error rate (>5%)
     */
    public boolean hasHighErrorRate() {
        return getErrorRate() > 0.05;
    }
    
    /**
     * Get current publish throughput (messages per second)
     */
    public double getPublishThroughput() {
        return calculateThroughput(getPublishedCount(), lastPublishedCount);
    }
    
    /**
     * Get current consume throughput (messages per second)
     */
    public double getConsumeThroughput() {
        return calculateThroughput(getConsumedCount(), lastConsumedCount);
    }
    
    /**
     * Get topic name
     */
    public String getTopic() {
        return topic;
    }
    
    /**
     * Get creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Get last error timestamp
     */
    public Instant getLastErrorTime() {
        long timestamp = lastErrorTime.get();
        return timestamp > 0 ? Instant.ofEpochMilli(timestamp) : null;
    }
    
    /**
     * Reset metrics (for testing or periodic resets)
     */
    public void reset() {
        publishedCount.reset();
        publishedBytes.reset();
        totalPublishTime.set(0);
        maxPublishTime.set(0);
        minPublishTime.set(Long.MAX_VALUE);
        
        consumedCount.reset();
        consumedBytes.reset();
        totalConsumeTime.set(0);
        maxConsumeTime.set(0);
        minConsumeTime.set(Long.MAX_VALUE);
        
        errorCounts.clear();
        lastErrorTime.set(0);
        
        lastThroughputCheck.set(System.currentTimeMillis());
        lastPublishedCount.set(0);
        lastConsumedCount.set(0);
    }
    
    private void updateMinTime(AtomicLong minTime, long newTime) {
        minTime.updateAndGet(current -> Math.min(current, newTime));
    }
    
    private void updateMaxTime(AtomicLong maxTime, long newTime) {
        maxTime.updateAndGet(current -> Math.max(current, newTime));
    }
    
    private double calculateThroughput(long currentCount, AtomicLong lastCount) {
        long now = System.currentTimeMillis();
        long lastCheck = lastThroughputCheck.get();
        
        if (now - lastCheck >= 1000) { // Update every second
            long timeDiff = now - lastCheck;
            long countDiff = currentCount - lastCount.get();
            
            lastThroughputCheck.set(now);
            lastCount.set(currentCount);
            
            return timeDiff > 0 ? (double) countDiff * 1000 / timeDiff : 0.0;
        }
        
        return 0.0;
    }
    
    @Override
    public String toString() {
        return String.format(
            "TopicMetrics{topic='%s', published=%d, consumed=%d, errors=%d, errorRate=%.2f%%}",
            topic, getPublishedCount(), getConsumedCount(), getTotalErrorCount(), getErrorRate() * 100
        );
    }
}
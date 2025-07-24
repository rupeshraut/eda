package eda.eventbus.kafka.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics for Kafka producers
 */
public class ProducerMetrics {
    private final String producerId;
    private final Instant createdAt;
    
    // Send metrics
    private final LongAdder recordsSent = new LongAdder();
    private final LongAdder bytesSent = new LongAdder();
    private final AtomicLong totalSendTime = new AtomicLong();
    private final AtomicLong maxSendTime = new AtomicLong();
    private final AtomicLong minSendTime = new AtomicLong(Long.MAX_VALUE);
    
    // Batch metrics
    private final LongAdder batchCount = new LongAdder();
    private final AtomicLong totalBatchSize = new AtomicLong();
    private final AtomicLong maxBatchSize = new AtomicLong();
    
    // Error metrics
    private final ConcurrentHashMap<String, LongAdder> errorCounts = new ConcurrentHashMap<>();
    private final LongAdder retriedRecords = new LongAdder();
    
    // Buffer metrics
    private final AtomicLong bufferTotalBytes = new AtomicLong();
    private final AtomicLong bufferAvailableBytes = new AtomicLong();
    
    // Throughput tracking
    private final AtomicLong lastThroughputCheck = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastRecordCount = new AtomicLong();
    
    public ProducerMetrics(String producerId) {
        this.producerId = producerId;
        this.createdAt = Instant.now();
    }
    
    /**
     * Record a successful send
     */
    public void recordSend(int recordSize, Duration sendTime) {
        recordsSent.increment();
        bytesSent.add(recordSize);
        
        long sendTimeMs = sendTime.toMillis();
        totalSendTime.addAndGet(sendTimeMs);
        
        // Update min/max send times
        minSendTime.updateAndGet(current -> Math.min(current, sendTimeMs));
        maxSendTime.updateAndGet(current -> Math.max(current, sendTimeMs));
    }
    
    /**
     * Record a batch sent
     */
    public void recordBatch(int batchSize, int recordCount) {
        batchCount.increment();
        totalBatchSize.addAndGet(batchSize);
        maxBatchSize.updateAndGet(current -> Math.max(current, batchSize));
    }
    
    /**
     * Record an error
     */
    public void recordError(String errorType) {
        errorCounts.computeIfAbsent(errorType, k -> new LongAdder()).increment();
    }
    
    /**
     * Record a retry
     */
    public void recordRetry() {
        retriedRecords.increment();
    }
    
    /**
     * Update buffer metrics
     */
    public void updateBufferMetrics(long totalBytes, long availableBytes) {
        bufferTotalBytes.set(totalBytes);
        bufferAvailableBytes.set(availableBytes);
    }
    
    /**
     * Get producer ID
     */
    public String getProducerId() {
        return producerId;
    }
    
    /**
     * Get total records sent
     */
    public long getRecordsSent() {
        return recordsSent.sum();
    }
    
    /**
     * Get total bytes sent
     */
    public long getBytesSent() {
        return bytesSent.sum();
    }
    
    /**
     * Get average send time in milliseconds
     */
    public double getAverageSendTime() {
        long count = getRecordsSent();
        return count > 0 ? (double) totalSendTime.get() / count : 0.0;
    }
    
    /**
     * Get maximum send time in milliseconds
     */
    public long getMaxSendTime() {
        return maxSendTime.get();
    }
    
    /**
     * Get minimum send time in milliseconds
     */
    public long getMinSendTime() {
        long min = minSendTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }
    
    /**
     * Get total batches sent
     */
    public long getBatchCount() {
        return batchCount.sum();
    }
    
    /**
     * Get average batch size in bytes
     */
    public double getAverageBatchSize() {
        long count = getBatchCount();
        return count > 0 ? (double) totalBatchSize.get() / count : 0.0;
    }
    
    /**
     * Get maximum batch size in bytes
     */
    public long getMaxBatchSize() {
        return maxBatchSize.get();
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
     * Get retry count
     */
    public long getRetryCount() {
        return retriedRecords.sum();
    }
    
    /**
     * Get error rate (errors per record)
     */
    public double getErrorRate() {
        long totalRecords = getRecordsSent();
        return totalRecords > 0 ? (double) getTotalErrorCount() / totalRecords : 0.0;
    }
    
    /**
     * Get retry rate (retries per record)
     */
    public double getRetryRate() {
        long totalRecords = getRecordsSent();
        return totalRecords > 0 ? (double) getRetryCount() / totalRecords : 0.0;
    }
    
    /**
     * Get buffer utilization (0.0 to 1.0)
     */
    public double getBufferUtilization() {
        long total = bufferTotalBytes.get();
        long available = bufferAvailableBytes.get();
        
        if (total > 0) {
            return (double) (total - available) / total;
        }
        return 0.0;
    }
    
    /**
     * Get current throughput (records per second)
     */
    public double getThroughput() {
        long now = System.currentTimeMillis();
        long lastCheck = lastThroughputCheck.get();
        
        if (now - lastCheck >= 1000) { // Update every second
            long timeDiff = now - lastCheck;
            long countDiff = getRecordsSent() - lastRecordCount.get();
            
            lastThroughputCheck.set(now);
            lastRecordCount.set(getRecordsSent());
            
            return timeDiff > 0 ? (double) countDiff * 1000 / timeDiff : 0.0;
        }
        
        return 0.0;
    }
    
    /**
     * Get overall throughput since creation (records per second)
     */
    public double getOverallThroughput() {
        long now = System.currentTimeMillis();
        long elapsedMs = now - createdAt.toEpochMilli();
        
        if (elapsedMs > 0) {
            return (double) getRecordsSent() * 1000 / elapsedMs;
        }
        return 0.0;
    }
    
    /**
     * Check if producer has high error rate (>5%)
     */
    public boolean hasHighErrorRate() {
        return getErrorRate() > 0.05;
    }
    
    /**
     * Check if buffer is highly utilized (>80%)
     */
    public boolean hasHighBufferUtilization() {
        return getBufferUtilization() > 0.8;
    }
    
    /**
     * Get creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Reset metrics
     */
    public void reset() {
        recordsSent.reset();
        bytesSent.reset();
        totalSendTime.set(0);
        maxSendTime.set(0);
        minSendTime.set(Long.MAX_VALUE);
        
        batchCount.reset();
        totalBatchSize.set(0);
        maxBatchSize.set(0);
        
        errorCounts.clear();
        retriedRecords.reset();
        
        bufferTotalBytes.set(0);
        bufferAvailableBytes.set(0);
        
        lastThroughputCheck.set(System.currentTimeMillis());
        lastRecordCount.set(0);
    }
    
    @Override
    public String toString() {
        return String.format(
            "ProducerMetrics{id='%s', sent=%d, bytes=%d, errors=%d, errorRate=%.2f%%, throughput=%.1f/s}",
            producerId, getRecordsSent(), getBytesSent(), getTotalErrorCount(), 
            getErrorRate() * 100, getThroughput()
        );
    }
}
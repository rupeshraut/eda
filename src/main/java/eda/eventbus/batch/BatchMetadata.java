package eda.eventbus.batch;

import java.time.Duration;
import java.util.Map;

/**
 * Metadata for event batches
 */
public class BatchMetadata {
    private final int maxSize;
    private final Duration maxWaitTime;
    private final Map<String, String> properties;
    private final BatchStrategy strategy;
    
    public BatchMetadata(int maxSize, Duration maxWaitTime, Map<String, String> properties, BatchStrategy strategy) {
        this.maxSize = maxSize;
        this.maxWaitTime = maxWaitTime;
        this.properties = Map.copyOf(properties);
        this.strategy = strategy;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public int getMaxSize() { return maxSize; }
    public Duration getMaxWaitTime() { return maxWaitTime; }
    public Map<String, String> getProperties() { return properties; }
    public BatchStrategy getStrategy() { return strategy; }
    
    public static class Builder {
        private int maxSize = 10;
        private Duration maxWaitTime = Duration.ofSeconds(1);
        private Map<String, String> properties = Map.of();
        private BatchStrategy strategy = BatchStrategy.SIZE_OR_TIME;
        
        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }
        
        public Builder maxWaitTime(Duration maxWaitTime) {
            this.maxWaitTime = maxWaitTime;
            return this;
        }
        
        public Builder properties(Map<String, String> properties) {
            this.properties = Map.copyOf(properties);
            return this;
        }
        
        public Builder strategy(BatchStrategy strategy) {
            this.strategy = strategy;
            return this;
        }
        
        public BatchMetadata build() {
            return new BatchMetadata(maxSize, maxWaitTime, properties, strategy);
        }
    }
    
    public enum BatchStrategy {
        /**
         * Trigger batch when size OR time limit is reached
         */
        SIZE_OR_TIME,
        
        /**
         * Trigger batch when size AND time limit is reached
         */
        SIZE_AND_TIME,
        
        /**
         * Trigger batch only when size limit is reached
         */
        SIZE_ONLY,
        
        /**
         * Trigger batch only when time limit is reached
         */
        TIME_ONLY
    }
}
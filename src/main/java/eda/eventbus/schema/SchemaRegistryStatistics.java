package eda.eventbus.schema;

import java.time.Instant;

/**
 * Statistics for EventSchemaRegistry
 */
public class SchemaRegistryStatistics {
    private final long totalSchemas;
    private final int eventTypes;
    private final long validationCount;
    private final long migrationCount;
    private final int cacheSize;
    private final Instant timestamp;
    
    private SchemaRegistryStatistics(Builder builder) {
        this.totalSchemas = builder.totalSchemas;
        this.eventTypes = builder.eventTypes;
        this.validationCount = builder.validationCount;
        this.migrationCount = builder.migrationCount;
        this.cacheSize = builder.cacheSize;
        this.timestamp = builder.timestamp;
    }
    
    public long getTotalSchemas() { return totalSchemas; }
    public int getEventTypes() { return eventTypes; }
    public long getValidationCount() { return validationCount; }
    public long getMigrationCount() { return migrationCount; }
    public int getCacheSize() { return cacheSize; }
    public Instant getTimestamp() { return timestamp; }
    
    /**
     * Get average schemas per event type
     */
    public double getAverageSchemasPerType() {
        return eventTypes > 0 ? (double) totalSchemas / eventTypes : 0.0;
    }
    
    /**
     * Check if registry is healthy
     */
    public boolean isHealthy() {
        return totalSchemas > 0 && eventTypes > 0;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private long totalSchemas = 0;
        private int eventTypes = 0;
        private long validationCount = 0;
        private long migrationCount = 0;
        private int cacheSize = 0;
        private Instant timestamp = Instant.now();
        
        public Builder totalSchemas(long totalSchemas) {
            this.totalSchemas = totalSchemas;
            return this;
        }
        
        public Builder eventTypes(int eventTypes) {
            this.eventTypes = eventTypes;
            return this;
        }
        
        public Builder validationCount(long validationCount) {
            this.validationCount = validationCount;
            return this;
        }
        
        public Builder migrationCount(long migrationCount) {
            this.migrationCount = migrationCount;
            return this;
        }
        
        public Builder cacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public SchemaRegistryStatistics build() {
            return new SchemaRegistryStatistics(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "SchemaRegistryStatistics{schemas=%d, types=%d, validations=%d, migrations=%d, cache=%d}",
            totalSchemas, eventTypes, validationCount, migrationCount, cacheSize
        );
    }
}
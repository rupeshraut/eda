package eda.eventbus.schema;

/**
 * Configuration for EventSchemaRegistry
 */
public class SchemaRegistryConfig {
    private final boolean enforceCompatibility;
    private final boolean enforceSchemaValidation;
    private final boolean allowSchemaEvolution;
    private final int maxCacheSize;
    private final SchemaCompatibility.CompatibilityType defaultCompatibilityType;
    private final boolean enableMetrics;
    
    private SchemaRegistryConfig(Builder builder) {
        this.enforceCompatibility = builder.enforceCompatibility;
        this.enforceSchemaValidation = builder.enforceSchemaValidation;
        this.allowSchemaEvolution = builder.allowSchemaEvolution;
        this.maxCacheSize = builder.maxCacheSize;
        this.defaultCompatibilityType = builder.defaultCompatibilityType;
        this.enableMetrics = builder.enableMetrics;
    }
    
    public boolean isEnforceCompatibility() { return enforceCompatibility; }
    public boolean isEnforceSchemaValidation() { return enforceSchemaValidation; }
    public boolean isAllowSchemaEvolution() { return allowSchemaEvolution; }
    public int getMaxCacheSize() { return maxCacheSize; }
    public SchemaCompatibility.CompatibilityType getDefaultCompatibilityType() { return defaultCompatibilityType; }
    public boolean isEnableMetrics() { return enableMetrics; }
    
    public static SchemaRegistryConfig defaultConfig() {
        return builder().build();
    }
    
    public static SchemaRegistryConfig strictConfig() {
        return builder()
            .enforceCompatibility(true)
            .enforceSchemaValidation(true)
            .allowSchemaEvolution(false)
            .defaultCompatibilityType(SchemaCompatibility.CompatibilityType.FULL)
            .build();
    }
    
    public static SchemaRegistryConfig lenientConfig() {
        return builder()
            .enforceCompatibility(false)
            .enforceSchemaValidation(false)
            .allowSchemaEvolution(true)
            .defaultCompatibilityType(SchemaCompatibility.CompatibilityType.NONE)
            .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean enforceCompatibility = true;
        private boolean enforceSchemaValidation = true;
        private boolean allowSchemaEvolution = true;
        private int maxCacheSize = 1000;
        private SchemaCompatibility.CompatibilityType defaultCompatibilityType = SchemaCompatibility.CompatibilityType.BACKWARD;
        private boolean enableMetrics = true;
        
        public Builder enforceCompatibility(boolean enforceCompatibility) {
            this.enforceCompatibility = enforceCompatibility;
            return this;
        }
        
        public Builder enforceSchemaValidation(boolean enforceSchemaValidation) {
            this.enforceSchemaValidation = enforceSchemaValidation;
            return this;
        }
        
        public Builder allowSchemaEvolution(boolean allowSchemaEvolution) {
            this.allowSchemaEvolution = allowSchemaEvolution;
            return this;
        }
        
        public Builder maxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
            return this;
        }
        
        public Builder defaultCompatibilityType(SchemaCompatibility.CompatibilityType defaultCompatibilityType) {
            this.defaultCompatibilityType = defaultCompatibilityType;
            return this;
        }
        
        public Builder enableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
            return this;
        }
        
        public SchemaRegistryConfig build() {
            return new SchemaRegistryConfig(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "SchemaRegistryConfig{compatibility=%s, validation=%s, evolution=%s, cache=%d, type=%s}",
            enforceCompatibility, enforceSchemaValidation, allowSchemaEvolution, maxCacheSize, defaultCompatibilityType
        );
    }
}
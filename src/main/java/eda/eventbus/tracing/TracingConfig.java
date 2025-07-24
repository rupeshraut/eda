package eda.eventbus.tracing;

/**
 * Configuration for distributed tracing
 */
public class TracingConfig {
    private final boolean enabled;
    private final double samplingRate;
    private final boolean includeStackTrace;
    private final boolean enableAsyncTracing;
    private final int maxSpanAttributes;
    private final int maxSpanEvents;
    private final boolean enableMetrics;
    private final String serviceName;
    private final String serviceVersion;
    
    private TracingConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.samplingRate = builder.samplingRate;
        this.includeStackTrace = builder.includeStackTrace;
        this.enableAsyncTracing = builder.enableAsyncTracing;
        this.maxSpanAttributes = builder.maxSpanAttributes;
        this.maxSpanEvents = builder.maxSpanEvents;
        this.enableMetrics = builder.enableMetrics;
        this.serviceName = builder.serviceName;
        this.serviceVersion = builder.serviceVersion;
    }
    
    public boolean isEnabled() { return enabled; }
    public double getSamplingRate() { return samplingRate; }
    public boolean isIncludeStackTrace() { return includeStackTrace; }
    public boolean isEnableAsyncTracing() { return enableAsyncTracing; }
    public int getMaxSpanAttributes() { return maxSpanAttributes; }
    public int getMaxSpanEvents() { return maxSpanEvents; }
    public boolean isEnableMetrics() { return enableMetrics; }
    public String getServiceName() { return serviceName; }
    public String getServiceVersion() { return serviceVersion; }
    
    public static TracingConfig defaultConfig() {
        return builder().build();
    }
    
    public static TracingConfig disabledConfig() {
        return builder()
            .enabled(false)
            .build();
    }
    
    public static TracingConfig highVolumeConfig() {
        return builder()
            .enabled(true)
            .samplingRate(0.1) // 10% sampling for high volume
            .includeStackTrace(false)
            .maxSpanAttributes(20)
            .maxSpanEvents(10)
            .build();
    }
    
    public static TracingConfig debugConfig() {
        return builder()
            .enabled(true)
            .samplingRate(1.0) // 100% sampling for debugging
            .includeStackTrace(true)
            .enableAsyncTracing(true)
            .enableMetrics(true)
            .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean enabled = true;
        private double samplingRate = 0.3; // 30% default sampling
        private boolean includeStackTrace = false;
        private boolean enableAsyncTracing = true;
        private int maxSpanAttributes = 50;
        private int maxSpanEvents = 20;
        private boolean enableMetrics = true;
        private String serviceName = "event-bus";
        private String serviceVersion = "1.0.0";
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder samplingRate(double samplingRate) {
            if (samplingRate < 0.0 || samplingRate > 1.0) {
                throw new IllegalArgumentException("Sampling rate must be between 0.0 and 1.0");
            }
            this.samplingRate = samplingRate;
            return this;
        }
        
        public Builder includeStackTrace(boolean includeStackTrace) {
            this.includeStackTrace = includeStackTrace;
            return this;
        }
        
        public Builder enableAsyncTracing(boolean enableAsyncTracing) {
            this.enableAsyncTracing = enableAsyncTracing;
            return this;
        }
        
        public Builder maxSpanAttributes(int maxSpanAttributes) {
            this.maxSpanAttributes = maxSpanAttributes;
            return this;
        }
        
        public Builder maxSpanEvents(int maxSpanEvents) {
            this.maxSpanEvents = maxSpanEvents;
            return this;
        }
        
        public Builder enableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
            return this;
        }
        
        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }
        
        public Builder serviceVersion(String serviceVersion) {
            this.serviceVersion = serviceVersion;
            return this;
        }
        
        public TracingConfig build() {
            return new TracingConfig(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "TracingConfig{enabled=%s, sampling=%.2f, stackTrace=%s, async=%s, service='%s'}",
            enabled, samplingRate, includeStackTrace, enableAsyncTracing, serviceName
        );
    }
}
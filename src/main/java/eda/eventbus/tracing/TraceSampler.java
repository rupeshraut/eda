package eda.eventbus.tracing;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Sampling strategy for traces
 */
public class TraceSampler {
    private final double samplingRate;
    private final SamplingStrategy strategy;
    
    public TraceSampler(double samplingRate) {
        this(samplingRate, SamplingStrategy.PROBABILISTIC);
    }
    
    public TraceSampler(double samplingRate, SamplingStrategy strategy) {
        if (samplingRate < 0.0 || samplingRate > 1.0) {
            throw new IllegalArgumentException("Sampling rate must be between 0.0 and 1.0");
        }
        this.samplingRate = samplingRate;
        this.strategy = strategy;
    }
    
    /**
     * Determine if a trace should be sampled
     */
    public boolean shouldSample() {
        return switch (strategy) {
            case ALWAYS_ON -> true;
            case ALWAYS_OFF -> false;
            case PROBABILISTIC -> ThreadLocalRandom.current().nextDouble() < samplingRate;
        };
    }
    
    /**
     * Determine if a trace should be sampled based on trace ID
     */
    public boolean shouldSample(String traceId) {
        if (strategy == SamplingStrategy.ALWAYS_ON) return true;
        if (strategy == SamplingStrategy.ALWAYS_OFF) return false;
        
        // Use trace ID for consistent sampling decisions
        if (traceId != null && !traceId.isEmpty()) {
            long hash = Math.abs(traceId.hashCode());
            return (hash % 100) < (samplingRate * 100);
        }
        
        return shouldSample();
    }
    
    public double getSamplingRate() {
        return samplingRate;
    }
    
    public SamplingStrategy getStrategy() {
        return strategy;
    }
    
    /**
     * Create a sampler that samples all traces
     */
    public static TraceSampler alwaysOn() {
        return new TraceSampler(1.0, SamplingStrategy.ALWAYS_ON);
    }
    
    /**
     * Create a sampler that samples no traces
     */
    public static TraceSampler alwaysOff() {
        return new TraceSampler(0.0, SamplingStrategy.ALWAYS_OFF);
    }
    
    /**
     * Create a probabilistic sampler
     */
    public static TraceSampler probabilistic(double rate) {
        return new TraceSampler(rate, SamplingStrategy.PROBABILISTIC);
    }
    
    @Override
    public String toString() {
        return String.format("TraceSampler{rate=%.2f, strategy=%s}", samplingRate, strategy);
    }
}

/**
 * Sampling strategies
 */
enum SamplingStrategy {
    ALWAYS_ON,
    ALWAYS_OFF,
    PROBABILISTIC
}
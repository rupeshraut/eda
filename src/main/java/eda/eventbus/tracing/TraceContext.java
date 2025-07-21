package eda.eventbus.tracing;

import java.util.Map;
import java.util.UUID;

/**
 * Context for distributed tracing
 */
public class TraceContext {
    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final Map<String, String> baggage;
    private final boolean sampled;
    
    public TraceContext(String traceId, String spanId, String parentSpanId, 
                       Map<String, String> baggage, boolean sampled) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.baggage = Map.copyOf(baggage);
        this.sampled = sampled;
    }
    
    public static TraceContext newTrace() {
        return new TraceContext(
            generateTraceId(),
            generateSpanId(),
            null,
            Map.of(),
            true
        );
    }
    
    public static TraceContext childSpan(TraceContext parent) {
        return new TraceContext(
            parent.traceId,
            generateSpanId(),
            parent.spanId,
            parent.baggage,
            parent.sampled
        );
    }
    
    // Getters
    public String getTraceId() { return traceId; }
    public String getSpanId() { return spanId; }
    public String getParentSpanId() { return parentSpanId; }
    public Map<String, String> getBaggage() { return baggage; }
    public boolean isSampled() { return sampled; }
    
    /**
     * Convert to HTTP headers format
     */
    public Map<String, String> toHeaders() {
        return Map.of(
            "x-trace-id", traceId,
            "x-span-id", spanId,
            "x-parent-span-id", parentSpanId != null ? parentSpanId : "",
            "x-sampled", String.valueOf(sampled)
        );
    }
    
    /**
     * Create context from HTTP headers
     */
    public static TraceContext fromHeaders(Map<String, String> headers) {
        String traceId = headers.get("x-trace-id");
        String spanId = headers.get("x-span-id");
        String parentSpanId = headers.get("x-parent-span-id");
        boolean sampled = Boolean.parseBoolean(headers.getOrDefault("x-sampled", "true"));
        
        if (traceId == null || spanId == null) {
            return newTrace();
        }
        
        return new TraceContext(
            traceId,
            spanId,
            parentSpanId.isEmpty() ? null : parentSpanId,
            Map.of(),
            sampled
        );
    }
    
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private static String generateSpanId() {
        return UUID.randomUUID().toString().substring(0, 16).replace("-", "");
    }
    
    @Override
    public String toString() {
        return "TraceContext{" +
                "traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", parentSpanId='" + parentSpanId + '\'' +
                ", sampled=" + sampled +
                '}';
    }
}
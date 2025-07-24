package eda.eventbus.tracing;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a trace span with timing and metadata
 */
public class TraceSpan {
    private final TraceContext context;
    private final String operationName;
    private final SpanKind kind;
    private final Instant startTime;
    private final Map<String, String> attributes = new ConcurrentHashMap<>();
    private final List<SpanEvent> events = new ArrayList<>();
    
    private volatile Instant endTime;
    private volatile SpanStatus status = SpanStatus.UNSET;
    private volatile Throwable error;
    private volatile boolean finished = false;
    
    private TraceSpan(Builder builder) {
        this.context = builder.context;
        this.operationName = builder.operationName;
        this.kind = builder.kind;
        this.startTime = builder.startTime;
        this.attributes.putAll(builder.attributes);
    }
    
    public TraceContext getContext() { return context; }
    public String getOperationName() { return operationName; }
    public SpanKind getKind() { return kind; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public SpanStatus getStatus() { return status; }
    public Throwable getError() { return error; }
    public boolean isFinished() { return finished; }
    public Map<String, String> getAttributes() { return Map.copyOf(attributes); }
    public List<SpanEvent> getEvents() { return List.copyOf(events); }
    
    /**
     * Add attribute to span
     */
    public void addAttribute(String key, String value) {
        if (!finished) {
            attributes.put(key, value);
        }
    }
    
    /**
     * Add multiple attributes
     */
    public void addAttributes(Map<String, String> attrs) {
        if (!finished) {
            attributes.putAll(attrs);
        }
    }
    
    /**
     * Add event to span
     */
    public void addEvent(SpanEvent event) {
        if (!finished) {
            synchronized (events) {
                events.add(event);
            }
        }
    }
    
    /**
     * Add event with name and timestamp
     */
    public void addEvent(String name) {
        addEvent(new SpanEvent(name, Instant.now(), Map.of()));
    }
    
    /**
     * Add event with name, timestamp and attributes
     */
    public void addEvent(String name, Map<String, String> attributes) {
        addEvent(new SpanEvent(name, Instant.now(), attributes));
    }
    
    /**
     * Record error in span
     */
    public void recordError(Throwable error) {
        if (!finished) {
            this.error = error;
            this.status = SpanStatus.ERROR;
            addAttribute("error", "true");
            addAttribute("error.type", error.getClass().getSimpleName());
            if (error.getMessage() != null) {
                addAttribute("error.message", error.getMessage());
            }
        }
    }
    
    /**
     * Set span status
     */
    public void setStatus(SpanStatus status) {
        if (!finished) {
            this.status = status;
        }
    }
    
    /**
     * Finish the span
     */
    public void finish() {
        if (!finished) {
            this.endTime = Instant.now();
            this.finished = true;
            
            if (status == SpanStatus.UNSET) {
                status = SpanStatus.OK;
            }
        }
    }
    
    /**
     * Get span duration
     */
    public Duration getDuration() {
        Instant end = endTime != null ? endTime : Instant.now();
        return Duration.between(startTime, end);
    }
    
    /**
     * Check if span has error
     */
    public boolean hasError() {
        return error != null || status == SpanStatus.ERROR;
    }
    
    /**
     * Get attribute value
     */
    public String getAttribute(String key) {
        return attributes.get(key);
    }
    
    /**
     * Check if attribute exists
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    
    /**
     * Get span ID
     */
    public String getSpanId() {
        return context.getSpanId();
    }
    
    /**
     * Get trace ID
     */
    public String getTraceId() {
        return context.getTraceId();
    }
    
    /**
     * Check if span is sampled
     */
    public boolean isSampled() {
        return context.isSampled();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private TraceContext context;
        private String operationName;
        private SpanKind kind = SpanKind.INTERNAL;
        private Instant startTime = Instant.now();
        private Map<String, String> attributes = new ConcurrentHashMap<>();
        
        public Builder context(TraceContext context) {
            this.context = context;
            return this;
        }
        
        public Builder operationName(String operationName) {
            this.operationName = operationName;
            return this;
        }
        
        public Builder kind(SpanKind kind) {
            this.kind = kind;
            return this;
        }
        
        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder addAttribute(String key, String value) {
            this.attributes.put(key, value);
            return this;
        }
        
        public Builder addAttributes(Map<String, String> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }
        
        public TraceSpan build() {
            if (context == null) {
                throw new IllegalArgumentException("TraceContext is required");
            }
            if (operationName == null || operationName.trim().isEmpty()) {
                throw new IllegalArgumentException("Operation name is required");
            }
            
            return new TraceSpan(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "TraceSpan{traceId='%s', spanId='%s', operation='%s', kind=%s, status=%s, duration=%s}",
            getTraceId(), getSpanId(), operationName, kind, status, 
            finished ? getDuration() : "ongoing"
        );
    }
}

/**
 * Span status enumeration
 */
enum SpanStatus {
    UNSET("Unset"),
    OK("OK"),
    ERROR("Error");
    
    private final String description;
    
    SpanStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

/**
 * Span kind enumeration
 */
enum SpanKind {
    INTERNAL("Internal"),
    SERVER("Server"),
    CLIENT("Client"),
    PRODUCER("Producer"),
    CONSUMER("Consumer");
    
    private final String description;
    
    SpanKind(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
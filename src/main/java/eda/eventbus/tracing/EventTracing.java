package eda.eventbus.tracing;

import eda.eventbus.core.GenericEvent;
import java.util.Map;

/**
 * Interface for distributed tracing support
 */
public interface EventTracing {
    
    /**
     * Start a new trace for event publishing
     */
    TraceContext startPublishTrace(GenericEvent<?, ?> event);
    
    /**
     * Start a new trace for event processing
     */
    TraceContext startProcessingTrace(GenericEvent<?, ?> event, String subscriberId);
    
    /**
     * Continue an existing trace from event headers
     */
    TraceContext continueTrace(Map<String, String> traceHeaders);
    
    /**
     * Add tracing headers to event
     */
    <T, D> GenericEvent<T, D> addTracingHeaders(GenericEvent<T, D> event, TraceContext context);
    
    /**
     * Record span event
     */
    void recordSpanEvent(TraceContext context, String eventName, Map<String, String> attributes);
    
    /**
     * Record error in span
     */
    void recordError(TraceContext context, Throwable error);
    
    /**
     * Finish trace span
     */
    void finishSpan(TraceContext context);
    
    /**
     * Get current trace context
     */
    TraceContext getCurrentContext();
    
    /**
     * Check if tracing is enabled
     */
    boolean isEnabled();
}
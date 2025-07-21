package eda.eventbus.tracing;

import eda.eventbus.core.GenericEvent;
import java.util.Map;

/**
 * No-op implementation of EventTracing for when tracing is disabled
 */
public class NoOpEventTracing implements EventTracing {
    
    @Override
    public TraceContext startPublishTrace(GenericEvent<?, ?> event) {
        return TraceContext.newTrace();
    }
    
    @Override
    public TraceContext startProcessingTrace(GenericEvent<?, ?> event, String subscriberId) {
        return TraceContext.newTrace();
    }
    
    @Override
    public TraceContext continueTrace(Map<String, String> traceHeaders) {
        return TraceContext.fromHeaders(traceHeaders);
    }
    
    @Override
    public <T, D> GenericEvent<T, D> addTracingHeaders(GenericEvent<T, D> event, TraceContext context) {
        return event.withHeaders(context.toHeaders());
    }
    
    @Override
    public void recordSpanEvent(TraceContext context, String eventName, Map<String, String> attributes) {
        // No-op
    }
    
    @Override
    public void recordError(TraceContext context, Throwable error) {
        // No-op
    }
    
    @Override
    public void finishSpan(TraceContext context) {
        // No-op
    }
    
    @Override
    public TraceContext getCurrentContext() {
        return TraceContext.newTrace();
    }
    
    @Override
    public boolean isEnabled() {
        return false;
    }
}
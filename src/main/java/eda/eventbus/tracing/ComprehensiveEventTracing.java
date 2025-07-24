package eda.eventbus.tracing;

import eda.eventbus.core.GenericEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Comprehensive implementation of EventTracing with distributed logging
 */
public class ComprehensiveEventTracing implements EventTracing {
    private static final Logger LOGGER = Logger.getLogger(ComprehensiveEventTracing.class.getName());
    
    private final TracingConfig config;
    private final List<TraceExporter> exporters = new ArrayList<>();
    private final List<Consumer<TraceSpan>> spanListeners = new ArrayList<>();
    
    // Active spans tracking
    private final ConcurrentHashMap<String, TraceSpan> activeSpans = new ConcurrentHashMap<>();
    private final ThreadLocal<TraceContext> currentContext = new ThreadLocal<>();
    
    // Statistics
    private final AtomicLong totalSpans = new AtomicLong();
    private final AtomicLong sampledSpans = new AtomicLong();
    private final LongAdder totalEvents = new LongAdder();
    private final LongAdder totalErrors = new LongAdder();
    
    // Sampling strategy
    private final TraceSampler sampler;
    
    public ComprehensiveEventTracing() {
        this(TracingConfig.defaultConfig());
    }
    
    public ComprehensiveEventTracing(TracingConfig config) {
        this.config = config;
        this.sampler = new TraceSampler(config.getSamplingRate());
        
        LOGGER.info("ComprehensiveEventTracing initialized with config: " + config);
    }
    
    @Override
    public TraceContext startPublishTrace(GenericEvent<?, ?> event) {
        if (!config.isEnabled()) {
            return TraceContext.newTrace();
        }
        
        TraceContext context = createNewTraceContext();
        TraceSpan span = createSpan(context, "event.publish", SpanKind.PRODUCER);
        
        // Add event metadata to span
        span.addAttribute("event.type", event.getEventType().toString());
        span.addAttribute("event.id", event.getEventId().toString());
        span.addAttribute("event.timestamp", event.getTimestamp().toString());
        span.addAttribute("component", "event-bus");
        span.addAttribute("operation", "publish");
        
        activeSpans.put(context.getSpanId(), span);
        setCurrentContext(context);
        
        totalSpans.incrementAndGet();
        if (context.isSampled()) {
            sampledSpans.incrementAndGet();
        }
        
        LOGGER.fine("Started publish trace: " + context.getTraceId());
        return context;
    }
    
    @Override
    public TraceContext startProcessingTrace(GenericEvent<?, ?> event, String subscriberId) {
        if (!config.isEnabled()) {
            return TraceContext.newTrace();
        }
        
        // Try to continue trace from event headers
        TraceContext parentContext = extractTraceFromEvent(event);
        TraceContext context = parentContext != null ? 
            TraceContext.childSpan(parentContext) : createNewTraceContext();
        
        TraceSpan span = createSpan(context, "event.process", SpanKind.CONSUMER);
        
        // Add processing metadata
        span.addAttribute("event.type", event.getEventType().toString());
        span.addAttribute("event.id", event.getEventId().toString());
        span.addAttribute("subscriber.id", subscriberId);
        span.addAttribute("component", "event-bus");
        span.addAttribute("operation", "process");
        
        // Add correlation information if parent exists
        if (parentContext != null) {
            span.addAttribute("parent.trace.id", parentContext.getTraceId());
            span.addAttribute("parent.span.id", parentContext.getSpanId());
        }
        
        activeSpans.put(context.getSpanId(), span);
        setCurrentContext(context);
        
        totalSpans.incrementAndGet();
        if (context.isSampled()) {
            sampledSpans.incrementAndGet();
        }
        
        LOGGER.fine("Started processing trace: " + context.getTraceId() + " for subscriber: " + subscriberId);
        return context;
    }
    
    @Override
    public TraceContext continueTrace(Map<String, String> traceHeaders) {
        if (!config.isEnabled() || traceHeaders == null || traceHeaders.isEmpty()) {
            return TraceContext.newTrace();
        }
        
        TraceContext context = TraceContext.fromHeaders(traceHeaders);
        setCurrentContext(context);
        
        LOGGER.fine("Continued trace: " + context.getTraceId());
        return context;
    }
    
    @Override
    public <T, D> GenericEvent<T, D> addTracingHeaders(GenericEvent<T, D> event, TraceContext context) {
        if (!config.isEnabled() || context == null) {
            return event;
        }
        
        Map<String, String> headers = new HashMap<>(event.getHeaders());
        headers.putAll(context.toHeaders());
        
        // Add correlation ID
        headers.put("correlation-id", generateCorrelationId(context, event));
        
        return event.withHeaders(headers);
    }
    
    @Override
    public void recordSpanEvent(TraceContext context, String eventName, Map<String, String> attributes) {
        if (!config.isEnabled() || context == null) {
            return;
        }
        
        TraceSpan span = activeSpans.get(context.getSpanId());
        if (span != null) {
            span.addEvent(new SpanEvent(eventName, Instant.now(), attributes));
            totalEvents.increment();
        }
        
        // Notify listeners
        notifySpanListeners(span);
        
        LOGGER.fine("Recorded span event: " + eventName + " in trace: " + context.getTraceId());
    }
    
    @Override
    public void recordError(TraceContext context, Throwable error) {
        if (!config.isEnabled() || context == null) {
            return;
        }
        
        TraceSpan span = activeSpans.get(context.getSpanId());
        if (span != null) {
            span.recordError(error);
            totalErrors.increment();
            
            // Add error attributes
            span.addAttribute("error", "true");
            span.addAttribute("error.type", error.getClass().getSimpleName());
            span.addAttribute("error.message", error.getMessage());
            
            // Add stack trace if configured
            if (config.isIncludeStackTrace()) {
                span.addAttribute("error.stack", getStackTrace(error));
            }
        }
        
        LOGGER.warning("Recorded error in trace " + context.getTraceId() + ": " + error.getMessage());
    }
    
    @Override
    public void finishSpan(TraceContext context) {
        if (!config.isEnabled() || context == null) {
            return;
        }
        
        TraceSpan span = activeSpans.remove(context.getSpanId());
        if (span != null) {
            span.finish();
            
            // Export span if sampled
            if (context.isSampled()) {
                exportSpan(span);
            }
            
            // Notify listeners
            notifySpanListeners(span);
        }
        
        // Clear current context if it matches
        if (context.equals(getCurrentContext())) {
            currentContext.remove();
        }
        
        LOGGER.fine("Finished span: " + context.getSpanId() + " in trace: " + context.getTraceId());
    }
    
    @Override
    public TraceContext getCurrentContext() {
        return currentContext.get();
    }
    
    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }
    
    /**
     * Add trace exporter
     */
    public void addExporter(TraceExporter exporter) {
        exporters.add(exporter);
        LOGGER.info("Added trace exporter: " + exporter.getClass().getSimpleName());
    }
    
    /**
     * Add span listener for real-time monitoring
     */
    public void addSpanListener(Consumer<TraceSpan> listener) {
        spanListeners.add(listener);
        LOGGER.info("Added span listener");
    }
    
    /**
     * Get tracing statistics
     */
    public TracingStatistics getStatistics() {
        return TracingStatistics.builder()
            .totalSpans(totalSpans.get())
            .sampledSpans(sampledSpans.get())
            .activeSpans(activeSpans.size())
            .totalEvents(totalEvents.sum())
            .totalErrors(totalErrors.sum())
            .samplingRate(config.getSamplingRate())
            .build();
    }
    
    /**
     * Create distributed trace for async operation
     */
    public CompletableFuture<TraceContext> createAsyncTrace(String operationName, 
                                                          Map<String, String> attributes) {
        return CompletableFuture.supplyAsync(() -> {
            TraceContext context = createNewTraceContext();
            TraceSpan span = createSpan(context, operationName, SpanKind.INTERNAL);
            
            attributes.forEach(span::addAttribute);
            span.addAttribute("async", "true");
            
            activeSpans.put(context.getSpanId(), span);
            setCurrentContext(context);
            
            return context;
        });
    }
    
    /**
     * Flush all pending traces
     */
    public CompletableFuture<Void> flush() {
        return CompletableFuture.runAsync(() -> {
            // Finish all active spans
            List<TraceSpan> spans = new ArrayList<>(activeSpans.values());
            for (TraceSpan span : spans) {
                span.finish();
                if (span.getContext().isSampled()) {
                    exportSpan(span);
                }
            }
            activeSpans.clear();
            
            // Flush all exporters
            for (TraceExporter exporter : exporters) {
                try {
                    exporter.flush();
                } catch (Exception e) {
                    LOGGER.warning("Error flushing exporter: " + e.getMessage());
                }
            }
            
            LOGGER.info("Flushed all traces and exporters");
        });
    }
    
    /**
     * Shutdown tracing system
     */
    public void shutdown() {
        flush().join();
        
        for (TraceExporter exporter : exporters) {
            try {
                exporter.shutdown();
            } catch (Exception e) {
                LOGGER.warning("Error shutting down exporter: " + e.getMessage());
            }
        }
        
        exporters.clear();
        spanListeners.clear();
        currentContext.remove();
        
        LOGGER.info("ComprehensiveEventTracing shutdown complete");
    }
    
    private TraceContext createNewTraceContext() {
        boolean shouldSample = sampler.shouldSample();
        return new TraceContext(
            generateTraceId(),
            generateSpanId(),
            null,
            Map.of(),
            shouldSample
        );
    }
    
    private TraceSpan createSpan(TraceContext context, String operationName, SpanKind kind) {
        return TraceSpan.builder()
            .context(context)
            .operationName(operationName)
            .kind(kind)
            .startTime(Instant.now())
            .build();
    }
    
    private TraceContext extractTraceFromEvent(GenericEvent<?, ?> event) {
        Map<String, String> headers = event.getHeaders();
        if (headers.containsKey("x-trace-id")) {
            return TraceContext.fromHeaders(headers);
        }
        return null;
    }
    
    private void setCurrentContext(TraceContext context) {
        currentContext.set(context);
    }
    
    private void exportSpan(TraceSpan span) {
        for (TraceExporter exporter : exporters) {
            try {
                exporter.export(span);
            } catch (Exception e) {
                LOGGER.warning("Error exporting span: " + e.getMessage());
            }
        }
    }
    
    private void notifySpanListeners(TraceSpan span) {
        if (span == null) return;
        
        for (Consumer<TraceSpan> listener : spanListeners) {
            try {
                listener.accept(span);
            } catch (Exception e) {
                LOGGER.warning("Error in span listener: " + e.getMessage());
            }
        }
    }
    
    private String generateCorrelationId(TraceContext context, GenericEvent<?, ?> event) {
        return context.getTraceId() + "-" + event.getEventType() + "-" + event.getEventId();
    }
    
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private String generateSpanId() {
        return UUID.randomUUID().toString().substring(0, 16).replace("-", "");
    }
    
    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
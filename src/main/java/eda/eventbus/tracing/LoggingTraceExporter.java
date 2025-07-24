package eda.eventbus.tracing;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Trace exporter that logs spans to Java logging
 */
public class LoggingTraceExporter implements TraceExporter {
    private static final Logger LOGGER = Logger.getLogger(LoggingTraceExporter.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT;
    
    private final Level logLevel;
    private final boolean includeAttributes;
    private final boolean includeEvents;
    private final AtomicLong exportedSpans = new AtomicLong();
    private volatile boolean healthy = true;
    
    public LoggingTraceExporter() {
        this(Level.INFO, true, true);
    }
    
    public LoggingTraceExporter(Level logLevel, boolean includeAttributes, boolean includeEvents) {
        this.logLevel = logLevel;
        this.includeAttributes = includeAttributes;
        this.includeEvents = includeEvents;
    }
    
    @Override
    public CompletableFuture<Void> export(TraceSpan span) {
        return CompletableFuture.runAsync(() -> {
            try {
                logSpan(span);
                exportedSpans.incrementAndGet();
            } catch (Exception e) {
                LOGGER.warning("Failed to export span: " + e.getMessage());
                healthy = false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> export(List<TraceSpan> spans) {
        return CompletableFuture.runAsync(() -> {
            for (TraceSpan span : spans) {
                try {
                    logSpan(span);
                    exportedSpans.incrementAndGet();
                } catch (Exception e) {
                    LOGGER.warning("Failed to export span: " + e.getMessage());
                    healthy = false;
                }
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> flush() {
        // Nothing to flush for logging exporter
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        LOGGER.info("LoggingTraceExporter shutdown. Total spans exported: " + exportedSpans.get());
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public String getName() {
        return "LoggingTraceExporter";
    }
    
    @Override
    public boolean isHealthy() {
        return healthy;
    }
    
    private void logSpan(TraceSpan span) {
        StringBuilder logMessage = new StringBuilder();
        
        // Basic span information
        logMessage.append("TRACE ")
                  .append("traceId=").append(span.getTraceId())
                  .append(" spanId=").append(span.getSpanId());
        
        if (span.getContext().getParentSpanId() != null) {
            logMessage.append(" parentSpanId=").append(span.getContext().getParentSpanId());
        }
        
        logMessage.append(" operation=").append(span.getOperationName())
                  .append(" kind=").append(span.getKind())
                  .append(" status=").append(span.getStatus())
                  .append(" duration=").append(span.getDuration().toMillis()).append("ms")
                  .append(" start=").append(TIMESTAMP_FORMAT.format(span.getStartTime()));
        
        if (span.getEndTime() != null) {
            logMessage.append(" end=").append(TIMESTAMP_FORMAT.format(span.getEndTime()));
        }
        
        // Add attributes if enabled
        if (includeAttributes && !span.getAttributes().isEmpty()) {
            logMessage.append(" attributes={");
            span.getAttributes().forEach((key, value) -> 
                logMessage.append(key).append("=").append(value).append(" "));
            logMessage.append("}");
        }
        
        // Add events if enabled
        if (includeEvents && !span.getEvents().isEmpty()) {
            logMessage.append(" events=[");
            for (SpanEvent event : span.getEvents()) {
                logMessage.append("{name=").append(event.getName())
                          .append(" time=").append(TIMESTAMP_FORMAT.format(event.getTimestamp()));
                if (!event.getAttributes().isEmpty()) {
                    logMessage.append(" attrs=").append(event.getAttributes());
                }
                logMessage.append("} ");
            }
            logMessage.append("]");
        }
        
        // Add error information if present
        if (span.hasError() && span.getError() != null) {
            logMessage.append(" error=").append(span.getError().getClass().getSimpleName())
                      .append(" errorMessage=").append(span.getError().getMessage());
        }
        
        // Log at specified level
        LOGGER.log(logLevel, logMessage.toString());
    }
    
    /**
     * Get number of exported spans
     */
    public long getExportedSpansCount() {
        return exportedSpans.get();
    }
    
    /**
     * Reset health status
     */
    public void resetHealth() {
        healthy = true;
    }
}
package eda.eventbus.tracing;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for exporting trace spans to external systems
 */
public interface TraceExporter {
    
    /**
     * Export a single span
     */
    CompletableFuture<Void> export(TraceSpan span);
    
    /**
     * Export multiple spans
     */
    CompletableFuture<Void> export(List<TraceSpan> spans);
    
    /**
     * Flush any pending exports
     */
    CompletableFuture<Void> flush();
    
    /**
     * Shutdown the exporter
     */
    CompletableFuture<Void> shutdown();
    
    /**
     * Get exporter name
     */
    String getName();
    
    /**
     * Check if exporter is healthy
     */
    boolean isHealthy();
}
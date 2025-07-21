package eda.eventbus.metrics;

/**
 * Supported metrics export formats
 */
public enum MetricsFormat {
    /**
     * JSON format
     */
    JSON,
    
    /**
     * Prometheus format
     */
    PROMETHEUS,
    
    /**
     * Plain text format
     */
    TEXT,
    
    /**
     * CSV format
     */
    CSV
}
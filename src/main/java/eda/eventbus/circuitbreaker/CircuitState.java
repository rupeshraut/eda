package eda.eventbus.circuitbreaker;

/**
 * Circuit breaker states
 */
public enum CircuitState {
    /**
     * Circuit is closed - requests flow normally
     */
    CLOSED,
    
    /**
     * Circuit is open - requests are blocked
     */
    OPEN,
    
    /**
     * Circuit is half-open - limited requests allowed to test recovery
     */
    HALF_OPEN
}
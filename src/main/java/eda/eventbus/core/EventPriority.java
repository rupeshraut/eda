package eda.eventbus.core;

/**
 * Event priority levels for processing order
 */
public enum EventPriority {
    LOW(1),
    NORMAL(5),
    HIGH(10),
    CRITICAL(20);
    
    private final int value;
    
    EventPriority(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public boolean isHigherThan(EventPriority other) {
        return this.value > other.value;
    }
}
/**
 * Event-Driven Architecture (EDA) Reference Implementation in Modern Java
 * <p>
 * This implementation demonstrates the three core components of Event-Driven Architecture:
 * 1. Event Producers - Generate events
 * 2. Event Bus/Broker - Distribute events
 * 3. Event Consumers - Process events
 * <p>
 * Uses Java 17 features: records, var, functional interfaces, streams, and text blocks
 */

package eda.event;

/**
 * 1. EVENT DEFINITIONS
 */
// Enum for event types
public enum EventType {
    /**
     * User created event type.
     */
    USER_CREATED,
    /**
     * User updated event type.
     */
    USER_UPDATED,
    /**
     * Order placed event type.
     */
    ORDER_PLACED,
    /**
     * Order processed event type.
     */
    ORDER_PROCESSED,
    /**
     * Payment received event type.
     */
    PAYMENT_RECEIVED,
    /**
     * Inventory updated event type.
     */
    INVENTORY_UPDATED,
    SYSTEM_HEALTH,        // For health monitoring
    CIRCUIT_BREAKER       // For circuit breaker pattern
}



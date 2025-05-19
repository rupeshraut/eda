package eda.saga;

/**
 * 7. SAGA PATTERN IMPLEMENTATION
 * <p>
 * The Saga pattern helps manage distributed transactions across multiple services
 * by defining a sequence of local transactions and compensating actions.
 * There are two main types of sagas:
 * - Choreography-based: Events trigger the next step
 * - Orchestration-based: A central coordinator manages the workflow
 * <p>
 * Below is an implementation of both approaches.
 */

// Additional event types for saga
enum SagaEventType {
    ORDER_SAGA_STARTED,
    PAYMENT_PROCESSED,
    PAYMENT_FAILED,
    INVENTORY_RESERVED,
    INVENTORY_RESERVATION_FAILED,
    SHIPPING_PREPARED,
    SHIPPING_PREPARATION_FAILED,
    ORDER_COMPLETED,
    ORDER_FAILED,

    // Compensation events
    PAYMENT_REFUNDED,
    INVENTORY_RELEASED
}
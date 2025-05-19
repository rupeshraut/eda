package eda.resiliency;

import eda.event.Event;
import eda.event.EventBus;
import eda.event.EventType;

import java.time.Instant;
import java.util.Map;

/**
 * Circuit Breaker Pattern
 * <p>
 * Prevents a cascade of failures by temporarily stopping operations
 * when a dependent service is failing or slow.
 */
public class CircuitBreaker {
    private final EventBus eventBus;
    private final String serviceName;
    private State state = State.CLOSED;
    private final int failureThreshold;
    private final int resetTimeout;
    private final int halfOpenSuccessThreshold;
    private int failureCount;
    private long lastFailureTime;
    private int halfOpenSuccessCount;
    public CircuitBreaker(String serviceName, EventBus eventBus, int failureThreshold,
                          int resetTimeoutMs, int halfOpenSuccessThreshold) {
        this.serviceName = serviceName;
        this.eventBus = eventBus;
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeoutMs;
        this.halfOpenSuccessThreshold = halfOpenSuccessThreshold;
    }

    public synchronized <T> T execute(CircuitBreakerFunction<T> function) throws Exception {
        if (isOpen() && !shouldTryHalfOpen()) {
            publishStateChangeEvent();
            throw new CircuitBreakerOpenException("Circuit breaker is open for " + serviceName);
        }

        try {
            T result = function.apply();

            // Handle success
            if (state == State.HALF_OPEN) {
                halfOpenSuccessCount++;

                if (halfOpenSuccessCount >= halfOpenSuccessThreshold) {
                    transitionToClosed();
                }
            } else {
                // Reset failure count on success in closed state
                failureCount = 0;
            }

            return result;
        } catch (Exception e) {
            // Handle failure
            handleFailure();
            throw e;
        }
    }

    private synchronized void handleFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();

        if ((state == State.CLOSED && failureCount >= failureThreshold)
                || state == State.HALF_OPEN) {
            transitionToOpen();
        }
    }

    private synchronized void transitionToOpen() {
        if (state != State.OPEN) {
            state = State.OPEN;
            publishStateChangeEvent();
            System.out.println(serviceName + " circuit breaker transitioned to OPEN");
        }
    }

    private synchronized void transitionToHalfOpen() {
        if (state == State.OPEN) {
            state = State.HALF_OPEN;
            halfOpenSuccessCount = 0;
            publishStateChangeEvent();
            System.out.println(serviceName + " circuit breaker transitioned to HALF_OPEN");
        }
    }

    private synchronized void transitionToClosed() {
        if (state != State.CLOSED) {
            state = State.CLOSED;
            failureCount = 0;
            publishStateChangeEvent();
            System.out.println(serviceName + " circuit breaker transitioned to CLOSED");
        }
    }

    private synchronized boolean isOpen() {
        return state == State.OPEN;
    }

    private synchronized boolean shouldTryHalfOpen() {
        long elapsedTime = System.currentTimeMillis() - lastFailureTime;
        if (state == State.OPEN && elapsedTime >= resetTimeout) {
            transitionToHalfOpen();
            return true;
        }
        return state == State.HALF_OPEN;
    }

    private void publishStateChangeEvent() {
        if (eventBus != null) {
            Map<String, Object> data = Map.of(
                    "serviceName", serviceName,
                    "state", state.toString(),
                    "failureCount", failureCount,
                    "timestamp", Instant.now().toString()
            );

            Event event = Event.of(EventType.CIRCUIT_BREAKER, data, "CircuitBreaker");
            eventBus.publish(event);
        }
    }

    public enum State {
        CLOSED,    // Normal operation, requests pass through
        OPEN,      // Failure detected, requests are blocked
        HALF_OPEN  // Testing if service has recovered
    }

    @FunctionalInterface
    public interface CircuitBreakerFunction<T> {
        T apply() throws Exception;
    }

    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
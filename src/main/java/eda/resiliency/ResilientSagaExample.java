package eda.resiliency;

import eda.event.Event;
import eda.event.EventBus;
import eda.event.EventType;
import eda.monitor.HealthMonitor;
import eda.observability.DistributedTracing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 10. INTEGRATION WITH SAGAS
 * <p>
 * Example of how to use these resilience patterns with the Saga pattern
 */
public class ResilientSagaExample {

    private static final Logger LOGGER = Logger.getLogger(ResilientSagaExample.class.getName());

    private ResilientSagaExample() {
    }

    // Service using all resilience patterns
    public static class ResilientPaymentService {
        private final EventBus eventBus;
        private final CircuitBreaker circuitBreaker;
        private final RetryExecutor retryExecutor;
        private final TimeoutExecutor timeoutExecutor;
        private final DistributedTracing.TracingService tracingService;

        public ResilientPaymentService(EventBus eventBus, DistributedTracing.TracingService tracingService) {
            this.eventBus = eventBus;
            this.circuitBreaker = new CircuitBreaker("PaymentService", eventBus, 3, 5000, 2);
            this.retryExecutor = new RetryExecutor(3, 100, 2000, 2.0);
            this.timeoutExecutor = new TimeoutExecutor();
            this.tracingService = tracingService;

            // Report service health periodically
            reportHealth("UP", Map.of("activeTransactions", 0));
        }

        public String processPayment(String userId, double amount, DistributedTracing.TraceContext parentContext)
                throws Exception {

            // Start a span for this operation
            var span = tracingService.startSpan("processPayment", parentContext);
            span.addTag("userId", userId)
                    .addTag("amount", String.valueOf(amount));

            try {
                // Execute with all resilience patterns combined
                String paymentId = circuitBreaker.execute(() ->
                        retryExecutor.executeWithRetry(() ->
                                timeoutExecutor.executeWithTimeout(() -> {
                                    // Simulate payment processing
                                    span.addEvent("payment_started");

                                    // Simulate random delays
                                    try {
                                        Thread.sleep((long) (ThreadLocalRandom.current().nextDouble() * 100));
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }

                                    // Simulate occasional failures
                                    if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                                        span.addEvent("payment_error", Map.of("reason", "service_failure"));
                                        throw new RuntimeException("Payment processor error");
                                    }

                                    String result = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
                                    span.addEvent("payment_completed", Map.of("paymentId", result));

                                    // Publish health update
                                    reportHealth("UP", Map.of("lastProcessedAmount", amount));
                                    return result;
                                }, 500) // 500ms timeout
                        )
                );

                return paymentId;
            } catch (Exception e) {
                span.addEvent("payment_failed", Map.of("error", e.getMessage()));

                // Report degraded service health
                reportHealth("DEGRADED", Map.of("lastError", e.getMessage()));
                throw e;
            } finally {
                // Always report the span
                tracingService.reportSpan(span.end());
            }
        }

        private void reportHealth(String status, Map<String, Object> metrics) {
            Map<String, Object> healthData = new HashMap<>();
            healthData.put("serviceName", "PaymentService");
            healthData.put("status", status);
            healthData.put("metrics", metrics);

            Event healthEvent = Event.of(EventType.SYSTEM_HEALTH, healthData, "PaymentService");
            eventBus.publish(healthEvent);
        }
    }

    // Example of using the resilient payment service in a saga
    public static void demonstrateResilientSaga() {
        var eventBus = new EventBus();
        var tracingService = new DistributedTracing.SimpleTracingService();
        var healthMonitor = new HealthMonitor(eventBus);

        // Register and monitor services
        healthMonitor.registerService("PaymentService");
        healthMonitor.startMonitoring(5);

        // Create resilient service
        var paymentService = new ResilientPaymentService(eventBus, tracingService);

        // Create a trace context for the saga
        var sagaContext = DistributedTracing.TraceContext.createNew();
        var sagaSpan = tracingService.startSpan("orderSaga", sagaContext);

        LOGGER.log(Level.INFO, "Starting resilient saga with distributed tracing");

        try {
            // Execute the payment step with resilience patterns
            String paymentId = paymentService.processPayment("user123", 99.99, sagaContext);
            sagaSpan.addEvent("payment_step_completed", Map.of("paymentId", paymentId));

            LOGGER.log(Level.INFO, "Payment processed successfully: {}" , paymentId);

            // Next steps in the saga would follow...

        } catch (Exception e) {
            sagaSpan.addEvent("saga_failed", Map.of("reason", e.getMessage()));
            LOGGER.log(Level.INFO, "Saga failed: {}" , e.getMessage());

            // Compensating transactions would be triggered here
        } finally {
            tracingService.reportSpan(sagaSpan.end());
        }

        // Cleanup
        healthMonitor.shutdown();
    }
}
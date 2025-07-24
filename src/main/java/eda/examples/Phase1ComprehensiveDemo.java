package eda.examples;

import eda.eventbus.EventBusConfig;
import eda.eventbus.GenericEventBus;
import eda.eventbus.core.GenericEvent;
import eda.eventbus.core.DefaultEvent;
import eda.eventbus.dlq.*;
import eda.eventbus.schema.*;
import eda.eventbus.tracing.*;
import eda.eventbus.subscription.EventConsumer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Comprehensive demonstration of Phase 1 production readiness features:
 * 1. Dead Letter Queue & Poison Message Handling
 * 2. Event Schema Evolution & Registry
 * 3. Event Tracing & Distributed Logging
 */
public class Phase1ComprehensiveDemo {
    private static final Logger LOGGER = Logger.getLogger(Phase1ComprehensiveDemo.class.getName());

    public static void main(String[] args) {
        new Phase1ComprehensiveDemo().runDemo();
    }

    public void runDemo() {
        LOGGER.info("=== Phase 1 Production Readiness Features Demo ===");

        try {
            // 1. Setup infrastructure
            ProductionInfrastructure infrastructure = setupInfrastructure();

            // 2. Demonstrate Dead Letter Queue & Poison Message Handling
            demonstrateDeadLetterQueue(infrastructure);

            // 3. Demonstrate Event Schema Evolution & Registry
            demonstrateSchemaEvolution(infrastructure);

            // 4. Demonstrate Event Tracing & Distributed Logging
            demonstrateDistributedTracing(infrastructure);

            // 5. Demonstrate integrated features working together
            demonstrateIntegratedFeatures(infrastructure);

            // 6. Show production monitoring and observability
            showProductionMonitoring(infrastructure);

            // Wait for async operations to complete
            Thread.sleep(3000);

            // 7. Cleanup
            cleanup(infrastructure);

        } catch (Exception e) {
            LOGGER.severe("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }

        LOGGER.info("=== Phase 1 Demo Complete ===");
    }

    private ProductionInfrastructure setupInfrastructure() {
        LOGGER.info("1. Setting up Production Infrastructure...");

        // Configure event bus for production
        EventBusConfig eventBusConfig = EventBusConfig.builder()
            .enableMetrics(true)
            .build();

        GenericEventBus<String> eventBus = new GenericEventBus<>(eventBusConfig);

        // Setup Dead Letter Queue with poison message handling
        InMemoryDeadLetterQueue<String> dlq = new InMemoryDeadLetterQueue<>(1000, Duration.ofHours(24));
        PoisonMessagePolicy poisonPolicy = PoisonMessagePolicy.defaultPolicy();
        PoisonMessageHandler<String> poisonHandler = new PoisonMessageHandler<>(poisonPolicy, dlq);

        // Setup Schema Registry
        InMemoryEventSchemaRegistry schemaRegistry = new InMemoryEventSchemaRegistry(
            SchemaRegistryConfig.defaultConfig());

        // Setup Distributed Tracing
        TracingConfig tracingConfig = TracingConfig.builder()
            .enabled(true)
            .samplingRate(1.0)
            .includeStackTrace(true)
            .enableAsyncTracing(true)
            .enableMetrics(true)
            .serviceName("phase1-demo")
            .build();
        ComprehensiveEventTracing tracing = new ComprehensiveEventTracing(tracingConfig);
        tracing.addExporter(new LoggingTraceExporter());

        // Setup monitoring
        tracing.addSpanListener(span -> {
            if (span.hasError()) {
                LOGGER.warning("Span with error detected: " + span);
            }
        });

        ProductionInfrastructure infrastructure = new ProductionInfrastructure(
            eventBus, dlq, poisonHandler, schemaRegistry, tracing);

        LOGGER.info("✓ Production infrastructure setup complete");
        return infrastructure;
    }

    private void demonstrateDeadLetterQueue(ProductionInfrastructure infra) {
        LOGGER.info("2. Demonstrating Dead Letter Queue & Poison Message Handling...");

        // Create events that will fail processing
        GenericEvent<String, OrderData> poisonEvent = DefaultEvent.<String, OrderData>builder()
            .eventType("OrderCreated")
            .source("demo")
            .data(new OrderData("INVALID_ORDER", -100.0, null)) // Invalid data
            .timestamp(Instant.now())
            .build();

        GenericEvent<String, OrderData> normalEvent = DefaultEvent.<String, OrderData>builder()
            .eventType("OrderCreated")
            .source("demo")
            .data(new OrderData("ORDER-123", 99.99, "customer@example.com"))
            .timestamp(Instant.now())
            .build();

        // Subscribe with a consumer that fails on invalid data
        infra.eventBus.subscribe("OrderCreated", (EventConsumer<String, OrderData>) event -> {
            OrderData order = event.getData();
            
            // Simulate processing that fails on invalid data
            if (order.orderId().startsWith("INVALID") || order.amount() < 0) {
                throw new IllegalArgumentException("Invalid order data: " + order);
            }
            
            if (order.customerEmail() == null) {
                throw new RuntimeException("Missing customer email");
            }
            
            LOGGER.info("✓ Successfully processed order: " + order.orderId());
            return CompletableFuture.completedFuture(null);
        });

        // Enable DLQ monitoring
        infra.dlq.subscribe(dlqEvent -> {
            LOGGER.info("📋 DLQ Event: " + dlqEvent);
        });

        try {
            // Publish events
            infra.eventBus.publish(normalEvent);
            infra.eventBus.publish(poisonEvent);

            // Wait for processing
            Thread.sleep(1000);

            // Check DLQ statistics
            DLQStatistics dlqStats = infra.dlq.getStatistics().join();
            LOGGER.info("📊 DLQ Statistics: " + dlqStats);

            // Demonstrate DLQ operations
            List<DeadLetterEvent<String>> dlqEvents = infra.dlq.retrieveAll().join();
            LOGGER.info("📥 Events in DLQ: " + dlqEvents.size());

            // Try to retry failed events
            for (DeadLetterEvent<String> dlqEvent : dlqEvents) {
                if (dlqEvent.isEligibleForRetry()) {
                    RetryResult result = infra.dlq.retry(dlqEvent.getId()).join();
                    LOGGER.info("🔄 Retry result: " + result);
                }
            }

            // Demonstrate poison message handling
            PoisonMessageStatistics poisonStats = infra.poisonHandler.getStatistics();
            LOGGER.info("☠️ Poison Message Statistics: " + poisonStats);

        } catch (Exception e) {
            LOGGER.warning("Error in DLQ demo: " + e.getMessage());
        }

        LOGGER.info("✓ Dead Letter Queue demonstration complete");
    }

    private void demonstrateSchemaEvolution(ProductionInfrastructure infra) {
        LOGGER.info("3. Demonstrating Event Schema Evolution & Registry...");

        try {
            // Register initial schema version
            EventSchema orderSchemaV1 = JsonEventSchema.builder()
                .eventType("OrderCreated")
                .version("1.0.0")
                .description("Initial order schema")
                .addRequiredField("orderId", FieldType.STRING)
                .addRequiredField("amount", FieldType.DOUBLE)
                .addOptionalField("customerEmail", FieldType.STRING)
                .build();

            infra.schemaRegistry.registerSchema(orderSchemaV1).join();
            LOGGER.info("📋 Registered schema v1.0.0");

            // Register evolved schema version
            EventSchema orderSchemaV2 = JsonEventSchema.builder()
                .eventType("OrderCreated")
                .version("2.0.0")
                .description("Enhanced order schema with customer ID")
                .addRequiredField("orderId", FieldType.STRING)
                .addRequiredField("amount", FieldType.DOUBLE)
                .addRequiredField("customerId", FieldType.STRING) // New required field
                .addOptionalField("customerEmail", FieldType.STRING)
                .addOptionalField("discountCode", FieldType.STRING) // New optional field
                .build();

            infra.schemaRegistry.registerSchema(orderSchemaV2).join();
            LOGGER.info("📋 Registered schema v2.0.0");

            // Test schema validation
            GenericEvent<String, Map<String, Object>> validEvent = DefaultEvent.<String, Map<String, Object>>builder()
                .eventType("OrderCreated")
                .source("demo")
                .data(Map.of(
                    "orderId", "ORDER-456",
                    "amount", 149.99,
                    "customerId", "CUST-789",
                    "customerEmail", "customer@example.com"
                ))
                .header("schemaVersion", "2.0.0")
                .build();

            ValidationResult validation = infra.schemaRegistry.validateEvent(validEvent).join();
            LOGGER.info("✅ Schema validation result: " + validation.isValid());

            // Test invalid event
            GenericEvent<String, Map<String, Object>> invalidEvent = DefaultEvent.<String, Map<String, Object>>builder()
                .eventType("OrderCreated")
                .source("demo")
                .data(Map.of(
                    "orderId", "ORDER-789"
                    // Missing required fields
                ))
                .header("schemaVersion", "2.0.0")
                .build();

            ValidationResult invalidValidation = infra.schemaRegistry.validateEvent(invalidEvent).join();
            LOGGER.info("❌ Invalid event validation: " + invalidValidation.isValid());
            if (!invalidValidation.isValid()) {
                invalidValidation.getErrors().forEach(error -> 
                    LOGGER.info("   Error: " + error));
            }

            // Demonstrate schema compatibility checking
            boolean compatible = infra.schemaRegistry.isCompatible("OrderCreated", "1.0.0", "2.0.0").join();
            LOGGER.info("🔄 Schema compatibility (1.0.0 -> 2.0.0): " + compatible);

            // Show registry statistics
            SchemaRegistryStatistics schemaStats = infra.schemaRegistry.getStatistics();
            LOGGER.info("📊 Schema Registry Statistics: " + schemaStats);

        } catch (Exception e) {
            LOGGER.warning("Error in schema demo: " + e.getMessage());
        }

        LOGGER.info("✓ Schema Evolution demonstration complete");
    }

    private void demonstrateDistributedTracing(ProductionInfrastructure infra) {
        LOGGER.info("4. Demonstrating Event Tracing & Distributed Logging...");

        try {
            // Create traced event
            GenericEvent<String, UserData> userEvent = DefaultEvent.<String, UserData>builder()
                .eventType("UserRegistered")
                .source("demo")
                .data(new UserData("john.doe@example.com", "John Doe", "premium"))
                .build();

            // Start trace for publishing
            TraceContext publishContext = infra.tracing.startPublishTrace(userEvent);
            infra.tracing.recordSpanEvent(publishContext, "validation.started", 
                Map.of("validator", "email-validator"));

            // Add tracing headers to event
            GenericEvent<String, UserData> tracedEvent = infra.tracing.addTracingHeaders(userEvent, publishContext);

            // Subscribe with traced processing
            infra.eventBus.subscribe("UserRegistered", (EventConsumer<String, UserData>) event -> {
                // Continue trace from event headers
                TraceContext processContext = infra.tracing.continueTrace(event.getHeaders());
                TraceContext childContext = infra.tracing.startProcessingTrace(event, "user-processor");

                try {
                    // Simulate processing steps with tracing
                    infra.tracing.recordSpanEvent(childContext, "user.validation", 
                        Map.of("email", event.getData().email()));
                    
                    Thread.sleep(50); // Simulate processing time
                    
                    infra.tracing.recordSpanEvent(childContext, "user.persisted", 
                        Map.of("userId", "USER-" + UUID.randomUUID().toString().substring(0, 8)));

                    LOGGER.info("✓ Processed user registration: " + event.getData().email());

                } catch (Exception e) {
                    infra.tracing.recordError(childContext, e);
                    throw new RuntimeException("User processing failed", e);
                } finally {
                    infra.tracing.finishSpan(childContext);
                }
                return CompletableFuture.completedFuture(null);
            });

            // Publish traced event
            infra.eventBus.publish(tracedEvent);

            // Finish publish trace
            infra.tracing.recordSpanEvent(publishContext, "publish.completed", Map.of());
            infra.tracing.finishSpan(publishContext);

            // Wait for processing
            Thread.sleep(1000);

            // Demonstrate async tracing
            CompletableFuture<TraceContext> asyncTrace = infra.tracing.createAsyncTrace(
                "async.user.notification", 
                Map.of("channel", "email", "template", "welcome"));

            asyncTrace.thenCompose(context -> {
                infra.tracing.recordSpanEvent(context, "email.template.loaded", Map.of());
                // Simulate async operation
                return CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(100);
                        infra.tracing.recordSpanEvent(context, "email.sent", 
                            Map.of("recipient", "john.doe@example.com"));
                    } catch (InterruptedException e) {
                        infra.tracing.recordError(context, e);
                    } finally {
                        infra.tracing.finishSpan(context);
                    }
                });
            });

            // Show tracing statistics
            TracingStatistics tracingStats = infra.tracing.getStatistics();
            LOGGER.info("📊 Tracing Statistics: " + tracingStats);

        } catch (Exception e) {
            LOGGER.warning("Error in tracing demo: " + e.getMessage());
        }

        LOGGER.info("✓ Distributed Tracing demonstration complete");
    }

    private void demonstrateIntegratedFeatures(ProductionInfrastructure infra) {
        LOGGER.info("5. Demonstrating Integrated Features...");

        try {
            CountDownLatch latch = new CountDownLatch(1);

            // Create a complex scenario combining all features
            GenericEvent<String, PaymentData> paymentEvent = DefaultEvent.<String, PaymentData>builder()
                .eventType("PaymentProcessed")
                .source("demo")
                .data(new PaymentData("PAY-123", 299.99, "USD", "credit-card"))
                .header("schemaVersion", "1.0.0")
                .build();

            // Register payment schema
            EventSchema paymentSchema = JsonEventSchema.builder()
                .eventType("PaymentProcessed")
                .version("1.0.0")
                .addRequiredField("paymentId", FieldType.STRING)
                .addRequiredField("amount", FieldType.DOUBLE)
                .addRequiredField("currency", FieldType.STRING)
                .addRequiredField("method", FieldType.STRING)
                .build();

            infra.schemaRegistry.registerSchema(paymentSchema).join();

            // Subscribe with comprehensive error handling and tracing
            infra.eventBus.subscribe("PaymentProcessed", (EventConsumer<String, PaymentData>) event -> {
                TraceContext traceContext = infra.tracing.startProcessingTrace(event, "payment-processor");
                
                try {
                    // Validate schema
                    ValidationResult validation = infra.schemaRegistry.validateEvent(event).join();
                    if (!validation.isValid()) {
                        throw new IllegalArgumentException("Schema validation failed: " + validation.getErrors());
                    }
                    
                    infra.tracing.recordSpanEvent(traceContext, "schema.validated", Map.of());
                    
                    PaymentData payment = event.getData();
                    
                    // Simulate payment processing
                    if (payment.amount() > 1000.0) {
                        throw new RuntimeException("Payment amount exceeds limit");
                    }
                    
                    infra.tracing.recordSpanEvent(traceContext, "payment.validated", 
                        Map.of("amount", payment.amount().toString(), "method", payment.method()));
                    
                    // Simulate processing delay
                    Thread.sleep(100);
                    
                    infra.tracing.recordSpanEvent(traceContext, "payment.completed", 
                        Map.of("paymentId", payment.paymentId()));
                    
                    LOGGER.info("✅ Payment processed successfully: " + payment.paymentId());
                    
                } catch (Exception e) {
                    // Record error in trace
                    infra.tracing.recordError(traceContext, e);
                    
                    // Handle with poison message detection
                    PoisonMessageResult<String> poisonResult = infra.poisonHandler.handleFailedEvent(event, e, 1);
                    
                    if (poisonResult.isPoisonMessage()) {
                        LOGGER.warning("☠️ Poison message detected: " + poisonResult.getMessage());
                    } else {
                        // Store in DLQ for retry
                        DeadLetterEvent<String> dlqEvent = DeadLetterEvent.<String>builder()
                            .originalEvent(event)
                            .addFailureReason(FailureReason.fromException(e, 1))
                            .build();
                        infra.dlq.store(dlqEvent);
                        LOGGER.info("📥 Event stored in DLQ for retry");
                    }
                    
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Processing interrupted", e);
                    } else if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new RuntimeException("Processing failed", e);
                    }
                } finally {
                    infra.tracing.finishSpan(traceContext);
                    latch.countDown();
                }
                return CompletableFuture.completedFuture(null);
            });

            // Start trace and publish
            TraceContext publishContext = infra.tracing.startPublishTrace(paymentEvent);
            GenericEvent<String, PaymentData> tracedEvent = infra.tracing.addTracingHeaders(paymentEvent, publishContext);
            
            infra.eventBus.publish(tracedEvent);
            infra.tracing.finishSpan(publishContext);

            // Wait for processing
            latch.await(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            LOGGER.warning("Error in integrated demo: " + e.getMessage());
        }

        LOGGER.info("✓ Integrated Features demonstration complete");
    }

    private void showProductionMonitoring(ProductionInfrastructure infra) {
        LOGGER.info("6. Production Monitoring & Observability...");

        try {
            // DLQ Monitoring
            DLQStatistics dlqStats = infra.dlq.getStatistics().join();
            LOGGER.info("📊 DLQ Health: " + dlqStats.getHealth() + 
                       " (Active: " + dlqStats.getActiveEvents() + 
                       ", Resolved: " + dlqStats.getResolvedEvents() + ")");

            // Schema Registry Monitoring
            SchemaRegistryStatistics schemaStats = infra.schemaRegistry.getStatistics();
            LOGGER.info("📊 Schema Registry: " + schemaStats.getTotalSchemas() + " schemas, " +
                       schemaStats.getValidationCount() + " validations");

            // Tracing Monitoring
            TracingStatistics tracingStats = infra.tracing.getStatistics();
            LOGGER.info("📊 Tracing: " + tracingStats.getTotalSpans() + " spans (" +
                       String.format("%.1f%%", tracingStats.getActualSamplingRate() * 100) + " sampled), " +
                       tracingStats.getTotalErrors() + " errors");

            // Poison Message Monitoring
            PoisonMessageStatistics poisonStats = infra.poisonHandler.getStatistics();
            LOGGER.info("📊 Poison Messages: " + poisonStats.getTotalPoisonMessages() + " detected, " +
                       poisonStats.getTotalQuarantined() + " quarantined");

            // Overall system health
            boolean systemHealthy = dlqStats.isHealthy() && 
                                  schemaStats.isHealthy() && 
                                  tracingStats.isHealthy() && 
                                  poisonStats.isHealthy();

            LOGGER.info("🏥 Overall System Health: " + (systemHealthy ? "✅ HEALTHY" : "⚠️ ISSUES DETECTED"));

        } catch (Exception e) {
            LOGGER.warning("Error in monitoring demo: " + e.getMessage());
        }

        LOGGER.info("✓ Production Monitoring demonstration complete");
    }

    private void cleanup(ProductionInfrastructure infra) {
        LOGGER.info("7. Cleaning up resources...");

        try {
            infra.tracing.flush().join();
            infra.tracing.shutdown();
            infra.dlq.shutdown();
            infra.eventBus.shutdown();

            LOGGER.info("✓ Cleanup complete");
        } catch (Exception e) {
            LOGGER.warning("Error during cleanup: " + e.getMessage());
        }
    }

    // Data classes for demo
    record OrderData(String orderId, Double amount, String customerEmail) {}
    record UserData(String email, String name, String tier) {}
    record PaymentData(String paymentId, Double amount, String currency, String method) {}

    // Infrastructure container
    static class ProductionInfrastructure {
        final GenericEventBus<String> eventBus;
        final InMemoryDeadLetterQueue<String> dlq;
        final PoisonMessageHandler<String> poisonHandler;
        final InMemoryEventSchemaRegistry schemaRegistry;
        final ComprehensiveEventTracing tracing;

        ProductionInfrastructure(GenericEventBus<String> eventBus,
                               InMemoryDeadLetterQueue<String> dlq,
                               PoisonMessageHandler<String> poisonHandler,
                               InMemoryEventSchemaRegistry schemaRegistry,
                               ComprehensiveEventTracing tracing) {
            this.eventBus = eventBus;
            this.dlq = dlq;
            this.poisonHandler = poisonHandler;
            this.schemaRegistry = schemaRegistry;
            this.tracing = tracing;
        }
    }
}
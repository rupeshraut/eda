package eda.saga;

import eda.event.Event;
import eda.event.EventBus;
import eda.event.EventType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Orchestration-based Saga implementation
 * <p>
 * In this approach, a central coordinator (the orchestrator) manages the entire process.
 * The orchestrator knows the saga steps and directs the participating services.
 */
public class OrchestrationSaga {

    // Demo method
    public static void demonstrateOrchestrationSaga() {
        var eventBus = new EventBus();
        var orchestrator = new OrderSagaOrchestrator(eventBus);

        // Start a saga
        orchestrator.startOrderSaga("ORD-123", "USER-456", 99.99, "PROD-789", 2);
    }

    // Saga states
    enum OrderSagaState {
        STARTED,
        PAYMENT_PROCESSING,
        PAYMENT_COMPLETED,
        INVENTORY_RESERVING,
        INVENTORY_RESERVED,
        SHIPPING_PREPARING,
        SHIPPING_PREPARED,
        COMPLETED,
        FAILED
    }

    // Saga data - passed between steps
    record OrderSagaData(
            String orderId,
            String userId,
            double amount,
            String productId,
            int quantity,
            String paymentId,
            String inventoryId,
            String shippingId,
            String errorReason
    ) {
        // Data updaters
        public OrderSagaData withPaymentId(String paymentId) {
            return new OrderSagaData(orderId, userId, amount, productId, quantity,
                    paymentId, inventoryId, shippingId, errorReason);
        }

        public OrderSagaData withInventoryId(String inventoryId) {
            return new OrderSagaData(orderId, userId, amount, productId, quantity,
                    paymentId, inventoryId, shippingId, errorReason);
        }

        public OrderSagaData withShippingId(String shippingId) {
            return new OrderSagaData(orderId, userId, amount, productId, quantity,
                    paymentId, inventoryId, shippingId, errorReason);
        }

        public OrderSagaData withError(String errorReason) {
            return new OrderSagaData(orderId, userId, amount, productId, quantity,
                    paymentId, inventoryId, shippingId, errorReason);
        }
    }

    // Saga orchestrator
    public static class OrderSagaOrchestrator {
        private final EventBus eventBus;
        private final Map<UUID, OrderSagaState> sagaStates = new ConcurrentHashMap<>();
        private final Map<UUID, OrderSagaData> sagaData = new ConcurrentHashMap<>();

        // Service references
        private final PaymentService paymentService;
        private final InventoryService inventoryService;
        private final ShippingService shippingService;

        public OrderSagaOrchestrator(EventBus eventBus) {
            this.eventBus = eventBus;

            // Create service instances
            this.paymentService = new PaymentService();
            this.inventoryService = new InventoryService();
            this.shippingService = new ShippingService();

            // Subscribe to saga events
            eventBus.subscribe(EventType.PAYMENT_RECEIVED, this::handlePaymentResult);
            eventBus.subscribe(EventType.INVENTORY_UPDATED, this::handleInventoryResult);
            eventBus.subscribe(EventType.ORDER_PROCESSED, this::handleOrderResult);
        }

        // Start a new saga
        public UUID startOrderSaga(String orderId, String userId, double amount, String productId, int quantity) {
            // Create saga ID and initial data
            UUID sagaId = UUID.randomUUID();
            OrderSagaData data = new OrderSagaData(orderId, userId, amount, productId, quantity,
                    null, null, null, null);

            // Store saga state and data
            sagaStates.put(sagaId, OrderSagaState.STARTED);
            sagaData.put(sagaId, data);

            System.out.println("Starting Order Saga " + sagaId + " for order: " + orderId);

            // Publish saga started event
            publishSagaEvent(sagaId, SagaEventType.ORDER_SAGA_STARTED,
                    Map.of("orderId", orderId, "userId", userId, "amount", amount));

            // Move to first step
            processPayment(sagaId);

            return sagaId;
        }

        // Step 1: Process payment
        private void processPayment(UUID sagaId) {
            OrderSagaData data = sagaData.get(sagaId);
            sagaStates.put(sagaId, OrderSagaState.PAYMENT_PROCESSING);

            System.out.println("Saga " + sagaId + ": Processing payment for order " + data.orderId());

            try {
                // Call payment service
                String paymentId = paymentService.processPayment(data.userId(), data.amount());

                // Update saga data with payment ID
                sagaData.put(sagaId, data.withPaymentId(paymentId));

                // Publish payment processed event
                publishSagaEvent(sagaId, SagaEventType.PAYMENT_PROCESSED,
                        Map.of("orderId", data.orderId(), "paymentId", paymentId));

                // Move to next step
                reserveInventory(sagaId);
            } catch (Exception e) {
                // Handle payment failure
                failSaga(sagaId, "Payment failed: " + e.getMessage());
            }
        }

        // Step 2: Reserve inventory
        private void reserveInventory(UUID sagaId) {
            OrderSagaData data = sagaData.get(sagaId);
            sagaStates.put(sagaId, OrderSagaState.INVENTORY_RESERVING);

            System.out.println("Saga " + sagaId + ": Reserving inventory for order " + data.orderId());

            try {
                // Call inventory service
                String inventoryId = inventoryService.reserveInventory(data.productId(), data.quantity());

                // Update saga data with inventory ID
                sagaData.put(sagaId, data.withInventoryId(inventoryId));

                // Publish inventory reserved event
                publishSagaEvent(sagaId, SagaEventType.INVENTORY_RESERVED,
                        Map.of("orderId", data.orderId(), "inventoryId", inventoryId));

                // Move to next step
                prepareShipping(sagaId);
            } catch (Exception e) {
                // Handle inventory failure
                failSaga(sagaId, "Inventory reservation failed: " + e.getMessage());
            }
        }

        // Step 3: Prepare shipping
        private void prepareShipping(UUID sagaId) {
            OrderSagaData data = sagaData.get(sagaId);
            sagaStates.put(sagaId, OrderSagaState.SHIPPING_PREPARING);

            System.out.println("Saga " + sagaId + ": Preparing shipping for order " + data.orderId());

            try {
                // Call shipping service
                String shippingId = shippingService.prepareShipping(data.orderId(), data.userId());

                // Update saga data with shipping ID
                sagaData.put(sagaId, data.withShippingId(shippingId));

                // Publish shipping prepared event
                publishSagaEvent(sagaId, SagaEventType.SHIPPING_PREPARED,
                        Map.of("orderId", data.orderId(), "shippingId", shippingId));

                // Complete the saga
                completeSaga(sagaId);
            } catch (Exception e) {
                // Handle shipping failure
                failSaga(sagaId, "Shipping preparation failed: " + e.getMessage());
            }
        }

        // Complete saga successfully
        private void completeSaga(UUID sagaId) {
            OrderSagaData data = sagaData.get(sagaId);
            sagaStates.put(sagaId, OrderSagaState.COMPLETED);

            System.out.println("Saga " + sagaId + ": Completed successfully for order " + data.orderId());

            // Publish saga completed event
            publishSagaEvent(sagaId, SagaEventType.ORDER_COMPLETED,
                    Map.of("orderId", data.orderId()));
        }

        // Fail saga and trigger compensations
        private void failSaga(UUID sagaId, String reason) {
            OrderSagaData data = sagaData.get(sagaId);
            sagaStates.put(sagaId, OrderSagaState.FAILED);

            // Store failure reason
            sagaData.put(sagaId, data.withError(reason));

            System.out.println("Saga " + sagaId + ": Failed for order " + data.orderId() + ": " + reason);

            // Publish saga failed event
            publishSagaEvent(sagaId, SagaEventType.ORDER_FAILED,
                    Map.of("orderId", data.orderId(), "reason", reason));

            // Trigger compensating transactions based on current state
            OrderSagaState state = sagaStates.get(sagaId);

            // Execute compensations in reverse order
            switch (state) {
                case SHIPPING_PREPARING, SHIPPING_PREPARED -> {
                    compensateShipping(sagaId);
                    compensateInventory(sagaId);
                    compensatePayment(sagaId);
                }
                case INVENTORY_RESERVING, INVENTORY_RESERVED -> {
                    compensateInventory(sagaId);
                    compensatePayment(sagaId);
                }
                case PAYMENT_PROCESSING, PAYMENT_COMPLETED -> {
                    compensatePayment(sagaId);
                }
                default -> {
                    // No compensation needed
                }
            }
        }

        // Compensation for payment
        private void compensatePayment(UUID sagaId) {
            OrderSagaData data = sagaData.get(sagaId);

            if (data.paymentId() != null) {
                System.out.println("Saga " + sagaId + ": Compensating payment for order " + data.orderId());

                try {
                    // Call payment service to refund
                    paymentService.refundPayment(data.paymentId(), data.amount());

                    // Publish payment refunded event
                    publishSagaEvent(sagaId, SagaEventType.PAYMENT_REFUNDED,
                            Map.of("orderId", data.orderId(), "paymentId", data.paymentId()));
                } catch (Exception e) {
                    System.err.println("Failed to compensate payment: " + e.getMessage());
                    // In a real system, we'd have a compensation recovery mechanism
                }
            }
        }

        // Compensation for inventory
        private void compensateInventory(UUID sagaId) {
            OrderSagaData data = sagaData.get(sagaId);

            if (data.inventoryId() != null) {
                System.out.println("Saga " + sagaId + ": Compensating inventory for order " + data.orderId());

                try {
                    // Call inventory service to release
                    inventoryService.releaseInventory(data.inventoryId());

                    // Publish inventory released event
                    publishSagaEvent(sagaId, SagaEventType.INVENTORY_RELEASED,
                            Map.of("orderId", data.orderId(), "inventoryId", data.inventoryId()));
                } catch (Exception e) {
                    System.err.println("Failed to compensate inventory: " + e.getMessage());
                    // In a real system, we'd have a compensation recovery mechanism
                }
            }
        }

        // Compensation for shipping
        private void compensateShipping(UUID sagaId) {
            OrderSagaData data = sagaData.get(sagaId);

            if (data.shippingId() != null) {
                System.out.println("Saga " + sagaId + ": Compensating shipping for order " + data.orderId());

                try {
                    // Call shipping service to cancel
                    shippingService.cancelShipping(data.shippingId());
                } catch (Exception e) {
                    System.err.println("Failed to compensate shipping: " + e.getMessage());
                    // In a real system, we'd have a compensation recovery mechanism
                }
            }
        }

        // Helper method to publish saga events
        private void publishSagaEvent(UUID sagaId, SagaEventType eventType, Map<String, Object> data) {
            SagaEvent event = SagaEvent.of(sagaId, eventType, data, "OrderSagaOrchestrator");
            System.out.println("Publishing saga event: " + event);
            // In a real system, we'd publish this to the event bus
        }

        // Event handlers for service responses
        private void handlePaymentResult(Event event) {
            // Would extract sagaId from event and update saga state
        }

        private void handleInventoryResult(Event event) {
            // Would extract sagaId from event and update saga state
        }

        private void handleOrderResult(Event event) {
            // Would extract sagaId from event and update saga state
        }
    }

    // Service implementations for the orchestration saga
    public static class PaymentService {
        public String processPayment(String userId, double amount) {
            // Simulate payment processing
            if (ThreadLocalRandom.current().nextDouble() < 0.1) { // 10% failure rate
                throw new RuntimeException("Payment processor error");
            }

            String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
            System.out.println("Payment processed: " + paymentId + " for $" + amount);
            return paymentId;
        }

        public void refundPayment(String paymentId, double amount) {
            System.out.println("Payment refunded: " + paymentId + " for $" + amount);
            // Refund logic
        }
    }

    public static class InventoryService {
        public String reserveInventory(String productId, int quantity) {
            // Simulate inventory reservation
            if (ThreadLocalRandom.current().nextDouble() < 0.2) { // 20% failure rate
                throw new RuntimeException("Insufficient inventory");
            }

            String inventoryId = "INV-" + UUID.randomUUID().toString().substring(0, 8);
            System.out.println("Inventory reserved: " + inventoryId + " for product " + productId);
            return inventoryId;
        }

        public void releaseInventory(String inventoryId) {
            System.out.println("Inventory released: " + inventoryId);
            // Release logic
        }
    }

    public static class ShippingService {
        public String prepareShipping(String orderId, String userId) {
            // Simulate shipping preparation
            if (ThreadLocalRandom.current().nextDouble() < 0.05) { // 5% failure rate
                throw new RuntimeException("Shipping service unavailable");
            }

            String shippingId = "SHP-" + UUID.randomUUID().toString().substring(0, 8);
            System.out.println("Shipping prepared: " + shippingId + " for order " + orderId);
            return shippingId;
        }

        public void cancelShipping(String shippingId) {
            System.out.println("Shipping cancelled: " + shippingId);
            // Cancel shipping logic
        }
    }
}
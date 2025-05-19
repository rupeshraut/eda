package eda.saga;

import eda.event.Event;
import eda.event.EventBus;
import eda.event.EventType;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Choreography-based Saga implementation
 * <p>
 * In this approach, each service performs its local transaction and publishes events
 * that trigger the next steps in the saga. Services also listen for failure events
 * to execute compensating transactions.
 */
public class ChoreographySaga {

    // Demo method
    public static void demonstrateChoreographySaga(EventBus eventBus) {
        var orderService = new OrderService(eventBus);
        var paymentService = new PaymentService(eventBus);
        var inventoryService = new InventoryService(eventBus);

        // Start a saga by creating an order
        orderService.createOrder("ORD-123", "USER-456", 99.99, "PROD-789", 2);
    }

    // Order Service
    public static class OrderService {
        private final EventBus eventBus;

        public OrderService(EventBus eventBus) {
            this.eventBus = eventBus;

            // Subscribe to relevant events
            eventBus.subscribe(EventType.PAYMENT_RECEIVED, this::handlePaymentReceived);
            eventBus.subscribe(EventType.INVENTORY_UPDATED, this::handleInventoryUpdated);

            // Subscribe to failure events
            eventBus.subscribe(EventType.INVENTORY_UPDATED, event -> {
                var status = (String) event.data().get("status");
                if ("FAILED".equals(status)) {
                    var orderId = (String) event.data().get("orderId");
                    cancelOrder(orderId);
                }
            });
        }

        public void createOrder(String orderId, String userId, double amount, String productId, int quantity) {
            // Start the saga by creating an order and publishing an event
            System.out.println("Creating order: " + orderId);

            // Publish order placed event to trigger payment
            Map<String, Object> orderData = Map.of(
                    "orderId", orderId,
                    "userId", userId,
                    "amount", amount,
                    "productId", productId,
                    "quantity", quantity
            );

            eventBus.publish(Event.of(EventType.ORDER_PLACED, orderData, "OrderService"));
        }

        private void handlePaymentReceived(Event event) {
            var orderId = (String) event.data().get("orderId");
            System.out.println("Payment received for order: " + orderId + ", updating order status");

            // Order status updated to "PAID" - next step will be triggered by this event
        }

        private void handleInventoryUpdated(Event event) {
            var orderId = (String) event.data().get("orderId");
            var status = (String) event.data().get("status");

            if ("RESERVED".equals(status)) {
                System.out.println("Inventory reserved for order: " + orderId + ", completing order");
                completeOrder(orderId);
            }
        }

        private void completeOrder(String orderId) {
            System.out.println("Order completed: " + orderId);

            // Publish order completed event
            eventBus.publish(Event.of(
                    EventType.ORDER_PROCESSED,
                    Map.of("orderId", orderId, "status", "COMPLETED"),
                    "OrderService"
            ));
        }

        private void cancelOrder(String orderId) {
            System.out.println("Cancelling order: " + orderId);

            // Publish order cancelled event
            eventBus.publish(Event.of(
                    EventType.ORDER_PROCESSED,
                    Map.of("orderId", orderId, "status", "CANCELLED"),
                    "OrderService"
            ));
        }
    }

    // Payment Service
    public static class PaymentService {
        private final EventBus eventBus;

        public PaymentService(EventBus eventBus) {
            this.eventBus = eventBus;

            // Subscribe to order placed events
            eventBus.subscribe(EventType.ORDER_PLACED, this::processPayment);

            // Subscribe to compensation events
            eventBus.subscribe(EventType.INVENTORY_UPDATED, event -> {
                var status = (String) event.data().get("status");
                if ("FAILED".equals(status)) {
                    var orderId = (String) event.data().get("orderId");
                    var amount = (double) event.data().get("amount");
                    refundPayment(orderId, amount);
                }
            });
        }

        private void processPayment(Event event) {
            var orderId = (String) event.data().get("orderId");
            var userId = (String) event.data().get("userId");
            var amount = (double) event.data().get("amount");

            System.out.println("Processing payment for order: " + orderId + " amount: " + amount);

            // Simulate payment processing
            boolean paymentSuccessful = ThreadLocalRandom.current().nextDouble() > 0.1; // 90% success rate

            if (paymentSuccessful) {
                System.out.println("Payment successful for order: " + orderId);

                // Publish payment received event to trigger next step (inventory)
                eventBus.publish(Event.of(
                        EventType.PAYMENT_RECEIVED,
                        Map.of("orderId", orderId, "userId", userId, "amount", amount),
                        "PaymentService"
                ));
            } else {
                System.out.println("Payment failed for order: " + orderId);

                // Publish payment failed event to trigger saga failure
                eventBus.publish(Event.of(
                        EventType.PAYMENT_RECEIVED,
                        Map.of("orderId", orderId, "status", "FAILED"),
                        "PaymentService"
                ));
            }
        }

        private void refundPayment(String orderId, double amount) {
            System.out.println("Refunding payment for order: " + orderId + " amount: " + amount);

            // Process refund logic
        }
    }

    // Inventory Service
    public static class InventoryService {
        private final EventBus eventBus;

        public InventoryService(EventBus eventBus) {
            this.eventBus = eventBus;

            // Subscribe to payment received events
            eventBus.subscribe(EventType.PAYMENT_RECEIVED, this::reserveInventory);
        }

        private void reserveInventory(Event event) {
            var orderId = (String) event.data().get("orderId");

            // Skip if this is a failure event
            if (event.data().containsKey("status") && "FAILED".equals(event.data().get("status"))) {
                return;
            }

            var productId = ""; // Would normally come from the event or be looked up
            var quantity = 0;   // Would normally come from the event or be looked up

            // If these were missing from the original event, we'd need to look them up
            if (event.data().containsKey("productId")) {
                productId = (String) event.data().get("productId");
            }

            if (event.data().containsKey("quantity")) {
                quantity = (int) event.data().get("quantity");
            }

            System.out.println("Reserving inventory for order: " + orderId);

            // Simulate inventory reservation
            boolean inventoryAvailable = ThreadLocalRandom.current().nextDouble() > 0.2; // 80% success rate

            if (inventoryAvailable) {
                System.out.println("Inventory reserved for order: " + orderId);

                // Publish inventory updated event
                eventBus.publish(Event.of(
                        EventType.INVENTORY_UPDATED,
                        Map.of("orderId", orderId, "status", "RESERVED"),
                        "InventoryService"
                ));
            } else {
                System.out.println("Inventory not available for order: " + orderId);

                // Publish inventory reservation failed event
                eventBus.publish(Event.of(
                        EventType.INVENTORY_UPDATED,
                        Map.of("orderId", orderId, "status", "FAILED"),
                        "InventoryService"
                ));
            }
        }

        private void releaseInventory(String orderId, String productId, int quantity) {
            System.out.println("Releasing inventory for order: " + orderId);

            // Release inventory logic
        }
    }
}

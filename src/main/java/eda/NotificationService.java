package eda;

import eda.event.Event;
import eda.event.EventBus;
import eda.event.EventConsumer;
import eda.event.EventType;

/**
 * The type Notification service.
 */
public class NotificationService implements EventConsumer {
    /**
     * Instantiates a new Notification service.
     *
     * @param eventBus the event bus
     */
    public NotificationService(EventBus eventBus) {
        // Subscribe to relevant events
        eventBus.subscribe(EventType.USER_CREATED, this::handleEvent);
        eventBus.subscribe(EventType.ORDER_PLACED, this::handleEvent);
    }

    @Override
    public void handleEvent(Event event) {
        switch (event.eventType()) {
            case USER_CREATED -> {
                var userData = event.data();
                var email = (String) userData.get("email");
                sendWelcomeEmail(email);
            }
            case ORDER_PLACED -> {
                var orderData = event.data();
                var orderId = (String) orderData.get("orderId");
                var userId = (String) orderData.get("userId");
                sendOrderConfirmation(orderId, userId);
            }
            default -> System.out.println("Event type not handled: " + event.eventType());
        }
    }

    private void sendWelcomeEmail(String email) {
        System.out.println("Sending welcome email to: " + email);
        // Email sending logic
    }

    private void sendOrderConfirmation(String orderId, String userId) {
        System.out.println("Sending order confirmation for order: " + orderId + " to user: " + userId);
        // Order confirmation logic
    }
}
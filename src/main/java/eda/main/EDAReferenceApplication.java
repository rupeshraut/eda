package eda.main;

import eda.NotificationService;
import eda.UserService;
import eda.event.EventBus;
import eda.resiliency.ResilientSagaExample;
import eda.saga.ChoreographySaga;
import eda.saga.OrchestrationSaga;

import java.util.logging.Logger;

/**
 * The type Eda reference application.
 */
public class EDAReferenceApplication {

    public static final Logger LOGGER = Logger.getLogger(EDAReferenceApplication.class.getName());

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        // Create event bus
        var eventBus = new EventBus();

        // Create event producers
        var userService = new UserService(eventBus);

        // Create event consumers (automatically subscribe to relevant events)
        var notificationService = new NotificationService(eventBus);

        // Example: Create a user which will trigger events
        userService.createUser("user123", "John Doe", "john.doe@example.com");

        // Example: Demonstrate the saga pattern
        LOGGER.info("\n=== CHOREOGRAPHY SAGA EXAMPLE ===");
        ChoreographySaga.demonstrateChoreographySaga(eventBus);

        LOGGER.info("\n=== ORCHESTRATION SAGA EXAMPLE ===");
        OrchestrationSaga.demonstrateOrchestrationSaga();

        LOGGER.info("\n=== RESILIENT SAGA WITH MONITORING EXAMPLE ===");
        ResilientSagaExample.demonstrateResilientSaga();

        // Allow time for async processing
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Proper shutdown
        eventBus.shutdown();
    }
}
package eda.service;

import eda.event.Event;
import eda.event.EventBus;
import eda.event.EventProducer;
import eda.event.EventType;

import java.util.Map;

/**
 * The type User service.
 */
public class UserService implements EventProducer {
    private final EventBus eventBus;

    /**
     * Instantiates a new User service.
     *
     * @param eventBus the event bus
     */
    public UserService(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void sendEvent(Event event) {
        eventBus.publish(event);
    }

    /**
     * Create user.
     *
     * @param userId the user id
     * @param name   the name
     * @param email  the email
     */
    public void createUser(String userId, String name, String email) {
        // Business logic for user creation
        // ...

        // Publish event after user creation
        final Map<String, Object> userData = Map.of(
                "userId", userId,
                "name", name,
                "email", email
        );

        sendEvent(Event.of(EventType.USER_CREATED, userData, "UserService"));
    }
}

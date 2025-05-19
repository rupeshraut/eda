package eda.cqrs;

import eda.event.Event;
import eda.event.EventBus;
import eda.event.EventType;

import java.util.Map;

/**
 * The type Command handler.
 */
public class CommandHandler {
    private final EventBus eventBus;

    /**
     * Instantiates a new Command handler.
     *
     * @param eventBus the event bus
     */
    public CommandHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Handle command.
     *
     * @param command the command
     */
    public void handleCommand(Command command) {
        // Process command and generate events
        switch (command.name()) {
            case "CreateUser" -> {
                var params = command.parameters();
                var userId = (String) params.get("userId");
                var name = (String) params.get("name");
                var email = (String) params.get("email");

                // Validate command data
                if (userId == null || name == null || email == null) {
                    throw new IllegalArgumentException("Missing required parameters");
                }

                // Generate event
                var event = Event.of(
                        EventType.USER_CREATED,
                        Map.of("userId", userId, "name", name, "email", email),
                        "CommandHandler"
                );

                eventBus.publish(event);
            }
            default -> throw new UnsupportedOperationException("Unknown command: " + command.name());
        }
    }
}
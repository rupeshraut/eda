package eda.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The type Event bus.
 */
public class EventBus {
    public static final Logger LOGGER = Logger.getLogger(EventBus.class.getName());
    private final Map<EventType, List<Consumer<Event>>> subscribers = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    /**
     * Instantiates a new Event bus.
     */
    public EventBus() {
        // Use virtual threads if on Java 21, otherwise use standard thread pool
        // For Java 21: this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Subscribe.
     *
     * @param eventType the event type
     * @param handler   the handler
     */
// Subscribe to specific event types
    public void subscribe(EventType eventType, Consumer<Event> handler) {
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    /**
     * Publish.
     *
     * @param event the event
     */
// Publish events to the bus
    public void publish(Event event) {
        // Log the event (in production, might use a proper logging framework)
        LOGGER.log(Level.INFO,"Event published: {}", event);

        // Get subscribers for this event type
        var handlers = subscribers.getOrDefault(event.eventType(), List.of());

        // Dispatch event to all subscribers asynchronously
        for (var handler : handlers) {
            executorService.submit(() -> {
                try {
                    handler.accept(event);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE,"Error handling event: {}",  e.getMessage());
                }
            });
        }
    }

    /**
     * Shutdown.
     */
// Clean shutdown
    public void shutdown() {
        executorService.shutdown();
    }
}
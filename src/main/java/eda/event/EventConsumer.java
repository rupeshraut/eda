package eda.event;

/**
 * The interface Event consumer.
 */
public interface EventConsumer {
    /**
     * Handle event.
     *
     * @param event the event
     */
    void handleEvent(Event event);
}
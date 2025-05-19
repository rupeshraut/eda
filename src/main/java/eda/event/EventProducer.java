package eda.event;

/**
 * The interface Event producer.
 */
public interface EventProducer {
    /**
     * Send event.
     *
     * @param event the event
     */
    void sendEvent(Event event);
}
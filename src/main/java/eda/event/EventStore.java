package eda.event;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Event store.
 */
// Event sourcing - storing all events as the source of truth
public class EventStore {
    private final List<Event> events = new ArrayList<>();

    /**
     * Store.
     *
     * @param event the event
     */
    public void store(Event event) {
        events.add(event);
    }

    /**
     * Gets events by type.
     *
     * @param eventType the event type
     * @return the events by type
     */
    public List<Event> getEventsByType(EventType eventType) {
        return events.stream()
                .filter(event -> event.eventType() == eventType)
                .toList();
    }
}

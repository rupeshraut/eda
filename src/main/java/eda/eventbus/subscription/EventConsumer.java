package eda.eventbus.subscription;

import eda.eventbus.core.GenericEvent;
import java.util.concurrent.CompletableFuture;

/**
 * Generic event consumer interface
 * @param <T> Event type
 * @param <D> Event data type
 */
@FunctionalInterface
public interface EventConsumer<T, D> {
    /**
     * Handle an event
     * @param event The event to handle
     * @return CompletableFuture that completes when event processing is done
     */
    CompletableFuture<Void> handle(GenericEvent<T, D> event);
}
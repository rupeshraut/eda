package eda.eventbus.subscription;

import eda.eventbus.core.GenericEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for managing event subscriptions
 */
public interface SubscriptionManager {
    
    /**
     * Subscribe to events of a specific type
     */
    <T, D> EventSubscription<T, D> subscribe(T eventType, EventConsumer<T, D> consumer);
    
    /**
     * Subscribe to events with options
     */
    <T, D> EventSubscription<T, D> subscribe(T eventType, EventConsumer<T, D> consumer, SubscriptionOptions options);
    
    /**
     * Unsubscribe by subscription ID
     */
    CompletableFuture<Boolean> unsubscribe(UUID subscriptionId);
    
    /**
     * Unsubscribe all subscriptions for a subscriber ID
     */
    CompletableFuture<Integer> unsubscribeAll(String subscriberId);
    
    /**
     * Get all active subscriptions for an event type
     */
    <T> List<EventSubscription<T, ?>> getSubscriptions(T eventType);
    
    /**
     * Get subscription by ID
     */
    Optional<EventSubscription<?, ?>> getSubscription(UUID subscriptionId);
    
    /**
     * Get all subscriptions for a subscriber ID
     */
    List<EventSubscription<?, ?>> getSubscriptionsBySubscriber(String subscriberId);
    
    /**
     * Get all active subscriptions
     */
    List<EventSubscription<?, ?>> getAllSubscriptions();
    
    /**
     * Publish event to all matching subscriptions
     */
    <T, D> CompletableFuture<Void> publishToSubscriptions(GenericEvent<T, D> event);
    
    /**
     * Get subscription statistics
     */
    List<EventSubscription.SubscriptionStats> getSubscriptionStats();
    
    /**
     * Cleanup inactive subscriptions
     */
    CompletableFuture<Integer> cleanupInactiveSubscriptions();
}
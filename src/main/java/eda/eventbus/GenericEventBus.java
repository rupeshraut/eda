package eda.eventbus;

import eda.eventbus.core.GenericEvent;
import eda.eventbus.deadletter.DeadLetterEvent;
import eda.eventbus.deadletter.DeadLetterQueue;
import eda.eventbus.deadletter.DeadLetterReason;
import eda.eventbus.deadletter.InMemoryDeadLetterQueue;
import eda.eventbus.retry.RetryableEventProcessor;
import eda.eventbus.subscription.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic, reusable event bus implementation with high-priority features
 * @param <T> Event type enum/class
 */
public class GenericEventBus<T> implements SubscriptionManager {
    private static final Logger LOGGER = Logger.getLogger(GenericEventBus.class.getName());
    
    private final Map<T, List<EventSubscription<T, ?>>> subscriptions = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    private final RetryableEventProcessor retryProcessor;
    private final DeadLetterQueue deadLetterQueue;
    private final EventBusConfig config;
    
    public GenericEventBus() {
        this(EventBusConfig.defaultConfig());
    }
    
    public GenericEventBus(EventBusConfig config) {
        this.config = config;
        this.executorService = createExecutorService(config);
        this.scheduledExecutor = Executors.newScheduledThreadPool(config.getScheduledThreadPoolSize());
        this.retryProcessor = new RetryableEventProcessor(scheduledExecutor, config.getDefaultRetryConfig());
        this.deadLetterQueue = new InMemoryDeadLetterQueue();
        
        // Start cleanup task
        if (config.getCleanupInterval() != null) {
            startCleanupTask();
        }
    }
    
    /**
     * Publish an event to all matching subscribers
     */
    public <D> CompletableFuture<Void> publish(GenericEvent<T, D> event) {
        LOGGER.log(Level.FINE, "Publishing event: {0}", event);
        
        List<EventSubscription<T, ?>> eventSubscriptions = subscriptions.getOrDefault(event.getEventType(), List.of());
        
        if (eventSubscriptions.isEmpty()) {
            LOGGER.log(Level.FINE, "No subscribers for event type: {0}", event.getEventType());
            return CompletableFuture.completedFuture(null);
        }
        
        // Process subscriptions by priority
        List<CompletableFuture<Void>> futures = eventSubscriptions.stream()
            .filter(EventSubscription::isActive)
            .sorted((s1, s2) -> s2.getOptions().getPriority().compareTo(s1.getOptions().getPriority()))
            .map(subscription -> processEventForSubscription(event, subscription))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    @SuppressWarnings("unchecked")
    private <D> CompletableFuture<Void> processEventForSubscription(
            GenericEvent<T, D> event, 
            EventSubscription<T, ?> subscription) {
        
        EventSubscription<T, D> typedSubscription = (EventSubscription<T, D>) subscription;
        SubscriptionOptions options = subscription.getOptions();
        
        // Apply timeout
        CompletableFuture<Void> processTask = options.isOrdered() 
            ? processOrderedEvent(event, typedSubscription)
            : processUnorderedEvent(event, typedSubscription);
        
        // Apply timeout
        CompletableFuture<Void> timeoutTask = applyTimeout(processTask, options.getTimeout());
        
        // Handle failures and dead lettering
        return timeoutTask.exceptionally(throwable -> {
            if (options.isDeadLetterEnabled()) {
                sendToDeadLetter(event, subscription, throwable, DeadLetterReason.RETRY_EXHAUSTED);
            }
            throw new RuntimeException("Event processing failed for subscription: " + subscription.getSubscriptionId(), throwable);
        });
    }
    
    private <D> CompletableFuture<Void> processUnorderedEvent(
            GenericEvent<T, D> event, 
            EventSubscription<T, D> subscription) {
        
        return retryProcessor.processWithRetry(
            event,
            evt -> subscription.processEvent(evt),
            subscription.getOptions().getRetryConfig()
        );
    }
    
    private <D> CompletableFuture<Void> processOrderedEvent(
            GenericEvent<T, D> event, 
            EventSubscription<T, D> subscription) {
        
        // For ordered processing, we need to ensure sequential execution
        return CompletableFuture.runAsync(() -> {
            try {
                subscription.processEvent(event).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    private <D> CompletableFuture<Void> applyTimeout(CompletableFuture<Void> task, Duration timeout) {
        CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();
        
        // Schedule timeout
        ScheduledFuture<?> timeoutTask = scheduledExecutor.schedule(() -> {
            if (!task.isDone()) {
                timeoutFuture.completeExceptionally(new TimeoutException("Event processing timeout"));
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        
        // Complete when original task completes
        task.whenComplete((result, throwable) -> {
            timeoutTask.cancel(false);
            if (throwable != null) {
                timeoutFuture.completeExceptionally(throwable);
            } else {
                timeoutFuture.complete(result);
            }
        });
        
        return timeoutFuture;
    }
    
    private <D> void sendToDeadLetter(
            GenericEvent<T, D> event, 
            EventSubscription<T, ?> subscription, 
            Throwable cause, 
            DeadLetterReason reason) {
        
        DeadLetterEvent<T, D> deadLetterEvent = new DeadLetterEvent<>(
            event,
            cause.getMessage(),
            cause,
            subscription.getOptions().getSubscriberId(),
            subscription.getOptions().getRetryConfig().getMaxAttempts(),
            reason
        );
        
        deadLetterQueue.sendToDeadLetter(deadLetterEvent)
            .exceptionally(dlqError -> {
                LOGGER.log(Level.SEVERE, "Failed to send event to dead letter queue", dlqError);
                return null;
            });
    }
    
    @Override
    public <ET, D> EventSubscription<ET, D> subscribe(ET eventType, EventConsumer<ET, D> consumer) {
        return subscribe(eventType, consumer, SubscriptionOptions.defaultOptions());
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <ET, D> EventSubscription<ET, D> subscribe(ET eventType, EventConsumer<ET, D> consumer, SubscriptionOptions options) {
        T typedEventType = (T) eventType;
        EventConsumer<T, D> typedConsumer = (EventConsumer<T, D>) consumer;
        
        EventSubscription<T, D> subscription = new EventSubscription<>(typedEventType, typedConsumer, options);
        
        subscriptions.computeIfAbsent(typedEventType, k -> new CopyOnWriteArrayList<>())
                    .add(subscription);
        
        LOGGER.log(Level.INFO, "New subscription created: {0}", subscription);
        return (EventSubscription<ET, D>) subscription;
    }
    
    @Override
    public CompletableFuture<Boolean> unsubscribe(UUID subscriptionId) {
        return CompletableFuture.supplyAsync(() -> {
            for (List<EventSubscription<T, ?>> subs : subscriptions.values()) {
                Optional<EventSubscription<T, ?>> found = subs.stream()
                    .filter(sub -> sub.getSubscriptionId().equals(subscriptionId))
                    .findFirst();
                
                if (found.isPresent()) {
                    found.get().unsubscribe();
                    subs.remove(found.get());
                    LOGGER.log(Level.INFO, "Unsubscribed: {0}", subscriptionId);
                    return true;
                }
            }
            return false;
        });
    }
    
    @Override
    public CompletableFuture<Integer> unsubscribeAll(String subscriberId) {
        return CompletableFuture.supplyAsync(() -> {
            int count = 0;
            for (List<EventSubscription<T, ?>> subs : subscriptions.values()) {
                Iterator<EventSubscription<T, ?>> iterator = subs.iterator();
                while (iterator.hasNext()) {
                    EventSubscription<T, ?> sub = iterator.next();
                    if (sub.getOptions().getSubscriberId().equals(subscriberId)) {
                        sub.unsubscribe();
                        iterator.remove();
                        count++;
                    }
                }
            }
            LOGGER.log(Level.INFO, "Unsubscribed {0} subscriptions for subscriber: {1}", 
                new Object[]{count, subscriberId});
            return count;
        });
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <ET> List<EventSubscription<ET, ?>> getSubscriptions(ET eventType) {
        return (List<EventSubscription<ET, ?>>) (List<?>) subscriptions.getOrDefault(eventType, List.of());
    }
    
    @Override
    public Optional<EventSubscription<?, ?>> getSubscription(UUID subscriptionId) {
        for (List<EventSubscription<T, ?>> subs : subscriptions.values()) {
            for (EventSubscription<T, ?> sub : subs) {
                if (sub.getSubscriptionId().equals(subscriptionId)) {
                    return Optional.of(sub);
                }
            }
        }
        return Optional.empty();
    }
    
    @Override
    public List<EventSubscription<?, ?>> getSubscriptionsBySubscriber(String subscriberId) {
        List<EventSubscription<?, ?>> result = new ArrayList<>();
        for (List<EventSubscription<T, ?>> subs : subscriptions.values()) {
            for (EventSubscription<T, ?> sub : subs) {
                if (sub.getOptions().getSubscriberId().equals(subscriberId)) {
                    result.add(sub);
                }
            }
        }
        return result;
    }
    
    @Override
    public List<EventSubscription<?, ?>> getAllSubscriptions() {
        List<EventSubscription<?, ?>> result = new ArrayList<>();
        for (List<EventSubscription<T, ?>> subs : subscriptions.values()) {
            result.addAll(subs);
        }
        return result;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <ET, D> CompletableFuture<Void> publishToSubscriptions(GenericEvent<ET, D> event) {
        return publish((GenericEvent<T, D>) event);
    }
    
    @Override
    public List<EventSubscription.SubscriptionStats> getSubscriptionStats() {
        return getAllSubscriptions().stream()
            .map(EventSubscription::getStats)
            .toList();
    }
    
    @Override
    public CompletableFuture<Integer> cleanupInactiveSubscriptions() {
        return CompletableFuture.supplyAsync(() -> {
            int count = 0;
            for (List<EventSubscription<T, ?>> subs : subscriptions.values()) {
                Iterator<EventSubscription<T, ?>> iterator = subs.iterator();
                while (iterator.hasNext()) {
                    if (!iterator.next().isActive()) {
                        iterator.remove();
                        count++;
                    }
                }
            }
            LOGGER.log(Level.INFO, "Cleaned up {0} inactive subscriptions", count);
            return count;
        });
    }
    
    /**
     * Get the dead letter queue for manual inspection
     */
    public DeadLetterQueue getDeadLetterQueue() {
        return deadLetterQueue;
    }
    
    /**
     * Graceful shutdown
     */
    public void shutdown() {
        LOGGER.info("Shutting down event bus...");
        
        executorService.shutdown();
        scheduledExecutor.shutdown();
        
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
            scheduledExecutor.shutdownNow();
        }
        
        LOGGER.info("Event bus shutdown complete");
    }
    
    private ExecutorService createExecutorService(EventBusConfig config) {
        if (config.isUseVirtualThreads() && isVirtualThreadsAvailable()) {
            try {
                // Use virtual threads if available (Java 21+)
                return (ExecutorService) Executors.class
                    .getMethod("newVirtualThreadPerTaskExecutor")
                    .invoke(null);
            } catch (Exception e) {
                LOGGER.warning("Failed to create virtual thread executor, falling back to cached thread pool");
            }
        }
        return Executors.newCachedThreadPool();
    }
    
    private boolean isVirtualThreadsAvailable() {
        try {
            Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    private void startCleanupTask() {
        scheduledExecutor.scheduleAtFixedRate(
            this::cleanupInactiveSubscriptions,
            config.getCleanupInterval().toMinutes(),
            config.getCleanupInterval().toMinutes(),
            TimeUnit.MINUTES
        );
    }
}
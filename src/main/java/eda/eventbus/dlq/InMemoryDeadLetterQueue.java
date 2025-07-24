package eda.eventbus.dlq;

import eda.eventbus.core.GenericEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * In-memory implementation of Dead Letter Queue
 */
public class InMemoryDeadLetterQueue<T> implements DeadLetterQueue<T> {
    private static final Logger LOGGER = Logger.getLogger(InMemoryDeadLetterQueue.class.getName());
    
    private final ConcurrentHashMap<String, DeadLetterEvent<T>> events = new ConcurrentHashMap<>();
    private final List<Consumer<DLQEvent>> eventSubscribers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(2);
    private final ScheduledExecutorService maintenanceExecutor = Executors.newSingleThreadScheduledExecutor();
    
    private volatile RetryPolicy autoRetryPolicy;
    private volatile boolean autoRetryEnabled = false;
    private final Set<String> retryingEvents = ConcurrentHashMap.newKeySet();
    
    // Statistics
    private final AtomicLong totalEventsStored = new AtomicLong();
    private final AtomicLong totalEventsRetried = new AtomicLong();
    private final AtomicLong totalEventsResolved = new AtomicLong();
    private final AtomicLong totalEventsPurged = new AtomicLong();
    
    // Configuration
    private final int maxSize;
    private final Duration defaultRetention;
    
    public InMemoryDeadLetterQueue() {
        this(10000, Duration.ofDays(7));
    }
    
    public InMemoryDeadLetterQueue(int maxSize, Duration defaultRetention) {
        this.maxSize = maxSize;
        this.defaultRetention = defaultRetention;
        
        // Start maintenance task
        maintenanceExecutor.scheduleAtFixedRate(this::performMaintenance, 1, 1, TimeUnit.HOURS);
        
        LOGGER.info("InMemoryDeadLetterQueue initialized with maxSize=" + maxSize + ", retention=" + defaultRetention);
    }
    
    @Override
    public CompletableFuture<Void> store(DeadLetterEvent<T> deadLetterEvent) {
        return CompletableFuture.runAsync(() -> {
            // Check size limit
            if (events.size() >= maxSize) {
                LOGGER.warning("DLQ size limit reached (" + maxSize + "), purging old events");
                purgeOldestEvents(maxSize / 10); // Purge 10% oldest events
            }
            
            // Store the event
            events.put(deadLetterEvent.getId(), deadLetterEvent);
            totalEventsStored.incrementAndGet();
            
            // Notify subscribers
            notifySubscribers(new DLQEvent(DLQEventType.EVENT_STORED, deadLetterEvent.getId(), 
                "Event stored in DLQ: " + deadLetterEvent.getEventType()));
            
            // Trigger auto-retry if enabled and eligible
            if (autoRetryEnabled && deadLetterEvent.isEligibleForRetry()) {
                scheduleAutoRetry(deadLetterEvent);
            }
            
            LOGGER.fine("Stored event in DLQ: " + deadLetterEvent.getId());
        });
    }
    
    @Override
    public CompletableFuture<List<DeadLetterEvent<T>>> retrieve(DLQFilter filter) {
        return CompletableFuture.supplyAsync(() -> {
            return events.values().stream()
                .filter(event -> matchesFilter(event, filter))
                .sorted((e1, e2) -> e2.getLastFailureTime().compareTo(e1.getLastFailureTime()))
                .limit(filter.getLimit())
                .collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<List<DeadLetterEvent<T>>> retrieveAll() {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(events.values()));
    }
    
    @Override
    public CompletableFuture<RetryResult> retry(String eventId) {
        return CompletableFuture.supplyAsync(() -> {
            DeadLetterEvent<T> event = events.get(eventId);
            if (event == null) {
                return RetryResult.builder()
                    .eventId(eventId)
                    .success(false)
                    .message("Event not found in DLQ")
                    .newStatus(DLQStatus.FAILED)
                    .build();
            }
            
            return performRetry(event);
        });
    }
    
    @Override
    public CompletableFuture<List<RetryResult>> retryBatch(List<String> eventIds) {
        return CompletableFuture.supplyAsync(() -> {
            return eventIds.stream()
                .map(this::retry)
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<Void> remove(String eventId) {
        return CompletableFuture.runAsync(() -> {
            DeadLetterEvent<T> removed = events.remove(eventId);
            if (removed != null) {
                totalEventsResolved.incrementAndGet();
                notifySubscribers(new DLQEvent(DLQEventType.EVENT_REMOVED, eventId, "Event removed from DLQ"));
                LOGGER.fine("Removed event from DLQ: " + eventId);
            }
        });
    }
    
    @Override
    public CompletableFuture<Integer> purge(Duration retentionPeriod) {
        return CompletableFuture.supplyAsync(() -> {
            Instant cutoff = Instant.now().minus(retentionPeriod);
            List<String> toRemove = events.values().stream()
                .filter(event -> event.getFirstFailureTime().isBefore(cutoff))
                .map(DeadLetterEvent::getId)
                .collect(Collectors.toList());
            
            int removedCount = 0;
            for (String eventId : toRemove) {
                if (events.remove(eventId) != null) {
                    removedCount++;
                }
            }
            
            totalEventsPurged.addAndGet(removedCount);
            
            if (removedCount > 0) {
                notifySubscribers(new DLQEvent(DLQEventType.PURGE_COMPLETED, null, 
                    "Purged " + removedCount + " events older than " + retentionPeriod));
                LOGGER.info("Purged " + removedCount + " events from DLQ");
            }
            
            return removedCount;
        });
    }
    
    @Override
    public CompletableFuture<DLQStatistics> getStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            Map<DLQStatus, Long> statusCounts = events.values().stream()
                .collect(Collectors.groupingBy(DeadLetterEvent::getStatus, Collectors.counting()));
            
            Map<String, Long> eventTypeCounts = events.values().stream()
                .collect(Collectors.groupingBy(DeadLetterEvent::getEventType, Collectors.counting()));
            
            Map<String, Long> errorTypeCounts = events.values().stream()
                .flatMap(event -> event.getFailureHistory().stream())
                .collect(Collectors.groupingBy(FailureReason::getErrorType, Collectors.counting()));
            
            double avgRetryCount = events.values().stream()
                .mapToInt(DeadLetterEvent::getRetryCount)
                .average()
                .orElse(0.0);
            
            long oldestEventAge = events.values().stream()
                .mapToLong(DeadLetterEvent::getTimeInDLQMillis)
                .max()
                .orElse(0);
            
            DLQStatistics.DLQHealth health = calculateHealth(statusCounts, oldestEventAge);
            
            return DLQStatistics.builder()
                .totalEvents(events.size())
                .failedEvents(statusCounts.getOrDefault(DLQStatus.FAILED, 0L))
                .retryingEvents(statusCounts.getOrDefault(DLQStatus.RETRYING, 0L))
                .exhaustedEvents(statusCounts.getOrDefault(DLQStatus.EXHAUSTED, 0L))
                .resolvedEvents(totalEventsResolved.get())
                .quarantinedEvents(statusCounts.getOrDefault(DLQStatus.QUARANTINED, 0L))
                .eventsByType(eventTypeCounts)
                .errorsByType(errorTypeCounts)
                .averageRetryCount(avgRetryCount)
                .oldestEventAgeMillis(oldestEventAge)
                .health(health)
                .build();
        });
    }
    
    @Override
    public void subscribe(Consumer<DLQEvent> eventConsumer) {
        eventSubscribers.add(eventConsumer);
        LOGGER.fine("Added DLQ event subscriber: " + eventConsumer.getClass().getSimpleName());
    }
    
    @Override
    public void enableAutoRetry(RetryPolicy retryPolicy) {
        this.autoRetryPolicy = retryPolicy;
        this.autoRetryEnabled = true;
        LOGGER.info("Auto-retry enabled with policy: " + retryPolicy);
    }
    
    @Override
    public void disableAutoRetry() {
        this.autoRetryEnabled = false;
        LOGGER.info("Auto-retry disabled");
    }
    
    @Override
    public void shutdown() {
        LOGGER.info("Shutting down InMemoryDeadLetterQueue...");
        
        retryExecutor.shutdown();
        maintenanceExecutor.shutdown();
        
        try {
            if (!retryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                retryExecutor.shutdownNow();
            }
            if (!maintenanceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                maintenanceExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            retryExecutor.shutdownNow();
            maintenanceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOGGER.info("InMemoryDeadLetterQueue shutdown complete");
    }
    
    private boolean matchesFilter(DeadLetterEvent<T> event, DLQFilter filter) {
        if (filter.getFromTime() != null && event.getFirstFailureTime().isBefore(filter.getFromTime())) {
            return false;
        }
        if (filter.getToTime() != null && event.getLastFailureTime().isAfter(filter.getToTime())) {
            return false;
        }
        if (filter.getEventType() != null && !filter.getEventType().equals(event.getEventType())) {
            return false;
        }
        if (filter.getStatus() != null && filter.getStatus() != event.getStatus()) {
            return false;
        }
        if (filter.getMaxRetries() != null && event.getRetryCount() > filter.getMaxRetries()) {
            return false;
        }
        if (filter.getErrorType() != null) {
            boolean hasErrorType = event.getFailureHistory().stream()
                .anyMatch(reason -> filter.getErrorType().equals(reason.getErrorType()));
            if (!hasErrorType) {
                return false;
            }
        }
        return true;
    }
    
    private RetryResult performRetry(DeadLetterEvent<T> event) {
        if (!event.isEligibleForRetry()) {
            return RetryResult.builder()
                .eventId(event.getId())
                .success(false)
                .message("Event is not eligible for retry")
                .attemptNumber(event.getRetryCount())
                .newStatus(event.getStatus())
                .build();
        }
        
        // Mark as retrying
        retryingEvents.add(event.getId());
        DeadLetterEvent<T> retryingEvent = event.withStatus(DLQStatus.RETRYING);
        events.put(event.getId(), retryingEvent);
        
        try {
            // Simulate event reprocessing
            // In a real implementation, this would republish the event to the original topic/queue
            boolean success = simulateEventReprocessing(event.getOriginalEvent());
            
            totalEventsRetried.incrementAndGet();
            
            if (success) {
                // Remove from DLQ on success
                events.remove(event.getId());
                retryingEvents.remove(event.getId());
                totalEventsResolved.incrementAndGet();
                
                notifySubscribers(new DLQEvent(DLQEventType.EVENT_RETRIED, event.getId(), 
                    "Event retry successful"));
                
                return RetryResult.success(event.getId(), event.getRetryCount() + 1);
            } else {
                // Update retry count and status
                FailureReason newFailure = FailureReason.builder()
                    .timestamp(Instant.now())
                    .errorType("RetryFailure")
                    .errorMessage("Simulated retry failure")
                    .attemptNumber(event.getRetryCount() + 1)
                    .build();
                
                DeadLetterEvent<T> updatedEvent = event.withRetryAttempt(newFailure);
                events.put(event.getId(), updatedEvent);
                retryingEvents.remove(event.getId());
                
                notifySubscribers(new DLQEvent(DLQEventType.RETRY_FAILED, event.getId(), 
                    "Event retry failed"));
                
                return RetryResult.failure(event.getId(), event.getRetryCount() + 1, 
                    new RuntimeException("Simulated retry failure"));
            }
            
        } catch (Exception e) {
            retryingEvents.remove(event.getId());
            
            FailureReason newFailure = FailureReason.fromException(e, event.getRetryCount() + 1);
            DeadLetterEvent<T> updatedEvent = event.withRetryAttempt(newFailure);
            events.put(event.getId(), updatedEvent);
            
            notifySubscribers(new DLQEvent(DLQEventType.RETRY_FAILED, event.getId(), 
                "Event retry failed with exception", e));
            
            return RetryResult.failure(event.getId(), event.getRetryCount() + 1, e);
        }
    }
    
    private void scheduleAutoRetry(DeadLetterEvent<T> event) {
        if (autoRetryPolicy == null) return;
        
        Duration delay = autoRetryPolicy.calculateDelay(event.getRetryCount() + 1);
        
        retryExecutor.schedule(() -> {
            if (events.containsKey(event.getId()) && !retryingEvents.contains(event.getId())) {
                DeadLetterEvent<T> currentEvent = events.get(event.getId());
                if (currentEvent != null && currentEvent.isEligibleForRetry()) {
                    notifySubscribers(new DLQEvent(DLQEventType.AUTO_RETRY_TRIGGERED, event.getId(), 
                        "Auto-retry triggered"));
                    performRetry(currentEvent);
                }
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
        
        LOGGER.fine("Scheduled auto-retry for event " + event.getId() + " in " + delay);
    }
    
    private void performMaintenance() {
        try {
            // Purge old events
            int purged = purge(defaultRetention).join();
            if (purged > 0) {
                LOGGER.info("Maintenance: purged " + purged + " old events");
            }
            
            // Clean up completed retry tracking
            retryingEvents.removeIf(eventId -> !events.containsKey(eventId));
            
        } catch (Exception e) {
            LOGGER.warning("Error during DLQ maintenance: " + e.getMessage());
        }
    }
    
    private void purgeOldestEvents(int count) {
        List<String> oldest = events.values().stream()
            .sorted(Comparator.comparing(DeadLetterEvent::getFirstFailureTime))
            .limit(count)
            .map(DeadLetterEvent::getId)
            .collect(Collectors.toList());
        
        for (String eventId : oldest) {
            events.remove(eventId);
        }
        
        totalEventsPurged.addAndGet(oldest.size());
        LOGGER.info("Purged " + oldest.size() + " oldest events due to size limit");
    }
    
    private DLQStatistics.DLQHealth calculateHealth(Map<DLQStatus, Long> statusCounts, long oldestEventAge) {
        long totalEvents = events.size();
        long problemEvents = statusCounts.getOrDefault(DLQStatus.EXHAUSTED, 0L) + 
                           statusCounts.getOrDefault(DLQStatus.QUARANTINED, 0L);
        
        // Health based on problem event ratio and age
        if (totalEvents == 0) {
            return DLQStatistics.DLQHealth.HEALTHY;
        }
        
        double problemRatio = (double) problemEvents / totalEvents;
        long oldestAgeHours = oldestEventAge / (1000 * 60 * 60);
        
        if (problemRatio > 0.5 || oldestAgeHours > 168) { // > 50% problems or > 7 days old
            return DLQStatistics.DLQHealth.CRITICAL;
        } else if (problemRatio > 0.2 || oldestAgeHours > 72) { // > 20% problems or > 3 days old
            return DLQStatistics.DLQHealth.WARNING;
        } else {
            return DLQStatistics.DLQHealth.HEALTHY;
        }
    }
    
    private boolean simulateEventReprocessing(GenericEvent<T, ?> event) {
        // Simulate 70% success rate for retries
        return Math.random() > 0.3;
    }
    
    private void notifySubscribers(DLQEvent dlqEvent) {
        for (Consumer<DLQEvent> subscriber : eventSubscribers) {
            try {
                subscriber.accept(dlqEvent);
            } catch (Exception e) {
                LOGGER.warning("Error notifying DLQ subscriber: " + e.getMessage());
            }
        }
    }
}
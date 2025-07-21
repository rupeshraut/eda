package eda.eventbus.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of EventBusMetrics
 */
public class InMemoryEventBusMetrics implements EventBusMetrics {
    
    // Global counters
    private final LongAdder totalEventsPublished = new LongAdder();
    private final LongAdder totalEventsProcessed = new LongAdder();
    private final LongAdder totalEventsFailed = new LongAdder();
    private final LongAdder totalDeadLettered = new LongAdder();
    private final LongAdder totalRetries = new LongAdder();
    private final LongAdder totalTimeouts = new LongAdder();
    private final LongAdder totalSubscriptions = new LongAdder();
    private final LongAdder totalProcessingTimeMs = new LongAdder();
    
    // Per event type metrics
    private final Map<String, EventTypeMetricsCollector> eventTypeMetrics = new ConcurrentHashMap<>();
    
    // Per subscriber metrics  
    private final Map<String, SubscriberMetricsCollector> subscriberMetrics = new ConcurrentHashMap<>();
    
    @Override
    public void recordEventPublished(String eventType, String source) {
        totalEventsPublished.increment();
        getEventTypeCollector(eventType).published.increment();
    }
    
    @Override
    public void recordEventProcessed(String eventType, String subscriberId, Duration processingTime) {
        totalEventsProcessed.increment();
        long processingTimeMs = processingTime.toMillis();
        totalProcessingTimeMs.add(processingTimeMs);
        
        getEventTypeCollector(eventType).recordProcessed(processingTimeMs);
        getSubscriberCollector(subscriberId).recordProcessed(processingTimeMs);
    }
    
    @Override
    public void recordEventFailed(String eventType, String subscriberId, String errorType, Duration processingTime) {
        totalEventsFailed.increment();
        long processingTimeMs = processingTime.toMillis();
        totalProcessingTimeMs.add(processingTimeMs);
        
        getEventTypeCollector(eventType).recordFailed(processingTimeMs);
        getSubscriberCollector(subscriberId).recordFailed(processingTimeMs);
    }
    
    @Override
    public void recordDeadLetter(String eventType, String subscriberId, String reason) {
        totalDeadLettered.increment();
        getEventTypeCollector(eventType).deadLettered.increment();
        getSubscriberCollector(subscriberId).deadLettered.increment();
    }
    
    @Override
    public void recordSubscriptionCreated(String eventType, String subscriberId) {
        totalSubscriptions.increment();
    }
    
    @Override
    public void recordSubscriptionRemoved(String eventType, String subscriberId) {
        totalSubscriptions.decrement();
    }
    
    @Override
    public void recordRetryAttempt(String eventType, String subscriberId, int attemptNumber) {
        totalRetries.increment();
        getEventTypeCollector(eventType).retries.increment();
        getSubscriberCollector(subscriberId).retries.increment();
    }
    
    @Override
    public void recordTimeout(String eventType, String subscriberId) {
        totalTimeouts.increment();
        getSubscriberCollector(subscriberId).timeouts.increment();
    }
    
    @Override
    public CompletableFuture<EventBusMetricsSnapshot> getMetricsSnapshot() {
        return CompletableFuture.supplyAsync(() -> {
            long processed = totalEventsProcessed.sum();
            long failed = totalEventsFailed.sum();
            double successRate = (processed + failed) > 0 ? (double) processed / (processed + failed) : 0.0;
            double avgProcessingTime = processed > 0 ? (double) totalProcessingTimeMs.sum() / processed : 0.0;
            
            Map<String, EventTypeMetrics> eventMetrics = new HashMap<>();
            eventTypeMetrics.forEach((eventType, collector) -> 
                eventMetrics.put(eventType, collector.toMetrics(eventType)));
            
            Map<String, SubscriberMetrics> subMetrics = new HashMap<>();
            subscriberMetrics.forEach((subscriberId, collector) -> 
                subMetrics.put(subscriberId, collector.toMetrics(subscriberId)));
            
            return new EventBusMetricsSnapshot(
                Instant.now(),
                totalEventsPublished.sum(),
                processed,
                failed,
                totalDeadLettered.sum(),
                totalRetries.sum(),
                totalTimeouts.sum(),
                totalSubscriptions.sum(),
                avgProcessingTime,
                successRate,
                eventMetrics,
                subMetrics
            );
        });
    }
    
    @Override
    public CompletableFuture<EventTypeMetrics> getEventTypeMetrics(String eventType) {
        return CompletableFuture.supplyAsync(() -> 
            getEventTypeCollector(eventType).toMetrics(eventType));
    }
    
    @Override
    public CompletableFuture<SubscriberMetrics> getSubscriberMetrics(String subscriberId) {
        return CompletableFuture.supplyAsync(() -> 
            getSubscriberCollector(subscriberId).toMetrics(subscriberId));
    }
    
    @Override
    public void reset() {
        totalEventsPublished.reset();
        totalEventsProcessed.reset();
        totalEventsFailed.reset();
        totalDeadLettered.reset();
        totalRetries.reset();
        totalTimeouts.reset();
        totalSubscriptions.reset();
        totalProcessingTimeMs.reset();
        eventTypeMetrics.clear();
        subscriberMetrics.clear();
    }
    
    @Override
    public CompletableFuture<String> exportMetrics(MetricsFormat format) {
        return getMetricsSnapshot().thenApply(snapshot -> {
            return switch (format) {
                case JSON -> exportAsJson(snapshot);
                case PROMETHEUS -> exportAsPrometheus(snapshot);
                case TEXT -> exportAsText(snapshot);
                case CSV -> exportAsCsv(snapshot);
            };
        });
    }
    
    private EventTypeMetricsCollector getEventTypeCollector(String eventType) {
        return eventTypeMetrics.computeIfAbsent(eventType, k -> new EventTypeMetricsCollector());
    }
    
    private SubscriberMetricsCollector getSubscriberCollector(String subscriberId) {
        return subscriberMetrics.computeIfAbsent(subscriberId, k -> new SubscriberMetricsCollector());
    }
    
    private String exportAsJson(EventBusMetricsSnapshot snapshot) {
        return """
            {
              "timestamp": "%s",
              "totalEventsPublished": %d,
              "totalEventsProcessed": %d,
              "totalEventsFailed": %d,
              "successRate": %.4f,
              "averageProcessingTimeMs": %.2f
            }""".formatted(
                snapshot.getTimestamp(),
                snapshot.getTotalEventsPublished(),
                snapshot.getTotalEventsProcessed(),
                snapshot.getTotalEventsFailed(),
                snapshot.getSuccessRate(),
                snapshot.getAverageProcessingTimeMs()
            );
    }
    
    private String exportAsPrometheus(EventBusMetricsSnapshot snapshot) {
        return """
            # HELP eventbus_events_published_total Total number of events published
            # TYPE eventbus_events_published_total counter
            eventbus_events_published_total %d
            
            # HELP eventbus_events_processed_total Total number of events processed successfully
            # TYPE eventbus_events_processed_total counter
            eventbus_events_processed_total %d
            
            # HELP eventbus_events_failed_total Total number of events failed
            # TYPE eventbus_events_failed_total counter
            eventbus_events_failed_total %d
            
            # HELP eventbus_success_rate Success rate of event processing
            # TYPE eventbus_success_rate gauge
            eventbus_success_rate %.4f
            """.formatted(
                snapshot.getTotalEventsPublished(),
                snapshot.getTotalEventsProcessed(),
                snapshot.getTotalEventsFailed(),
                snapshot.getSuccessRate()
            );
    }
    
    private String exportAsText(EventBusMetricsSnapshot snapshot) {
        return snapshot.toString();
    }
    
    private String exportAsCsv(EventBusMetricsSnapshot snapshot) {
        return "timestamp,published,processed,failed,success_rate,avg_processing_time_ms\n" +
               "%s,%d,%d,%d,%.4f,%.2f".formatted(
                   snapshot.getTimestamp(),
                   snapshot.getTotalEventsPublished(),
                   snapshot.getTotalEventsProcessed(),
                   snapshot.getTotalEventsFailed(),
                   snapshot.getSuccessRate(),
                   snapshot.getAverageProcessingTimeMs()
               );
    }
    
    private static class EventTypeMetricsCollector {
        final LongAdder published = new LongAdder();
        final LongAdder processed = new LongAdder();
        final LongAdder failed = new LongAdder();
        final LongAdder deadLettered = new LongAdder();
        final LongAdder retries = new LongAdder();
        final LongAdder processingTimeMs = new LongAdder();
        
        void recordProcessed(long timeMs) {
            processed.increment();
            processingTimeMs.add(timeMs);
        }
        
        void recordFailed(long timeMs) {
            failed.increment();
            processingTimeMs.add(timeMs);
        }
        
        EventTypeMetrics toMetrics(String eventType) {
            long processedCount = processed.sum();
            long failedCount = failed.sum();
            double avgTime = (processedCount + failedCount) > 0 ? 
                (double) processingTimeMs.sum() / (processedCount + failedCount) : 0.0;
            
            return new EventTypeMetrics(
                eventType,
                published.sum(),
                processedCount,
                failedCount,
                deadLettered.sum(),
                retries.sum(),
                avgTime
            );
        }
    }
    
    private static class SubscriberMetricsCollector {
        final LongAdder processed = new LongAdder();
        final LongAdder failed = new LongAdder();
        final LongAdder deadLettered = new LongAdder();
        final LongAdder retries = new LongAdder();
        final LongAdder timeouts = new LongAdder();
        final LongAdder processingTimeMs = new LongAdder();
        
        void recordProcessed(long timeMs) {
            processed.increment();
            processingTimeMs.add(timeMs);
        }
        
        void recordFailed(long timeMs) {
            failed.increment();
            processingTimeMs.add(timeMs);
        }
        
        SubscriberMetrics toMetrics(String subscriberId) {
            long processedCount = processed.sum();
            long failedCount = failed.sum();
            double avgTime = (processedCount + failedCount) > 0 ? 
                (double) processingTimeMs.sum() / (processedCount + failedCount) : 0.0;
            
            return new SubscriberMetrics(
                subscriberId,
                processedCount,
                failedCount,
                deadLettered.sum(),
                retries.sum(),
                timeouts.sum(),
                avgTime
            );
        }
    }
}
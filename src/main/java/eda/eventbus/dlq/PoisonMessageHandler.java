package eda.eventbus.dlq;

import eda.eventbus.core.GenericEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Handler for detecting and managing poison messages
 */
public class PoisonMessageHandler<T> {
    private static final Logger LOGGER = Logger.getLogger(PoisonMessageHandler.class.getName());
    
    private final PoisonMessagePolicy policy;
    private final DeadLetterQueue<T> deadLetterQueue;
    private final ConcurrentHashMap<String, MessageFailureTracker> failureTrackers = new ConcurrentHashMap<>();
    private final Consumer<PoisonMessageEvent<T>> poisonMessageCallback;
    
    // Statistics
    private final AtomicLong totalPoisonMessages = new AtomicLong();
    private final AtomicLong totalQuarantined = new AtomicLong();
    private final AtomicLong totalDiscarded = new AtomicLong();
    
    public PoisonMessageHandler(PoisonMessagePolicy policy, DeadLetterQueue<T> deadLetterQueue) {
        this(policy, deadLetterQueue, null);
    }
    
    public PoisonMessageHandler(PoisonMessagePolicy policy, DeadLetterQueue<T> deadLetterQueue, 
                               Consumer<PoisonMessageEvent<T>> poisonMessageCallback) {
        this.policy = policy;
        this.deadLetterQueue = deadLetterQueue;
        this.poisonMessageCallback = poisonMessageCallback;
        
        LOGGER.info("PoisonMessageHandler initialized with policy: " + policy);
    }
    
    /**
     * Handle a failed event and determine if it's a poison message
     */
    public PoisonMessageResult<T> handleFailedEvent(GenericEvent<T, ?> event, Exception exception, int attemptNumber) {
        String eventKey = generateEventKey(event);
        
        // Track failure
        MessageFailureTracker tracker = failureTrackers.computeIfAbsent(eventKey, 
            k -> new MessageFailureTracker(eventKey));
        tracker.recordFailure(exception, attemptNumber);
        
        // Check if this is a poison message
        boolean isPoisonMessage = isPoisonMessage(event, exception, tracker);
        
        if (isPoisonMessage) {
            return handlePoisonMessage(event, exception, tracker);
        }
        
        // Not a poison message, continue with normal retry logic
        return PoisonMessageResult.continueProcessing(event, tracker.getTotalFailures());
    }
    
    /**
     * Check if an event should be considered a poison message
     */
    public boolean isPoisonMessage(GenericEvent<T, ?> event, Exception exception, MessageFailureTracker tracker) {
        // Check immediate poison conditions
        if (policy.getImmediatePoisonPredicate() != null && 
            policy.getImmediatePoisonPredicate().test(exception)) {
            return true;
        }
        
        // Check failure count threshold
        if (tracker.getTotalFailures() >= policy.getMaxFailuresBeforePoison()) {
            return true;
        }
        
        // Check consecutive same error threshold
        if (tracker.getConsecutiveSameErrors() >= policy.getMaxConsecutiveSameErrors()) {
            return true;
        }
        
        // Check failure rate in time window
        if (policy.getFailureRateWindow() != null) {
            double recentFailureRate = tracker.getFailureRateInWindow(policy.getFailureRateWindow());
            if (recentFailureRate >= policy.getMaxFailureRate()) {
                return true;
            }
        }
        
        // Check specific error patterns
        if (isKnownPoisonException(exception)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get poison message statistics
     */
    public PoisonMessageStatistics getStatistics() {
        return PoisonMessageStatistics.builder()
            .totalPoisonMessages(totalPoisonMessages.get())
            .totalQuarantined(totalQuarantined.get())
            .totalDiscarded(totalDiscarded.get())
            .activeTrackers(failureTrackers.size())
            .build();
    }
    
    /**
     * Clean up old failure trackers
     */
    public void cleanup() {
        Instant cutoff = Instant.now().minus(policy.getTrackerRetention());
        failureTrackers.entrySet().removeIf(entry -> 
            entry.getValue().getLastFailureTime().isBefore(cutoff));
        
        LOGGER.fine("Cleaned up old failure trackers, remaining: " + failureTrackers.size());
    }
    
    private PoisonMessageResult<T> handlePoisonMessage(GenericEvent<T, ?> event, Exception exception, 
                                                      MessageFailureTracker tracker) {
        totalPoisonMessages.incrementAndGet();
        
        LOGGER.warning("Poison message detected: " + generateEventKey(event) + 
                      ", failures: " + tracker.getTotalFailures() + 
                      ", last error: " + exception.getClass().getSimpleName());
        
        PoisonMessageAction action = policy.getPoisonMessageAction();
        PoisonMessageEvent<T> poisonEvent = new PoisonMessageEvent<>(event, exception, tracker, action);
        
        // Notify callback if provided
        if (poisonMessageCallback != null) {
            try {
                poisonMessageCallback.accept(poisonEvent);
            } catch (Exception callbackException) {
                LOGGER.warning("Error in poison message callback: " + callbackException.getMessage());
            }
        }
        
        switch (action) {
            case QUARANTINE:
                return quarantineMessage(event, exception, tracker);
                
            case DISCARD:
                return discardMessage(event, exception, tracker);
                
            case MOVE_TO_DLQ:
                return moveToDLQ(event, exception, tracker);
                
            case MANUAL_INTERVENTION:
                return requestManualIntervention(event, exception, tracker);
                
            default:
                LOGGER.warning("Unknown poison message action: " + action);
                return moveToDLQ(event, exception, tracker);
        }
    }
    
    private PoisonMessageResult<T> quarantineMessage(GenericEvent<T, ?> event, Exception exception, 
                                                    MessageFailureTracker tracker) {
        totalQuarantined.incrementAndGet();
        
        DeadLetterEvent<T> dlqEvent = DeadLetterEvent.<T>builder()
            .originalEvent(event)
            .addFailureReason(FailureReason.poisonMessage(
                "Quarantined after " + tracker.getTotalFailures() + " failures", 
                tracker.getTotalFailures()))
            .status(DLQStatus.QUARANTINED)
            .maxRetries(0) // No retries for quarantined messages
            .build();
        
        deadLetterQueue.store(dlqEvent);
        
        LOGGER.info("Quarantined poison message: " + generateEventKey(event));
        return PoisonMessageResult.quarantined(event, tracker.getTotalFailures());
    }
    
    private PoisonMessageResult<T> discardMessage(GenericEvent<T, ?> event, Exception exception, 
                                                 MessageFailureTracker tracker) {
        totalDiscarded.incrementAndGet();
        
        LOGGER.info("Discarded poison message: " + generateEventKey(event));
        return PoisonMessageResult.discarded(event, tracker.getTotalFailures());
    }
    
    private PoisonMessageResult<T> moveToDLQ(GenericEvent<T, ?> event, Exception exception, 
                                           MessageFailureTracker tracker) {
        DeadLetterEvent<T> dlqEvent = DeadLetterEvent.<T>builder()
            .originalEvent(event)
            .addFailureReason(FailureReason.poisonMessage(
                "Poison message moved to DLQ after " + tracker.getTotalFailures() + " failures", 
                tracker.getTotalFailures()))
            .status(DLQStatus.FAILED)
            .maxRetries(policy.getDlqRetries())
            .build();
        
        deadLetterQueue.store(dlqEvent);
        
        LOGGER.info("Moved poison message to DLQ: " + generateEventKey(event));
        return PoisonMessageResult.movedToDLQ(event, tracker.getTotalFailures());
    }
    
    private PoisonMessageResult<T> requestManualIntervention(GenericEvent<T, ?> event, Exception exception, 
                                                           MessageFailureTracker tracker) {
        DeadLetterEvent<T> dlqEvent = DeadLetterEvent.<T>builder()
            .originalEvent(event)
            .addFailureReason(FailureReason.poisonMessage(
                "Manual intervention required after " + tracker.getTotalFailures() + " failures", 
                tracker.getTotalFailures()))
            .status(DLQStatus.PENDING_MANUAL)
            .maxRetries(0)
            .build();
        
        deadLetterQueue.store(dlqEvent);
        
        LOGGER.warning("Manual intervention required for poison message: " + generateEventKey(event));
        return PoisonMessageResult.manualInterventionRequired(event, tracker.getTotalFailures());
    }
    
    private String generateEventKey(GenericEvent<T, ?> event) {
        // Generate a unique key for the event to track failures
        // In practice, this might use event ID, content hash, or other identifying information
        return event.getEventType() + ":" + event.getEventId() + ":" + event.getData().hashCode();
    }
    
    private boolean isKnownPoisonException(Exception exception) {
        // Common exceptions that typically indicate poison messages
        String exceptionName = exception.getClass().getSimpleName().toLowerCase();
        
        return exceptionName.contains("serialization") ||
               exceptionName.contains("deserialization") ||
               exceptionName.contains("parse") ||
               exceptionName.contains("format") ||
               exceptionName.contains("encoding") ||
               (exception instanceof ClassCastException) ||
               (exception instanceof NumberFormatException) ||
               (exception instanceof IllegalArgumentException && 
                exception.getMessage() != null && 
                exception.getMessage().toLowerCase().contains("invalid"));
    }
    
    /**
     * Message failure tracker for individual events
     */
    public static class MessageFailureTracker {
        private final String eventKey;
        private final java.util.List<FailureRecord> failures = new java.util.concurrent.CopyOnWriteArrayList<>();
        private volatile Instant lastFailureTime;
        private volatile String lastErrorType;
        private volatile int consecutiveSameErrors = 0;
        
        public MessageFailureTracker(String eventKey) {
            this.eventKey = eventKey;
        }
        
        public void recordFailure(Exception exception, int attemptNumber) {
            String errorType = exception.getClass().getSimpleName();
            Instant now = Instant.now();
            
            failures.add(new FailureRecord(now, errorType, exception.getMessage(), attemptNumber));
            lastFailureTime = now;
            
            if (errorType.equals(lastErrorType)) {
                consecutiveSameErrors++;
            } else {
                consecutiveSameErrors = 1;
                lastErrorType = errorType;
            }
        }
        
        public int getTotalFailures() {
            return failures.size();
        }
        
        public int getConsecutiveSameErrors() {
            return consecutiveSameErrors;
        }
        
        public Instant getLastFailureTime() {
            return lastFailureTime != null ? lastFailureTime : Instant.now();
        }
        
        public double getFailureRateInWindow(Duration window) {
            Instant cutoff = Instant.now().minus(window);
            long recentFailures = failures.stream()
                .mapToLong(f -> f.timestamp.isAfter(cutoff) ? 1 : 0)
                .sum();
            
            return (double) recentFailures / window.toMinutes(); // failures per minute
        }
        
        public String getEventKey() {
            return eventKey;
        }
        
        private static class FailureRecord {
            final Instant timestamp;
            final String errorType;
            final String errorMessage;
            final int attemptNumber;
            
            FailureRecord(Instant timestamp, String errorType, String errorMessage, int attemptNumber) {
                this.timestamp = timestamp;
                this.errorType = errorType;
                this.errorMessage = errorMessage;
                this.attemptNumber = attemptNumber;
            }
        }
    }
}
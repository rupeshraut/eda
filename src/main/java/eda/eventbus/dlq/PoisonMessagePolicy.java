package eda.eventbus.dlq;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * Policy for handling poison messages
 */
public class PoisonMessagePolicy {
    private final int maxFailuresBeforePoison;
    private final int maxConsecutiveSameErrors;
    private final Duration failureRateWindow;
    private final double maxFailureRate;
    private final PoisonMessageAction poisonMessageAction;
    private final Predicate<Exception> immediatePoisonPredicate;
    private final Duration trackerRetention;
    private final int dlqRetries;
    
    private PoisonMessagePolicy(Builder builder) {
        this.maxFailuresBeforePoison = builder.maxFailuresBeforePoison;
        this.maxConsecutiveSameErrors = builder.maxConsecutiveSameErrors;
        this.failureRateWindow = builder.failureRateWindow;
        this.maxFailureRate = builder.maxFailureRate;
        this.poisonMessageAction = builder.poisonMessageAction;
        this.immediatePoisonPredicate = builder.immediatePoisonPredicate;
        this.trackerRetention = builder.trackerRetention;
        this.dlqRetries = builder.dlqRetries;
    }
    
    public int getMaxFailuresBeforePoison() { return maxFailuresBeforePoison; }
    public int getMaxConsecutiveSameErrors() { return maxConsecutiveSameErrors; }
    public Duration getFailureRateWindow() { return failureRateWindow; }
    public double getMaxFailureRate() { return maxFailureRate; }
    public PoisonMessageAction getPoisonMessageAction() { return poisonMessageAction; }
    public Predicate<Exception> getImmediatePoisonPredicate() { return immediatePoisonPredicate; }
    public Duration getTrackerRetention() { return trackerRetention; }
    public int getDlqRetries() { return dlqRetries; }
    
    /**
     * Default poison message policy
     */
    public static PoisonMessagePolicy defaultPolicy() {
        return builder().build();
    }
    
    /**
     * Strict policy - quick poison detection
     */
    public static PoisonMessagePolicy strictPolicy() {
        return builder()
            .maxFailuresBeforePoison(3)
            .maxConsecutiveSameErrors(2)
            .poisonMessageAction(PoisonMessageAction.QUARANTINE)
            .build();
    }
    
    /**
     * Lenient policy - more tolerance for failures
     */
    public static PoisonMessagePolicy lenientPolicy() {
        return builder()
            .maxFailuresBeforePoison(10)
            .maxConsecutiveSameErrors(5)
            .poisonMessageAction(PoisonMessageAction.MOVE_TO_DLQ)
            .dlqRetries(2)
            .build();
    }
    
    /**
     * Zero tolerance policy - immediate action on specific errors
     */
    public static PoisonMessagePolicy zeroTolerancePolicy() {
        return builder()
            .maxFailuresBeforePoison(1)
            .maxConsecutiveSameErrors(1)
            .poisonMessageAction(PoisonMessageAction.DISCARD)
            .immediatePoisonPredicate(exception -> 
                exception instanceof IllegalArgumentException ||
                exception instanceof NumberFormatException ||
                exception instanceof ClassCastException)
            .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int maxFailuresBeforePoison = 5;
        private int maxConsecutiveSameErrors = 3;
        private Duration failureRateWindow = Duration.ofMinutes(5);
        private double maxFailureRate = 10.0; // failures per minute
        private PoisonMessageAction poisonMessageAction = PoisonMessageAction.QUARANTINE;
        private Predicate<Exception> immediatePoisonPredicate;
        private Duration trackerRetention = Duration.ofHours(24);
        private int dlqRetries = 1;
        
        public Builder maxFailuresBeforePoison(int maxFailuresBeforePoison) {
            this.maxFailuresBeforePoison = maxFailuresBeforePoison;
            return this;
        }
        
        public Builder maxConsecutiveSameErrors(int maxConsecutiveSameErrors) {
            this.maxConsecutiveSameErrors = maxConsecutiveSameErrors;
            return this;
        }
        
        public Builder failureRateWindow(Duration failureRateWindow) {
            this.failureRateWindow = failureRateWindow;
            return this;
        }
        
        public Builder maxFailureRate(double maxFailureRate) {
            this.maxFailureRate = maxFailureRate;
            return this;
        }
        
        public Builder poisonMessageAction(PoisonMessageAction poisonMessageAction) {
            this.poisonMessageAction = poisonMessageAction;
            return this;
        }
        
        public Builder immediatePoisonPredicate(Predicate<Exception> immediatePoisonPredicate) {
            this.immediatePoisonPredicate = immediatePoisonPredicate;
            return this;
        }
        
        public Builder trackerRetention(Duration trackerRetention) {
            this.trackerRetention = trackerRetention;
            return this;
        }
        
        public Builder dlqRetries(int dlqRetries) {
            this.dlqRetries = dlqRetries;
            return this;
        }
        
        public PoisonMessagePolicy build() {
            return new PoisonMessagePolicy(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "PoisonMessagePolicy{maxFailures=%d, maxConsecutive=%d, action=%s, window=%s, maxRate=%.1f}",
            maxFailuresBeforePoison, maxConsecutiveSameErrors, poisonMessageAction, 
            failureRateWindow, maxFailureRate
        );
    }
}

/**
 * Actions to take when a poison message is detected
 */
enum PoisonMessageAction {
    /**
     * Move to quarantine (DLQ with no retries)
     */
    QUARANTINE,
    
    /**
     * Discard the message completely
     */
    DISCARD,
    
    /**
     * Move to DLQ with limited retries
     */
    MOVE_TO_DLQ,
    
    /**
     * Flag for manual intervention
     */
    MANUAL_INTERVENTION
}
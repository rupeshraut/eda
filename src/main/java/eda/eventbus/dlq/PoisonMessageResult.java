package eda.eventbus.dlq;

import eda.eventbus.core.GenericEvent;

/**
 * Result of poison message handling
 */
public class PoisonMessageResult<T> {
    private final GenericEvent<T, ?> event;
    private final PoisonMessageAction actionTaken;
    private final boolean isPoisonMessage;
    private final boolean shouldStopProcessing;
    private final String message;
    private final int totalFailures;
    
    private PoisonMessageResult(GenericEvent<T, ?> event, PoisonMessageAction actionTaken, 
                               boolean isPoisonMessage, boolean shouldStopProcessing, 
                               String message, int totalFailures) {
        this.event = event;
        this.actionTaken = actionTaken;
        this.isPoisonMessage = isPoisonMessage;
        this.shouldStopProcessing = shouldStopProcessing;
        this.message = message;
        this.totalFailures = totalFailures;
    }
    
    public GenericEvent<T, ?> getEvent() { return event; }
    public PoisonMessageAction getActionTaken() { return actionTaken; }
    public boolean isPoisonMessage() { return isPoisonMessage; }
    public boolean shouldStopProcessing() { return shouldStopProcessing; }
    public String getMessage() { return message; }
    public int getTotalFailures() { return totalFailures; }
    
    /**
     * Continue normal processing - not a poison message
     */
    public static <T> PoisonMessageResult<T> continueProcessing(GenericEvent<T, ?> event, int totalFailures) {
        return new PoisonMessageResult<>(event, null, false, false, 
            "Continue normal processing", totalFailures);
    }
    
    /**
     * Message was quarantined
     */
    public static <T> PoisonMessageResult<T> quarantined(GenericEvent<T, ?> event, int totalFailures) {
        return new PoisonMessageResult<>(event, PoisonMessageAction.QUARANTINE, true, true,
            "Message quarantined due to poison detection", totalFailures);
    }
    
    /**
     * Message was discarded
     */
    public static <T> PoisonMessageResult<T> discarded(GenericEvent<T, ?> event, int totalFailures) {
        return new PoisonMessageResult<>(event, PoisonMessageAction.DISCARD, true, true,
            "Message discarded due to poison detection", totalFailures);
    }
    
    /**
     * Message was moved to DLQ
     */
    public static <T> PoisonMessageResult<T> movedToDLQ(GenericEvent<T, ?> event, int totalFailures) {
        return new PoisonMessageResult<>(event, PoisonMessageAction.MOVE_TO_DLQ, true, true,
            "Message moved to DLQ due to poison detection", totalFailures);
    }
    
    /**
     * Manual intervention is required
     */
    public static <T> PoisonMessageResult<T> manualInterventionRequired(GenericEvent<T, ?> event, int totalFailures) {
        return new PoisonMessageResult<>(event, PoisonMessageAction.MANUAL_INTERVENTION, true, true,
            "Manual intervention required for poison message", totalFailures);
    }
    
    @Override
    public String toString() {
        return String.format(
            "PoisonMessageResult{poison=%s, action=%s, stopProcessing=%s, failures=%d, message='%s'}",
            isPoisonMessage, actionTaken, shouldStopProcessing, totalFailures, message
        );
    }
}
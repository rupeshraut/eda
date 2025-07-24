package eda.eventbus.dlq;

import java.time.Instant;

/**
 * Represents a failure reason for an event in DLQ
 */
public class FailureReason {
    private final Instant timestamp;
    private final String errorType;
    private final String errorMessage;
    private final String stackTrace;
    private final String processingStage;
    private final String consumerInfo;
    private final int attemptNumber;
    private final boolean isPoisonMessage;
    private final boolean isRetryable;
    
    private FailureReason(Builder builder) {
        this.timestamp = builder.timestamp;
        this.errorType = builder.errorType;
        this.errorMessage = builder.errorMessage;
        this.stackTrace = builder.stackTrace;
        this.processingStage = builder.processingStage;
        this.consumerInfo = builder.consumerInfo;
        this.attemptNumber = builder.attemptNumber;
        this.isPoisonMessage = builder.isPoisonMessage;
        this.isRetryable = builder.isRetryable;
    }
    
    public Instant getTimestamp() { return timestamp; }
    public String getErrorType() { return errorType; }
    public String getErrorMessage() { return errorMessage; }
    public String getStackTrace() { return stackTrace; }
    public String getProcessingStage() { return processingStage; }
    public String getConsumerInfo() { return consumerInfo; }
    public int getAttemptNumber() { return attemptNumber; }
    public boolean isPoisonMessage() { return isPoisonMessage; }
    public boolean isRetryable() { return isRetryable; }
    
    /**
     * Create failure reason from exception
     */
    public static FailureReason fromException(Exception exception, int attemptNumber) {
        return builder()
            .timestamp(Instant.now())
            .errorType(exception.getClass().getSimpleName())
            .errorMessage(exception.getMessage())
            .stackTrace(getStackTraceString(exception))
            .attemptNumber(attemptNumber)
            .isRetryable(isExceptionRetryable(exception))
            .isPoisonMessage(isPoisonMessageException(exception))
            .build();
    }
    
    /**
     * Create failure reason for poison message
     */
    public static FailureReason poisonMessage(String reason, int attemptNumber) {
        return builder()
            .timestamp(Instant.now())
            .errorType("PoisonMessage")
            .errorMessage(reason)
            .attemptNumber(attemptNumber)
            .isPoisonMessage(true)
            .isRetryable(false)
            .build();
    }
    
    /**
     * Create failure reason for processing timeout
     */
    public static FailureReason timeout(String stage, int attemptNumber) {
        return builder()
            .timestamp(Instant.now())
            .errorType("TimeoutException")
            .errorMessage("Processing timeout in stage: " + stage)
            .processingStage(stage)
            .attemptNumber(attemptNumber)
            .isRetryable(true)
            .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Instant timestamp = Instant.now();
        private String errorType;
        private String errorMessage;
        private String stackTrace;
        private String processingStage;
        private String consumerInfo;
        private int attemptNumber;
        private boolean isPoisonMessage = false;
        private boolean isRetryable = true;
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder errorType(String errorType) {
            this.errorType = errorType;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }
        
        public Builder processingStage(String processingStage) {
            this.processingStage = processingStage;
            return this;
        }
        
        public Builder consumerInfo(String consumerInfo) {
            this.consumerInfo = consumerInfo;
            return this;
        }
        
        public Builder attemptNumber(int attemptNumber) {
            this.attemptNumber = attemptNumber;
            return this;
        }
        
        public Builder isPoisonMessage(boolean isPoisonMessage) {
            this.isPoisonMessage = isPoisonMessage;
            return this;
        }
        
        public Builder isRetryable(boolean isRetryable) {
            this.isRetryable = isRetryable;
            return this;
        }
        
        public FailureReason build() {
            return new FailureReason(this);
        }
    }
    
    private static String getStackTraceString(Exception exception) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
    
    private static boolean isExceptionRetryable(Exception exception) {
        // Non-retryable exceptions
        if (exception instanceof IllegalArgumentException ||
            exception instanceof IllegalStateException ||
            exception instanceof UnsupportedOperationException ||
            exception instanceof SecurityException) {
            return false;
        }
        
        // Retryable exceptions
        if (exception instanceof java.net.SocketTimeoutException ||
            exception instanceof java.net.ConnectException ||
            exception instanceof java.util.concurrent.TimeoutException) {
            return true;
        }
        
        // Default to retryable
        return true;
    }
    
    private static boolean isPoisonMessageException(Exception exception) {
        // Serialization/deserialization errors often indicate poison messages
        String exceptionName = exception.getClass().getSimpleName().toLowerCase();
        return exceptionName.contains("serialization") ||
               exceptionName.contains("deserialization") ||
               exceptionName.contains("parse") ||
               exceptionName.contains("format") ||
               (exception instanceof ClassCastException) ||
               (exception instanceof NumberFormatException);
    }
    
    @Override
    public String toString() {
        return String.format(
            "FailureReason{timestamp=%s, attempt=%d, type='%s', message='%s', poison=%s, retryable=%s}",
            timestamp, attemptNumber, errorType, errorMessage, isPoisonMessage, isRetryable
        );
    }
}
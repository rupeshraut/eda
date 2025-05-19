package eda.resiliency;

/**
 * Retry Pattern
 * <p>
 * Automatically retries operations that might fail due to transient issues.
 */
public class RetryExecutor {
    private final int maxRetries;
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double backoffMultiplier;

    public RetryExecutor(int maxRetries, long initialDelayMs, long maxDelayMs, double backoffMultiplier) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    public <T> T executeWithRetry(RetryableTask<T> task) throws Exception {
        int retryCount = 0;
        long delay = initialDelayMs;
        Exception lastException = null;

        while (retryCount <= maxRetries) {
            try {
                if (retryCount > 0) {
                    System.out.println("Retry attempt " + retryCount + " after " + delay + "ms");
                }

                return task.execute();
            } catch (Exception e) {
                lastException = e;

                if (retryCount >= maxRetries || !isRetryable(e)) {
                    break;
                }

                // Wait before next retry with exponential backoff
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }

                // Calculate next delay with exponential backoff
                delay = Math.min((long) (delay * backoffMultiplier), maxDelayMs);
                retryCount++;
            }
        }

        throw new RetryException("Failed after " + retryCount + " retries", lastException);
    }

    private boolean isRetryable(Exception e) {
        // Determine which exceptions should trigger a retry
        // Usually network timeouts, temporary service unavailability, etc.
        // For demonstration, we'll retry everything except specific exceptions
        return !(e instanceof IllegalArgumentException ||
                e instanceof IllegalStateException ||
                e instanceof CircuitBreaker.CircuitBreakerOpenException);
    }

    @FunctionalInterface
    public interface RetryableTask<T> {
        T execute() throws Exception;
    }

    public static class RetryException extends Exception {
        public RetryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
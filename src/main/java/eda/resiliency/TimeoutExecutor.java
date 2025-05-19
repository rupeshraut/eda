package eda.resiliency;

import java.util.concurrent.*;

/**
 * Timeout Pattern
 * <p>
 * Prevents operations from hanging indefinitely by enforcing a maximum execution time.
 */
public class TimeoutExecutor {
    private final ExecutorService executorService;

    public TimeoutExecutor() {
        // Use virtual threads if on Java 21
        // this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.executorService = Executors.newCachedThreadPool();
    }

    public <T> T executeWithTimeout(Callable<T> task, long timeoutMs)
            throws TimeoutException, InterruptedException, ExecutionException {
        Future<T> future = executorService.submit(task);

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Operation timed out after " + timeoutMs + "ms");
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public static class TimeoutException extends Exception {
        public TimeoutException(String message) {
            super(message);
        }
    }
}
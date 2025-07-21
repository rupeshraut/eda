package eda.eventbus.batch;

import java.util.concurrent.CompletableFuture;

/**
 * Consumer interface for processing batches of events
 */
@FunctionalInterface
public interface BatchEventConsumer<T, D> {
    
    /**
     * Process a batch of events
     * @param batch The batch to process
     * @return CompletableFuture that completes when batch processing is done
     */
    CompletableFuture<BatchResult> processBatch(EventBatch<T, D> batch);
}
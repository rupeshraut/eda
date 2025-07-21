package eda.eventbus.example;

import eda.eventbus.GenericEventBus;
import eda.eventbus.core.DefaultEvent;
import eda.eventbus.core.GenericEvent;
import eda.eventbus.subscription.EventConsumer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple test to verify basic event bus functionality
 */
public class SimpleTest {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Simple Event Bus Test ===");
        
        // Create event bus for String event types
        GenericEventBus<String> eventBus = new GenericEventBus<>();
        
        // Counter for received events
        AtomicInteger counter = new AtomicInteger(0);
        
        // Subscribe to "test" events
        eventBus.subscribe("test", new EventConsumer<String, String>() {
            @Override
            public CompletableFuture<Void> handle(GenericEvent<String, String> event) {
                System.out.println("Received event: " + event.getData());
                counter.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        });
        
        // Create and publish event
        GenericEvent<String, String> event = DefaultEvent.<String, String>builder()
            .eventType("test")
            .data("Hello, Generic Event Bus!")
            .source("simple-test")
            .build();
        
        eventBus.publish(event);
        
        // Wait for processing
        Thread.sleep(500);
        
        System.out.println("Events processed: " + counter.get());
        System.out.println("Success: " + (counter.get() == 1));
        
        // Shutdown
        eventBus.shutdown();
    }
}
package eda.eventbus.schema;

import eda.eventbus.core.GenericEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Registry for managing event schemas and versions
 */
public interface EventSchemaRegistry {
    
    /**
     * Register a new schema version
     */
    CompletableFuture<Void> registerSchema(EventSchema schema);
    
    /**
     * Get schema by event type and version
     */
    CompletableFuture<Optional<EventSchema>> getSchema(String eventType, String version);
    
    /**
     * Get latest schema for event type
     */
    CompletableFuture<Optional<EventSchema>> getLatestSchema(String eventType);
    
    /**
     * Get all versions for an event type
     */
    CompletableFuture<List<String>> getVersions(String eventType);
    
    /**
     * Get all registered event types
     */
    CompletableFuture<List<String>> getEventTypes();
    
    /**
     * Validate event against its schema
     */
    CompletableFuture<ValidationResult> validateEvent(GenericEvent<?, ?> event);
    
    /**
     * Migrate event to latest schema version
     */
    CompletableFuture<GenericEvent<?, ?>> migrateToLatest(GenericEvent<?, ?> event);
    
    /**
     * Migrate event to specific schema version
     */
    CompletableFuture<GenericEvent<?, ?>> migrateToVersion(GenericEvent<?, ?> event, String targetVersion);
    
    /**
     * Check compatibility between schema versions
     */
    CompletableFuture<Boolean> isCompatible(String eventType, String fromVersion, String toVersion);
    
    /**
     * Remove schema version
     */
    CompletableFuture<Boolean> removeSchema(String eventType, String version);
    
    /**
     * Get schema evolution path between versions
     */
    CompletableFuture<List<String>> getEvolutionPath(String eventType, String fromVersion, String toVersion);
}
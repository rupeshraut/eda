package eda.eventbus.schema;

import eda.eventbus.core.GenericEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * In-memory implementation of EventSchemaRegistry
 */
public class InMemoryEventSchemaRegistry implements EventSchemaRegistry {
    private static final Logger LOGGER = Logger.getLogger(InMemoryEventSchemaRegistry.class.getName());
    
    // eventType -> version -> schema
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, EventSchema>> schemas = new ConcurrentHashMap<>();
    
    // eventType -> latest version
    private final ConcurrentHashMap<String, String> latestVersions = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicLong totalSchemas = new AtomicLong();
    private final AtomicLong validationCount = new AtomicLong();
    private final AtomicLong migrationCount = new AtomicLong();
    
    // Schema evolution cache
    private final ConcurrentHashMap<String, List<String>> evolutionPaths = new ConcurrentHashMap<>();
    
    // Configuration
    private final SchemaRegistryConfig config;
    
    public InMemoryEventSchemaRegistry() {
        this(SchemaRegistryConfig.defaultConfig());
    }
    
    public InMemoryEventSchemaRegistry(SchemaRegistryConfig config) {
        this.config = config;
        LOGGER.info("InMemoryEventSchemaRegistry initialized with config: " + config);
    }
    
    @Override
    public CompletableFuture<Void> registerSchema(EventSchema schema) {
        return CompletableFuture.runAsync(() -> {
            String eventType = schema.getEventType();
            String version = schema.getVersion();
            
            // Validate schema before registration
            validateSchemaForRegistration(schema);
            
            // Check compatibility if there are existing schemas
            if (schemas.containsKey(eventType)) {
                validateCompatibility(eventType, schema);
            }
            
            // Register the schema
            schemas.computeIfAbsent(eventType, k -> new ConcurrentHashMap<>())
                   .put(version, schema);
            
            // Update latest version
            updateLatestVersion(eventType, version);
            
            // Clear evolution path cache for this event type
            clearEvolutionPathCache(eventType);
            
            totalSchemas.incrementAndGet();
            
            LOGGER.info("Registered schema: " + eventType + ":" + version);
        });
    }
    
    @Override
    public CompletableFuture<Optional<EventSchema>> getSchema(String eventType, String version) {
        return CompletableFuture.supplyAsync(() -> {
            ConcurrentHashMap<String, EventSchema> typeSchemas = schemas.get(eventType);
            if (typeSchemas == null) {
                return Optional.empty();
            }
            
            EventSchema schema = typeSchemas.get(version);
            return Optional.ofNullable(schema);
        });
    }
    
    @Override
    public CompletableFuture<Optional<EventSchema>> getLatestSchema(String eventType) {
        return CompletableFuture.supplyAsync(() -> {
            String latestVersion = latestVersions.get(eventType);
            if (latestVersion == null) {
                return Optional.empty();
            }
            
            return getSchema(eventType, latestVersion).join();
        });
    }
    
    @Override
    public CompletableFuture<List<String>> getVersions(String eventType) {
        return CompletableFuture.supplyAsync(() -> {
            ConcurrentHashMap<String, EventSchema> typeSchemas = schemas.get(eventType);
            if (typeSchemas == null) {
                return List.of();
            }
            
            return typeSchemas.keySet().stream()
                    .sorted(this::compareVersions)
                    .collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<List<String>> getEventTypes() {
        return CompletableFuture.supplyAsync(() -> 
            new ArrayList<>(schemas.keySet()));
    }
    
    @Override
    public CompletableFuture<ValidationResult> validateEvent(GenericEvent<?, ?> event) {
        return CompletableFuture.supplyAsync(() -> {
            validationCount.incrementAndGet();
            
            String eventType = event.getEventType().toString();
            String version = extractVersionFromEvent(event);
            
            Optional<EventSchema> schemaOpt = getSchema(eventType, version).join();
            if (schemaOpt.isEmpty()) {
                return ValidationResult.failure(new ValidationResult.ValidationError(
                    "schema",
                    "No schema found for event type '" + eventType + "' version '" + version + "'",
                    ValidationResult.ErrorType.SCHEMA_MISMATCH
                ));
            }
            
            return schemaOpt.get().validate(event);
        });
    }
    
    @Override
    public CompletableFuture<GenericEvent<?, ?>> migrateToLatest(GenericEvent<?, ?> event) {
        return CompletableFuture.supplyAsync(() -> {
            String eventType = event.getEventType().toString();
            String currentVersion = extractVersionFromEvent(event);
            String latestVersion = latestVersions.get(eventType);
            
            if (latestVersion == null || latestVersion.equals(currentVersion)) {
                return event; // No migration needed
            }
            
            return migrateToVersion(event, latestVersion).join();
        });
    }
    
    @Override
    public CompletableFuture<GenericEvent<?, ?>> migrateToVersion(GenericEvent<?, ?> event, String targetVersion) {
        return CompletableFuture.supplyAsync(() -> {
            migrationCount.incrementAndGet();
            
            String eventType = event.getEventType().toString();
            String currentVersion = extractVersionFromEvent(event);
            
            if (currentVersion.equals(targetVersion)) {
                return event; // No migration needed
            }
            
            // Get evolution path
            List<String> evolutionPath = getEvolutionPath(eventType, currentVersion, targetVersion).join();
            if (evolutionPath.isEmpty()) {
                throw new RuntimeException("No evolution path found from " + currentVersion + " to " + targetVersion);
            }
            
            // Apply migrations step by step
            GenericEvent<?, ?> migratedEvent = event;
            for (int i = 0; i < evolutionPath.size() - 1; i++) {
                String fromVersion = evolutionPath.get(i);
                String toVersion = evolutionPath.get(i + 1);
                
                Optional<EventSchema> fromSchema = getSchema(eventType, fromVersion).join();
                if (fromSchema.isPresent()) {
                    migratedEvent = fromSchema.get().migrate(migratedEvent, toVersion, this);
                }
            }
            
            LOGGER.fine("Migrated event from " + currentVersion + " to " + targetVersion);
            return migratedEvent;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> isCompatible(String eventType, String fromVersion, String toVersion) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<EventSchema> fromSchema = getSchema(eventType, fromVersion).join();
            Optional<EventSchema> toSchema = getSchema(eventType, toVersion).join();
            
            if (fromSchema.isEmpty() || toSchema.isEmpty()) {
                return false;
            }
            
            return fromSchema.get().isCompatibleWith(toSchema.get());
        });
    }
    
    @Override
    public CompletableFuture<Boolean> removeSchema(String eventType, String version) {
        return CompletableFuture.supplyAsync(() -> {
            ConcurrentHashMap<String, EventSchema> typeSchemas = schemas.get(eventType);
            if (typeSchemas == null) {
                return false;
            }
            
            EventSchema removed = typeSchemas.remove(version);
            if (removed != null) {
                totalSchemas.decrementAndGet();
                
                // Update latest version if needed
                if (version.equals(latestVersions.get(eventType))) {
                    updateLatestVersionAfterRemoval(eventType);
                }
                
                // Clear evolution path cache
                clearEvolutionPathCache(eventType);
                
                LOGGER.info("Removed schema: " + eventType + ":" + version);
                return true;
            }
            
            return false;
        });
    }
    
    @Override
    public CompletableFuture<List<String>> getEvolutionPath(String eventType, String fromVersion, String toVersion) {
        return CompletableFuture.supplyAsync(() -> {
            String cacheKey = eventType + ":" + fromVersion + ":" + toVersion;
            
            // Check cache first
            List<String> cachedPath = evolutionPaths.get(cacheKey);
            if (cachedPath != null) {
                return cachedPath;
            }
            
            // Calculate evolution path
            List<String> path = calculateEvolutionPath(eventType, fromVersion, toVersion);
            
            // Cache the result
            if (!path.isEmpty()) {
                evolutionPaths.put(cacheKey, path);
            }
            
            return path;
        });
    }
    
    /**
     * Get registry statistics
     */
    public SchemaRegistryStatistics getStatistics() {
        return SchemaRegistryStatistics.builder()
            .totalSchemas(totalSchemas.get())
            .eventTypes(schemas.size())
            .validationCount(validationCount.get())
            .migrationCount(migrationCount.get())
            .cacheSize(evolutionPaths.size())
            .build();
    }
    
    /**
     * Clear all schemas (for testing)
     */
    public void clear() {
        schemas.clear();
        latestVersions.clear();
        evolutionPaths.clear();
        totalSchemas.set(0);
        LOGGER.info("Cleared all schemas from registry");
    }
    
    private void validateSchemaForRegistration(EventSchema schema) {
        if (schema.getEventType() == null || schema.getEventType().trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        
        if (schema.getVersion() == null || schema.getVersion().trim().isEmpty()) {
            throw new IllegalArgumentException("Schema version cannot be null or empty");
        }
        
        // Additional validation based on config
        if (config.isEnforceSchemaValidation()) {
            // Perform additional schema validation
            LOGGER.fine("Schema validation passed for " + schema.getEventType() + ":" + schema.getVersion());
        }
    }
    
    private void validateCompatibility(String eventType, EventSchema newSchema) {
        if (!config.isEnforceCompatibility()) {
            return;
        }
        
        Optional<EventSchema> latestSchema = getLatestSchema(eventType).join();
        if (latestSchema.isPresent()) {
            if (!newSchema.isCompatibleWith(latestSchema.get())) {
                throw new RuntimeException("Schema " + newSchema.getVersion() + 
                    " is not compatible with latest version " + latestSchema.get().getVersion());
            }
        }
    }
    
    private void updateLatestVersion(String eventType, String version) {
        latestVersions.compute(eventType, (key, currentLatest) -> {
            if (currentLatest == null) {
                return version;
            }
            
            // Compare versions and return the later one
            return compareVersions(version, currentLatest) > 0 ? version : currentLatest;
        });
    }
    
    private void updateLatestVersionAfterRemoval(String eventType) {
        ConcurrentHashMap<String, EventSchema> typeSchemas = schemas.get(eventType);
        if (typeSchemas == null || typeSchemas.isEmpty()) {
            latestVersions.remove(eventType);
            return;
        }
        
        String newLatest = typeSchemas.keySet().stream()
            .max(this::compareVersions)
            .orElse(null);
        
        if (newLatest != null) {
            latestVersions.put(eventType, newLatest);
        } else {
            latestVersions.remove(eventType);
        }
    }
    
    private int compareVersions(String version1, String version2) {
        // Simple version comparison (1.0.0 vs 1.0.1)
        // In a real implementation, this would use semantic versioning
        return version1.compareTo(version2);
    }
    
    private String extractVersionFromEvent(GenericEvent<?, ?> event) {
        // Try to extract version from event headers or use default
        if (event.getHeaders().containsKey("schemaVersion")) {
            return event.getHeaders().get("schemaVersion");
        }
        
        // Default to latest version or "1.0.0"
        String eventType = event.getEventType().toString();
        String latest = latestVersions.get(eventType);
        return latest != null ? latest : "1.0.0";
    }
    
    private List<String> calculateEvolutionPath(String eventType, String fromVersion, String toVersion) {
        // Simple path calculation - in practice this would be more sophisticated
        List<String> allVersions = getVersions(eventType).join();
        
        int fromIndex = allVersions.indexOf(fromVersion);
        int toIndex = allVersions.indexOf(toVersion);
        
        if (fromIndex == -1 || toIndex == -1) {
            return List.of(); // No path found
        }
        
        if (fromIndex <= toIndex) {
            // Forward evolution
            return allVersions.subList(fromIndex, toIndex + 1);
        } else {
            // Backward evolution (if supported)
            List<String> path = new ArrayList<>();
            for (int i = fromIndex; i >= toIndex; i--) {
                path.add(allVersions.get(i));
            }
            return path;
        }
    }
    
    private void clearEvolutionPathCache(String eventType) {
        evolutionPaths.entrySet().removeIf(entry -> entry.getKey().startsWith(eventType + ":"));
    }
}
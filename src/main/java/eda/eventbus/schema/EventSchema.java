package eda.eventbus.schema;

import eda.eventbus.core.GenericEvent;
import java.util.List;

/**
 * Interface for event schema definition and validation
 */
public interface EventSchema {
    
    /**
     * Get schema version
     */
    String getVersion();
    
    /**
     * Get event type this schema applies to
     */
    String getEventType();
    
    /**
     * Validate an event against this schema
     */
    ValidationResult validate(GenericEvent<?, ?> event);
    
    /**
     * Check if this schema is compatible with another version
     */
    boolean isCompatibleWith(EventSchema otherSchema);
    
    /**
     * Migrate event from this schema version to target version
     */
    GenericEvent<?, ?> migrate(GenericEvent<?, ?> event, String targetVersion, EventSchemaRegistry registry);
    
    /**
     * Get required fields for this schema
     */
    List<String> getRequiredFields();
    
    /**
     * Get optional fields for this schema
     */
    List<String> getOptionalFields();
    
    /**
     * Get field type information
     */
    FieldType getFieldType(String fieldName);
    
    /**
     * Get schema description
     */
    String getDescription();
}
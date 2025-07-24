package eda.eventbus.schema;

import eda.eventbus.core.GenericEvent;

import java.util.*;
import java.util.logging.Logger;

/**
 * JSON-based implementation of EventSchema
 */
public class JsonEventSchema implements EventSchema {
    private static final Logger LOGGER = Logger.getLogger(JsonEventSchema.class.getName());
    
    private final String version;
    private final String eventType;
    private final String description;
    private final Map<String, FieldDefinition> fields;
    private final Set<String> requiredFields;
    private final Map<String, Object> metadata;
    private final SchemaCompatibility compatibility;
    
    private JsonEventSchema(Builder builder) {
        this.version = builder.version;
        this.eventType = builder.eventType;
        this.description = builder.description;
        this.fields = Map.copyOf(builder.fields);
        this.requiredFields = Set.copyOf(builder.requiredFields);
        this.metadata = Map.copyOf(builder.metadata);
        this.compatibility = builder.compatibility;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    @Override
    public String getEventType() {
        return eventType;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public ValidationResult validate(GenericEvent<?, ?> event) {
        List<ValidationResult.ValidationError> errors = new ArrayList<>();
        
        // Check event type matches
        if (!eventType.equals(event.getEventType().toString())) {
            errors.add(new ValidationResult.ValidationError(
                "eventType", 
                "Expected '" + eventType + "' but got '" + event.getEventType() + "'",
                ValidationResult.ErrorType.SCHEMA_MISMATCH
            ));
        }
        
        // Get event data as map for validation
        Map<String, Object> eventData = extractEventDataAsMap(event);
        
        // Check required fields
        for (String requiredField : requiredFields) {
            if (!eventData.containsKey(requiredField) || eventData.get(requiredField) == null) {
                errors.add(new ValidationResult.ValidationError(
                    requiredField,
                    "Required field '" + requiredField + "' is missing",
                    ValidationResult.ErrorType.MISSING_REQUIRED_FIELD
                ));
            }
        }
        
        // Validate field types and values
        for (Map.Entry<String, Object> entry : eventData.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            
            FieldDefinition fieldDef = fields.get(fieldName);
            if (fieldDef == null) {
                if (compatibility.isStrictFieldValidation()) {
                    errors.add(new ValidationResult.ValidationError(
                        fieldName,
                        "Unknown field '" + fieldName + "'",
                        ValidationResult.ErrorType.UNKNOWN_FIELD
                    ));
                }
                continue;
            }
            
            // Validate field type
            ValidationResult fieldValidation = validateField(fieldName, value, fieldDef);
            if (!fieldValidation.isValid()) {
                errors.addAll(fieldValidation.getErrors());
            }
        }
        
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
    
    @Override
    public boolean isCompatibleWith(EventSchema otherSchema) {
        if (!eventType.equals(otherSchema.getEventType())) {
            return false;
        }
        
        if (!(otherSchema instanceof JsonEventSchema)) {
            return false;
        }
        
        JsonEventSchema other = (JsonEventSchema) otherSchema;
        
        // Check compatibility based on evolution rules
        return compatibility.isCompatibleWith(this, other);
    }
    
    @Override
    public GenericEvent<?, ?> migrate(GenericEvent<?, ?> event, String targetVersion, 
                                     EventSchemaRegistry registry) {
        // For now, return the event as-is
        // In a full implementation, this would apply migration rules
        LOGGER.info("Migrating event from version " + version + " to " + targetVersion);
        return event;
    }
    
    @Override
    public List<String> getRequiredFields() {
        return new ArrayList<>(requiredFields);
    }
    
    @Override
    public List<String> getOptionalFields() {
        return fields.keySet().stream()
            .filter(field -> !requiredFields.contains(field))
            .sorted()
            .toList();
    }
    
    @Override
    public FieldType getFieldType(String fieldName) {
        FieldDefinition fieldDef = fields.get(fieldName);
        return fieldDef != null ? fieldDef.getType() : null;
    }
    
    /**
     * Get field definition
     */
    public FieldDefinition getFieldDefinition(String fieldName) {
        return fields.get(fieldName);
    }
    
    /**
     * Get all field definitions
     */
    public Map<String, FieldDefinition> getFields() {
        return fields;
    }
    
    /**
     * Get schema metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Get compatibility configuration
     */
    public SchemaCompatibility getCompatibility() {
        return compatibility;
    }
    
    private ValidationResult validateField(String fieldName, Object value, FieldDefinition fieldDef) {
        List<ValidationResult.ValidationError> errors = new ArrayList<>();
        
        if (value == null) {
            if (requiredFields.contains(fieldName)) {
                errors.add(new ValidationResult.ValidationError(
                    fieldName,
                    "Required field cannot be null",
                    ValidationResult.ErrorType.MISSING_REQUIRED_FIELD
                ));
            }
            return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
        }
        
        // Type validation
        if (!isValidType(value, fieldDef.getType())) {
            errors.add(new ValidationResult.ValidationError(
                fieldName,
                "Expected type " + fieldDef.getType() + " but got " + value.getClass().getSimpleName(),
                ValidationResult.ErrorType.INVALID_TYPE
            ));
        }
        
        // Value validation
        if (fieldDef.getValidator() != null) {
            try {
                if (!fieldDef.getValidator().test(value)) {
                    errors.add(new ValidationResult.ValidationError(
                        fieldName,
                        "Field value validation failed: " + fieldDef.getValidationMessage(),
                        ValidationResult.ErrorType.INVALID_VALUE
                    ));
                }
            } catch (Exception e) {
                errors.add(new ValidationResult.ValidationError(
                    fieldName,
                    "Validation error: " + e.getMessage(),
                    ValidationResult.ErrorType.INVALID_VALUE
                ));
            }
        }
        
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
    
    private boolean isValidType(Object value, FieldType expectedType) {
        return switch (expectedType) {
            case STRING -> value instanceof String;
            case INTEGER -> value instanceof Integer;
            case LONG -> value instanceof Long || value instanceof Integer;
            case DOUBLE -> value instanceof Double || value instanceof Float || value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
            case TIMESTAMP -> value instanceof java.time.Instant || value instanceof java.util.Date;
            case UUID -> value instanceof java.util.UUID || 
                        (value instanceof String && isValidUUID((String) value));
            case OBJECT -> value instanceof Map;
            case ARRAY -> value instanceof List || value.getClass().isArray();
            case MAP -> value instanceof Map;
            case ANY -> true;
        };
    }
    
    private boolean isValidUUID(String str) {
        try {
            java.util.UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractEventDataAsMap(GenericEvent<?, ?> event) {
        Object data = event.getData();
        
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        
        // Convert object to map using reflection (simplified)
        Map<String, Object> result = new HashMap<>();
        if (data != null) {
            // In a real implementation, this would use proper object mapping
            result.put("data", data);
        }
        
        return result;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String version;
        private String eventType;
        private String description = "";
        private Map<String, FieldDefinition> fields = new HashMap<>();
        private Set<String> requiredFields = new HashSet<>();
        private Map<String, Object> metadata = new HashMap<>();
        private SchemaCompatibility compatibility = SchemaCompatibility.defaultCompatibility();
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder addField(String name, FieldType type, boolean required) {
            fields.put(name, new FieldDefinition(type, null, null));
            if (required) {
                requiredFields.add(name);
            }
            return this;
        }
        
        public Builder addField(String name, FieldDefinition fieldDef, boolean required) {
            fields.put(name, fieldDef);
            if (required) {
                requiredFields.add(name);
            }
            return this;
        }
        
        public Builder addRequiredField(String name, FieldType type) {
            return addField(name, type, true);
        }
        
        public Builder addOptionalField(String name, FieldType type) {
            return addField(name, type, false);
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }
        
        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder compatibility(SchemaCompatibility compatibility) {
            this.compatibility = compatibility;
            return this;
        }
        
        public JsonEventSchema build() {
            if (version == null || version.trim().isEmpty()) {
                throw new IllegalArgumentException("Version is required");
            }
            if (eventType == null || eventType.trim().isEmpty()) {
                throw new IllegalArgumentException("Event type is required");
            }
            
            return new JsonEventSchema(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "JsonEventSchema{eventType='%s', version='%s', fields=%d, required=%d}",
            eventType, version, fields.size(), requiredFields.size()
        );
    }
}
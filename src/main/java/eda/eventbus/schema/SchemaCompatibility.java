package eda.eventbus.schema;

/**
 * Schema compatibility rules and checking
 */
public class SchemaCompatibility {
    private final CompatibilityType type;
    private final boolean strictFieldValidation;
    private final boolean allowExtraFields;
    private final boolean allowFieldRemovals;
    private final boolean allowFieldTypeChanges;
    private final boolean allowRequiredFieldAdditions;
    
    private SchemaCompatibility(Builder builder) {
        this.type = builder.type;
        this.strictFieldValidation = builder.strictFieldValidation;
        this.allowExtraFields = builder.allowExtraFields;
        this.allowFieldRemovals = builder.allowFieldRemovals;
        this.allowFieldTypeChanges = builder.allowFieldTypeChanges;
        this.allowRequiredFieldAdditions = builder.allowRequiredFieldAdditions;
    }
    
    public CompatibilityType getType() { return type; }
    public boolean isStrictFieldValidation() { return strictFieldValidation; }
    public boolean isAllowExtraFields() { return allowExtraFields; }
    public boolean isAllowFieldRemovals() { return allowFieldRemovals; }
    public boolean isAllowFieldTypeChanges() { return allowFieldTypeChanges; }
    public boolean isAllowRequiredFieldAdditions() { return allowRequiredFieldAdditions; }
    
    /**
     * Check if this schema is compatible with another schema
     */
    public boolean isCompatibleWith(JsonEventSchema thisSchema, JsonEventSchema otherSchema) {
        return switch (type) {
            case BACKWARD -> isBackwardCompatible(thisSchema, otherSchema);
            case FORWARD -> isForwardCompatible(thisSchema, otherSchema);
            case FULL -> isBackwardCompatible(thisSchema, otherSchema) && 
                        isForwardCompatible(thisSchema, otherSchema);
            case NONE -> true; // No compatibility checking
        };
    }
    
    private boolean isBackwardCompatible(JsonEventSchema newSchema, JsonEventSchema oldSchema) {
        // Backward compatibility: new schema can read data written with old schema
        
        // Check if any required fields were added
        if (!allowRequiredFieldAdditions) {
            for (String requiredField : newSchema.getRequiredFields()) {
                if (!oldSchema.getFields().containsKey(requiredField)) {
                    return false; // New required field breaks backward compatibility
                }
            }
        }
        
        // Check if any fields were removed
        if (!allowFieldRemovals) {
            for (String oldField : oldSchema.getFields().keySet()) {
                if (!newSchema.getFields().containsKey(oldField)) {
                    return false; // Removed field breaks backward compatibility
                }
            }
        }
        
        // Check field type changes
        if (!allowFieldTypeChanges) {
            for (String fieldName : oldSchema.getFields().keySet()) {
                if (newSchema.getFields().containsKey(fieldName)) {
                    FieldType oldType = oldSchema.getFieldType(fieldName);
                    FieldType newType = newSchema.getFieldType(fieldName);
                    if (oldType != newType && !areTypesCompatible(oldType, newType)) {
                        return false; // Type change breaks backward compatibility
                    }
                }
            }
        }
        
        return true;
    }
    
    private boolean isForwardCompatible(JsonEventSchema newSchema, JsonEventSchema oldSchema) {
        // Forward compatibility: old schema can read data written with new schema
        
        // Check if new schema added any required fields
        for (String requiredField : newSchema.getRequiredFields()) {
            if (!oldSchema.getFields().containsKey(requiredField)) {
                return false; // New required field breaks forward compatibility
            }
        }
        
        // Check if old schema can handle extra fields from new schema
        if (!allowExtraFields) {
            for (String newField : newSchema.getFields().keySet()) {
                if (!oldSchema.getFields().containsKey(newField)) {
                    return false; // Extra field breaks forward compatibility
                }
            }
        }
        
        return true;
    }
    
    private boolean areTypesCompatible(FieldType oldType, FieldType newType) {
        // Define type compatibility rules
        if (oldType == newType) return true;
        
        // Some compatible type changes
        return switch (oldType) {
            case INTEGER -> newType == FieldType.LONG || newType == FieldType.DOUBLE;
            case LONG -> newType == FieldType.DOUBLE;
            case ANY -> true;
            default -> newType == FieldType.ANY;
        };
    }
    
    public static SchemaCompatibility defaultCompatibility() {
        return builder().type(CompatibilityType.BACKWARD).build();
    }
    
    public static SchemaCompatibility strictCompatibility() {
        return builder()
            .type(CompatibilityType.FULL)
            .strictFieldValidation(true)
            .allowExtraFields(false)
            .allowFieldRemovals(false)
            .allowFieldTypeChanges(false)
            .allowRequiredFieldAdditions(false)
            .build();
    }
    
    public static SchemaCompatibility lenientCompatibility() {
        return builder()
            .type(CompatibilityType.NONE)
            .strictFieldValidation(false)
            .allowExtraFields(true)
            .allowFieldRemovals(true)
            .allowFieldTypeChanges(true)
            .allowRequiredFieldAdditions(true)
            .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private CompatibilityType type = CompatibilityType.BACKWARD;
        private boolean strictFieldValidation = true;
        private boolean allowExtraFields = true;
        private boolean allowFieldRemovals = false;
        private boolean allowFieldTypeChanges = false;
        private boolean allowRequiredFieldAdditions = false;
        
        public Builder type(CompatibilityType type) {
            this.type = type;
            return this;
        }
        
        public Builder strictFieldValidation(boolean strictFieldValidation) {
            this.strictFieldValidation = strictFieldValidation;
            return this;
        }
        
        public Builder allowExtraFields(boolean allowExtraFields) {
            this.allowExtraFields = allowExtraFields;
            return this;
        }
        
        public Builder allowFieldRemovals(boolean allowFieldRemovals) {
            this.allowFieldRemovals = allowFieldRemovals;
            return this;
        }
        
        public Builder allowFieldTypeChanges(boolean allowFieldTypeChanges) {
            this.allowFieldTypeChanges = allowFieldTypeChanges;
            return this;
        }
        
        public Builder allowRequiredFieldAdditions(boolean allowRequiredFieldAdditions) {
            this.allowRequiredFieldAdditions = allowRequiredFieldAdditions;
            return this;
        }
        
        public SchemaCompatibility build() {
            return new SchemaCompatibility(this);
        }
    }
    
    /**
     * Types of schema compatibility
     */
    public enum CompatibilityType {
        /**
         * New schema is backward compatible with old schema
         * (new schema can read data written with old schema)
         */
        BACKWARD,
        
        /**
         * New schema is forward compatible with old schema
         * (old schema can read data written with new schema)
         */
        FORWARD,
        
        /**
         * New schema is both backward and forward compatible
         */
        FULL,
        
        /**
         * No compatibility checking
         */
        NONE
    }
    
    @Override
    public String toString() {
        return String.format(
            "SchemaCompatibility{type=%s, strict=%s, extraFields=%s}",
            type, strictFieldValidation, allowExtraFields
        );
    }
}
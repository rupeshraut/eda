package eda.eventbus.schema;

import java.util.function.Predicate;

/**
 * Definition of a field in an event schema
 */
public class FieldDefinition {
    private final FieldType type;
    private final Predicate<Object> validator;
    private final String validationMessage;
    private final Object defaultValue;
    private final String description;
    private final boolean deprecated;
    private final String deprecationMessage;
    private final FieldConstraints constraints;
    
    public FieldDefinition(FieldType type) {
        this(type, null, null, null, null, false, null, null);
    }
    
    public FieldDefinition(FieldType type, Predicate<Object> validator, String validationMessage) {
        this(type, validator, validationMessage, null, null, false, null, null);
    }
    
    public FieldDefinition(FieldType type, Predicate<Object> validator, String validationMessage,
                          Object defaultValue, String description, boolean deprecated,
                          String deprecationMessage, FieldConstraints constraints) {
        this.type = type;
        this.validator = validator;
        this.validationMessage = validationMessage;
        this.defaultValue = defaultValue;
        this.description = description;
        this.deprecated = deprecated;
        this.deprecationMessage = deprecationMessage;
        this.constraints = constraints;
    }
    
    public FieldType getType() { return type; }
    public Predicate<Object> getValidator() { return validator; }
    public String getValidationMessage() { return validationMessage; }
    public Object getDefaultValue() { return defaultValue; }
    public String getDescription() { return description; }
    public boolean isDeprecated() { return deprecated; }
    public String getDeprecationMessage() { return deprecationMessage; }
    public FieldConstraints getConstraints() { return constraints; }
    
    /**
     * Create a new field definition with updated properties
     */
    public FieldDefinition withValidator(Predicate<Object> validator, String message) {
        return new FieldDefinition(type, validator, message, defaultValue, description,
                                 deprecated, deprecationMessage, constraints);
    }
    
    public FieldDefinition withDefault(Object defaultValue) {
        return new FieldDefinition(type, validator, validationMessage, defaultValue, description,
                                 deprecated, deprecationMessage, constraints);
    }
    
    public FieldDefinition withDescription(String description) {
        return new FieldDefinition(type, validator, validationMessage, defaultValue, description,
                                 deprecated, deprecationMessage, constraints);
    }
    
    public FieldDefinition withDeprecation(String deprecationMessage) {
        return new FieldDefinition(type, validator, validationMessage, defaultValue, description,
                                 true, deprecationMessage, constraints);
    }
    
    public FieldDefinition withConstraints(FieldConstraints constraints) {
        return new FieldDefinition(type, validator, validationMessage, defaultValue, description,
                                 deprecated, deprecationMessage, constraints);
    }
    
    /**
     * Common field definition builders
     */
    public static FieldDefinition string() {
        return new FieldDefinition(FieldType.STRING);
    }
    
    public static FieldDefinition string(int minLength, int maxLength) {
        return new FieldDefinition(FieldType.STRING, 
            value -> {
                if (!(value instanceof String)) return false;
                String str = (String) value;
                return str.length() >= minLength && str.length() <= maxLength;
            },
            "String length must be between " + minLength + " and " + maxLength,
            null, null, false, null,
            FieldConstraints.builder().minLength(minLength).maxLength(maxLength).build());
    }
    
    public static FieldDefinition email() {
        return new FieldDefinition(FieldType.STRING,
            value -> value instanceof String && isValidEmail((String) value),
            "Must be a valid email address");
    }
    
    public static FieldDefinition integer(int min, int max) {
        return new FieldDefinition(FieldType.INTEGER,
            value -> {
                if (!(value instanceof Integer)) return false;
                int intValue = (Integer) value;
                return intValue >= min && intValue <= max;
            },
            "Integer must be between " + min + " and " + max,
            null, null, false, null,
            FieldConstraints.builder().minValue(min).maxValue(max).build());
    }
    
    public static FieldDefinition positiveInteger() {
        return integer(1, Integer.MAX_VALUE);
    }
    
    public static FieldDefinition nonNegativeInteger() {
        return integer(0, Integer.MAX_VALUE);
    }
    
    public static FieldDefinition enumValue(String... allowedValues) {
        java.util.Set<String> allowed = java.util.Set.of(allowedValues);
        return new FieldDefinition(FieldType.STRING,
            value -> value instanceof String && allowed.contains(value),
            "Must be one of: " + String.join(", ", allowedValues));
    }
    
    public static FieldDefinition timestamp() {
        return new FieldDefinition(FieldType.TIMESTAMP);
    }
    
    public static FieldDefinition uuid() {
        return new FieldDefinition(FieldType.UUID);
    }
    
    public static FieldDefinition object() {
        return new FieldDefinition(FieldType.OBJECT);
    }
    
    public static FieldDefinition array() {
        return new FieldDefinition(FieldType.ARRAY);
    }
    
    public static FieldDefinition array(int minSize, int maxSize) {
        return new FieldDefinition(FieldType.ARRAY,
            value -> {
                if (value instanceof java.util.List) {
                    int size = ((java.util.List<?>) value).size();
                    return size >= minSize && size <= maxSize;
                }
                if (value.getClass().isArray()) {
                    int length = java.lang.reflect.Array.getLength(value);
                    return length >= minSize && length <= maxSize;
                }
                return false;
            },
            "Array size must be between " + minSize + " and " + maxSize);
    }
    
    private static boolean isValidEmail(String email) {
        // Simplified email validation
        return email != null && email.contains("@") && email.contains(".");
    }
    
    @Override
    public String toString() {
        return String.format("FieldDefinition{type=%s, hasValidator=%s, deprecated=%s}", 
                           type, validator != null, deprecated);
    }
}

/**
 * Constraints for field validation
 */
class FieldConstraints {
    private final Integer minLength;
    private final Integer maxLength;
    private final Number minValue;
    private final Number maxValue;
    private final String pattern;
    private final java.util.Set<Object> allowedValues;
    
    private FieldConstraints(Builder builder) {
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
        this.minValue = builder.minValue;
        this.maxValue = builder.maxValue;
        this.pattern = builder.pattern;
        this.allowedValues = builder.allowedValues != null ? 
            java.util.Set.copyOf(builder.allowedValues) : null;
    }
    
    public Integer getMinLength() { return minLength; }
    public Integer getMaxLength() { return maxLength; }
    public Number getMinValue() { return minValue; }
    public Number getMaxValue() { return maxValue; }
    public String getPattern() { return pattern; }
    public java.util.Set<Object> getAllowedValues() { return allowedValues; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Integer minLength;
        private Integer maxLength;
        private Number minValue;
        private Number maxValue;
        private String pattern;
        private java.util.Set<Object> allowedValues;
        
        public Builder minLength(Integer minLength) {
            this.minLength = minLength;
            return this;
        }
        
        public Builder maxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }
        
        public Builder minValue(Number minValue) {
            this.minValue = minValue;
            return this;
        }
        
        public Builder maxValue(Number maxValue) {
            this.maxValue = maxValue;
            return this;
        }
        
        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }
        
        public Builder allowedValues(java.util.Set<Object> allowedValues) {
            this.allowedValues = new java.util.HashSet<>(allowedValues);
            return this;
        }
        
        public FieldConstraints build() {
            return new FieldConstraints(this);
        }
    }
}
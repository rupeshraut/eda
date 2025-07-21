package eda.eventbus.schema;

import java.util.List;

/**
 * Result of schema validation
 */
public class ValidationResult {
    private final boolean valid;
    private final List<ValidationError> errors;
    
    private ValidationResult(boolean valid, List<ValidationError> errors) {
        this.valid = valid;
        this.errors = List.copyOf(errors);
    }
    
    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }
    
    public static ValidationResult failure(List<ValidationError> errors) {
        return new ValidationResult(false, errors);
    }
    
    public static ValidationResult failure(ValidationError error) {
        return new ValidationResult(false, List.of(error));
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<ValidationError> getErrors() {
        return errors;
    }
    
    @Override
    public String toString() {
        if (valid) {
            return "ValidationResult{valid=true}";
        } else {
            return "ValidationResult{valid=false, errors=" + errors + "}";
        }
    }
    
    /**
     * Validation error details
     */
    public static class ValidationError {
        private final String field;
        private final String message;
        private final ErrorType type;
        
        public ValidationError(String field, String message, ErrorType type) {
            this.field = field;
            this.message = message;
            this.type = type;
        }
        
        public String getField() { return field; }
        public String getMessage() { return message; }
        public ErrorType getType() { return type; }
        
        @Override
        public String toString() {
            return "ValidationError{" +
                    "field='" + field + '\'' +
                    ", message='" + message + '\'' +
                    ", type=" + type +
                    '}';
        }
    }
    
    public enum ErrorType {
        MISSING_REQUIRED_FIELD,
        INVALID_TYPE,
        INVALID_VALUE,
        UNKNOWN_FIELD,
        SCHEMA_MISMATCH
    }
}
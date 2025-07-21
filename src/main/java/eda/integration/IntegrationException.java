package eda.integration;

public class IntegrationException extends Exception {
    
    public IntegrationException(String message) {
        super(message);
    }
    
    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public IntegrationException(Throwable cause) {
        super(cause);
    }
}
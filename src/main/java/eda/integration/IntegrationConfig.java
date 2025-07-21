package eda.integration;// ================ Configuration Classes ================

import java.util.HashMap;
import java.util.Map;

class IntegrationConfig {
    private final String endpoint;
    private final Map<String, String> properties;
    
    public IntegrationConfig(String endpoint) {
        this.endpoint = endpoint;
        this.properties = new HashMap<>();
    }
    
    public String getEndpoint() { return endpoint; }
    public Map<String, String> getProperties() { return properties; }
    
    public IntegrationConfig withProperty(String key, String value) {
        this.properties.put(key, value);
        return this;
    }
}
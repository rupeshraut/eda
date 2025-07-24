package eda.eventbus.kafka.monitoring;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Overall health status of Kafka system
 */
public class KafkaHealthStatus {
    private final HealthLevel overallHealth;
    private final Map<String, ComponentHealth> componentHealth;
    private final List<String> healthIssues;
    private final Instant timestamp;
    private final KafkaMetricsSnapshot snapshot;
    
    public KafkaHealthStatus(HealthLevel overallHealth, 
                           Map<String, ComponentHealth> componentHealth,
                           List<String> healthIssues,
                           KafkaMetricsSnapshot snapshot) {
        this.overallHealth = overallHealth;
        this.componentHealth = new ConcurrentHashMap<>(componentHealth);
        this.healthIssues = List.copyOf(healthIssues);
        this.snapshot = snapshot;
        this.timestamp = Instant.now();
    }
    
    /**
     * Get overall health level
     */
    public HealthLevel getOverallHealth() {
        return overallHealth;
    }
    
    /**
     * Get component health status
     */
    public Map<String, ComponentHealth> getComponentHealth() {
        return componentHealth;
    }
    
    /**
     * Get health issues
     */
    public List<String> getHealthIssues() {
        return healthIssues;
    }
    
    /**
     * Get health check timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get associated metrics snapshot
     */
    public KafkaMetricsSnapshot getSnapshot() {
        return snapshot;
    }
    
    /**
     * Check if system is healthy
     */
    public boolean isHealthy() {
        return overallHealth == HealthLevel.HEALTHY;
    }
    
    /**
     * Check if system is degraded
     */
    public boolean isDegraded() {
        return overallHealth == HealthLevel.DEGRADED;
    }
    
    /**
     * Check if system is unhealthy
     */
    public boolean isUnhealthy() {
        return overallHealth == HealthLevel.UNHEALTHY;
    }
    
    /**
     * Get health level for a specific component
     */
    public ComponentHealth getComponentHealth(String component) {
        return componentHealth.get(component);
    }
    
    /**
     * Check if a component is healthy
     */
    public boolean isComponentHealthy(String component) {
        ComponentHealth health = getComponentHealth(component);
        return health != null && health.getLevel() == HealthLevel.HEALTHY;
    }
    
    /**
     * Get number of healthy components
     */
    public long getHealthyComponentCount() {
        return componentHealth.values().stream()
            .mapToLong(health -> health.getLevel() == HealthLevel.HEALTHY ? 1 : 0)
            .sum();
    }
    
    /**
     * Get number of degraded components
     */
    public long getDegradedComponentCount() {
        return componentHealth.values().stream()
            .mapToLong(health -> health.getLevel() == HealthLevel.DEGRADED ? 1 : 0)
            .sum();
    }
    
    /**
     * Get number of unhealthy components
     */
    public long getUnhealthyComponentCount() {
        return componentHealth.values().stream()
            .mapToLong(health -> health.getLevel() == HealthLevel.UNHEALTHY ? 1 : 0)
            .sum();
    }
    
    /**
     * Get total component count
     */
    public int getTotalComponentCount() {
        return componentHealth.size();
    }
    
    /**
     * Get health score (0.0 to 1.0)
     */
    public double getHealthScore() {
        if (componentHealth.isEmpty()) {
            return 1.0;
        }
        
        double totalScore = componentHealth.values().stream()
            .mapToDouble(health -> health.getLevel().getScore())
            .sum();
            
        return totalScore / componentHealth.size();
    }
    
    @Override
    public String toString() {
        return String.format(
            "KafkaHealthStatus{overall=%s, components=%d, healthy=%d, degraded=%d, unhealthy=%d, score=%.2f, issues=%d}",
            overallHealth, getTotalComponentCount(), getHealthyComponentCount(), 
            getDegradedComponentCount(), getUnhealthyComponentCount(), getHealthScore(), healthIssues.size()
        );
    }
    
    /**
     * Health level enumeration
     */
    public enum HealthLevel {
        HEALTHY(1.0, "System is operating normally"),
        DEGRADED(0.6, "System is experiencing some issues but still functional"),
        UNHEALTHY(0.2, "System has critical issues affecting functionality");
        
        private final double score;
        private final String description;
        
        HealthLevel(double score, String description) {
            this.score = score;
            this.description = description;
        }
        
        public double getScore() {
            return score;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isHealthy() {
            return this == HEALTHY;
        }
        
        public boolean isDegraded() {
            return this == DEGRADED;
        }
        
        public boolean isUnhealthy() {
            return this == UNHEALTHY;
        }
    }
    
    /**
     * Health status for individual components
     */
    public static class ComponentHealth {
        private final String componentName;
        private final HealthLevel level;
        private final String status;
        private final List<String> issues;
        private final Map<String, Object> metrics;
        private final Instant lastChecked;
        
        public ComponentHealth(String componentName, HealthLevel level, 
                             String status, List<String> issues, Map<String, Object> metrics) {
            this.componentName = componentName;
            this.level = level;
            this.status = status;
            this.issues = List.copyOf(issues);
            this.metrics = Map.copyOf(metrics);
            this.lastChecked = Instant.now();
        }
        
        public String getComponentName() { return componentName; }
        public HealthLevel getLevel() { return level; }
        public String getStatus() { return status; }
        public List<String> getIssues() { return issues; }
        public Map<String, Object> getMetrics() { return metrics; }
        public Instant getLastChecked() { return lastChecked; }
        
        public boolean isHealthy() { return level.isHealthy(); }
        public boolean isDegraded() { return level.isDegraded(); }
        public boolean isUnhealthy() { return level.isUnhealthy(); }
        
        public boolean hasIssues() { return !issues.isEmpty(); }
        public int getIssueCount() { return issues.size(); }
        
        @Override
        public String toString() {
            return String.format(
                "ComponentHealth{name='%s', level=%s, status='%s', issues=%d}",
                componentName, level, status, issues.size()
            );
        }
    }
}
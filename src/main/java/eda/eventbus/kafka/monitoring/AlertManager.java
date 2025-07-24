package eda.eventbus.kafka.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages alerts and notifications for Kafka monitoring
 */
public class AlertManager {
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    private final List<AlertHandler> alertHandlers = new CopyOnWriteArrayList<>();
    private final Map<String, Alert> activeAlerts = new ConcurrentHashMap<>();
    private final List<Alert> alertHistory = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    private final int maxHistorySize;
    private final Duration alertCooldown;
    
    public AlertManager() {
        this(1000, Duration.ofMinutes(5));
    }
    
    public AlertManager(int maxHistorySize, Duration alertCooldown) {
        this.maxHistorySize = maxHistorySize;
        this.alertCooldown = alertCooldown;
        
        // Start alert cleanup task
        scheduler.scheduleAtFixedRate(this::cleanupAlerts, 1, 1, TimeUnit.MINUTES);
    }
    
    /**
     * Add an alert rule
     */
    public void addAlertRule(AlertRule rule) {
        alertRules.put(rule.getName(), rule);
    }
    
    /**
     * Remove an alert rule
     */
    public void removeAlertRule(String ruleName) {
        alertRules.remove(ruleName);
    }
    
    /**
     * Add an alert handler
     */
    public void addAlertHandler(AlertHandler handler) {
        alertHandlers.add(handler);
    }
    
    /**
     * Remove an alert handler
     */
    public void removeAlertHandler(AlertHandler handler) {
        alertHandlers.remove(handler);
    }
    
    /**
     * Process consumer lag alert
     */
    public void processConsumerLagAlert(AlertTypes.ConsumerLagAlert lagAlert) {
        String alertKey = String.format("consumer-lag-%s-%s-%d", 
            lagAlert.getConsumerGroup(), lagAlert.getTopic(), lagAlert.getPartition());
            
        if (shouldTriggerAlert(alertKey, lagAlert.getSeverity())) {
            Alert alert = new Alert(
                alertKey,
                AlertType.CONSUMER_LAG,
                lagAlert.getSeverity(),
                String.format("High consumer lag detected: %s on %s:%d (lag: %s)",
                    lagAlert.getConsumerGroup(), lagAlert.getTopic(), 
                    lagAlert.getPartition(), lagAlert.getLag()),
                Map.of(
                    "consumerGroup", lagAlert.getConsumerGroup(),
                    "topic", lagAlert.getTopic(),
                    "partition", lagAlert.getPartition(),
                    "lag", lagAlert.getLag().toString()
                ),
                lagAlert.getTimestamp()
            );
            
            triggerAlert(alert);
        }
    }
    
    /**
     * Process throughput alert
     */
    public void processThroughputAlert(AlertTypes.ThroughputAlert throughputAlert) {
        String alertKey = String.format("throughput-%s", throughputAlert.getIdentifier());
        
        if (shouldTriggerAlert(alertKey, throughputAlert.getSeverity())) {
            Alert alert = new Alert(
                alertKey,
                AlertType.THROUGHPUT_CHANGE,
                throughputAlert.getSeverity(),
                String.format("Throughput change detected for %s: %.2f -> %.2f (%.1f%% change)",
                    throughputAlert.getIdentifier(), throughputAlert.getPreviousThroughput(),
                    throughputAlert.getCurrentThroughput(), throughputAlert.getChangePercent()),
                Map.of(
                    "identifier", throughputAlert.getIdentifier(),
                    "currentThroughput", throughputAlert.getCurrentThroughput(),
                    "previousThroughput", throughputAlert.getPreviousThroughput(),
                    "changePercent", throughputAlert.getChangePercent()
                ),
                throughputAlert.getTimestamp()
            );
            
            triggerAlert(alert);
        }
    }
    
    /**
     * Process error rate alert
     */
    public void processErrorRateAlert(AlertTypes.ErrorRateAlert errorAlert) {
        String alertKey = String.format("error-rate-%s-%s", 
            errorAlert.getIdentifier(), errorAlert.getErrorType());
            
        if (shouldTriggerAlert(alertKey, errorAlert.getSeverity())) {
            Alert alert = new Alert(
                alertKey,
                AlertType.ERROR_RATE,
                errorAlert.getSeverity(),
                String.format("High error rate detected for %s: %.2f%% (%d errors out of %d messages)",
                    errorAlert.getIdentifier(), errorAlert.getErrorRate() * 100,
                    errorAlert.getTotalErrors(), errorAlert.getTotalMessages()),
                Map.of(
                    "identifier", errorAlert.getIdentifier(),
                    "errorType", errorAlert.getErrorType(),
                    "errorRate", errorAlert.getErrorRate(),
                    "totalErrors", errorAlert.getTotalErrors(),
                    "totalMessages", errorAlert.getTotalMessages()
                ),
                errorAlert.getTimestamp()
            );
            
            triggerAlert(alert);
        }
    }
    
    /**
     * Process connection alert
     */
    public void processConnectionAlert(AlertTypes.ConnectionAlert connectionAlert) {
        String alertKey = String.format("connection-%s", connectionAlert.getBrokerId());
        
        if (shouldTriggerAlert(alertKey, connectionAlert.getSeverity())) {
            Alert alert = new Alert(
                alertKey,
                AlertType.CONNECTION,
                connectionAlert.getSeverity(),
                String.format("Connection issue with broker %s: %s (disconnected for %s)",
                    connectionAlert.getBrokerId(), connectionAlert.getConnectionStatus(),
                    connectionAlert.getDisconnectedDuration()),
                Map.of(
                    "brokerId", connectionAlert.getBrokerId(),
                    "status", connectionAlert.getConnectionStatus(),
                    "duration", connectionAlert.getDisconnectedDuration().toString(),
                    "reason", connectionAlert.getReason()
                ),
                connectionAlert.getTimestamp()
            );
            
            triggerAlert(alert);
        }
    }
    
    /**
     * Process rebalance alert
     */
    public void processRebalanceAlert(AlertTypes.RebalanceAlert rebalanceAlert) {
        String alertKey = String.format("rebalance-%s", rebalanceAlert.getConsumerGroup());
        
        if (shouldTriggerAlert(alertKey, rebalanceAlert.getSeverity())) {
            Alert alert = new Alert(
                alertKey,
                AlertType.REBALANCE,
                rebalanceAlert.getSeverity(),
                String.format("High rebalance frequency for consumer group %s: %.1f rebalances/hour (%d rebalances)",
                    rebalanceAlert.getConsumerGroup(), rebalanceAlert.getFrequency(),
                    rebalanceAlert.getRebalanceCount()),
                Map.of(
                    "consumerGroup", rebalanceAlert.getConsumerGroup(),
                    "count", rebalanceAlert.getRebalanceCount(),
                    "frequency", rebalanceAlert.getFrequency(),
                    "averageTime", rebalanceAlert.getAverageRebalanceTime().toString()
                ),
                rebalanceAlert.getTimestamp()
            );
            
            triggerAlert(alert);
        }
    }
    
    /**
     * Clear alert if condition is resolved
     */
    public void clearAlert(String alertKey) {
        Alert alert = activeAlerts.remove(alertKey);
        if (alert != null) {
            alert.resolve();
            addToHistory(alert);
            
            // Notify handlers of resolution
            for (AlertHandler handler : alertHandlers) {
                try {
                    handler.onAlertResolved(alert);
                } catch (Exception e) {
                    // Log error but continue
                    System.err.println("Error in alert handler during resolution: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Get active alerts
     */
    public Map<String, Alert> getActiveAlerts() {
        return new HashMap<>(activeAlerts);
    }
    
    /**
     * Get active alerts by severity
     */
    public List<Alert> getActiveAlertsBySeverity(AlertTypes.AlertSeverity severity) {
        return activeAlerts.values().stream()
            .filter(alert -> alert.getSeverity() == severity)
            .sorted((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()))
            .toList();
    }
    
    /**
     * Get alert history
     */
    public List<Alert> getAlertHistory() {
        return new ArrayList<>(alertHistory);
    }
    
    /**
     * Get alert statistics
     */
    public AlertStatistics getStatistics() {
        return new AlertStatistics(
            activeAlerts.size(),
            alertHistory.size(),
            getActiveAlertsBySeverity(AlertTypes.AlertSeverity.CRITICAL).size(),
            getActiveAlertsBySeverity(AlertTypes.AlertSeverity.HIGH).size(),
            getActiveAlertsBySeverity(AlertTypes.AlertSeverity.MEDIUM).size(),
            getActiveAlertsBySeverity(AlertTypes.AlertSeverity.LOW).size()
        );
    }
    
    /**
     * Shutdown the alert manager
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private boolean shouldTriggerAlert(String alertKey, AlertTypes.AlertSeverity severity) {
        Alert existingAlert = activeAlerts.get(alertKey);
        
        if (existingAlert == null) {
            return true; // No existing alert, trigger new one
        }
        
        // Check cooldown period
        Duration timeSinceLastTrigger = Duration.between(existingAlert.getTimestamp(), Instant.now());
        if (timeSinceLastTrigger.compareTo(alertCooldown) < 0) {
            return false; // Still in cooldown
        }
        
        // Trigger if severity increased
        return severity.isHigherThan(existingAlert.getSeverity());
    }
    
    private void triggerAlert(Alert alert) {
        activeAlerts.put(alert.getKey(), alert);
        addToHistory(alert);
        
        // Notify all handlers
        for (AlertHandler handler : alertHandlers) {
            try {
                handler.onAlert(alert);
            } catch (Exception e) {
                // Log error but continue
                System.err.println("Error in alert handler: " + e.getMessage());
            }
        }
    }
    
    private void addToHistory(Alert alert) {
        alertHistory.add(alert);
        
        // Trim history if needed
        while (alertHistory.size() > maxHistorySize) {
            alertHistory.remove(0);
        }
    }
    
    private void cleanupAlerts() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));
        
        // Remove old resolved alerts from active list
        activeAlerts.entrySet().removeIf(entry -> {
            Alert alert = entry.getValue();
            return alert.isResolved() && alert.getResolvedAt() != null && 
                   alert.getResolvedAt().isBefore(cutoff);
        });
    }
    
    /**
     * Alert types
     */
    public enum AlertType {
        CONSUMER_LAG,
        THROUGHPUT_CHANGE,
        ERROR_RATE,
        CONNECTION,
        REBALANCE,
        SYSTEM_HEALTH
    }
    
    /**
     * Alert data structure
     */
    public static class Alert {
        private final String key;
        private final AlertType type;
        private final AlertTypes.AlertSeverity severity;
        private final String message;
        private final Map<String, Object> metadata;
        private final Instant timestamp;
        private volatile boolean resolved = false;
        private volatile Instant resolvedAt;
        
        public Alert(String key, AlertType type, AlertTypes.AlertSeverity severity,
                    String message, Map<String, Object> metadata, Instant timestamp) {
            this.key = key;
            this.type = type;
            this.severity = severity;
            this.message = message;
            this.metadata = Map.copyOf(metadata);
            this.timestamp = timestamp;
        }
        
        public String getKey() { return key; }
        public AlertType getType() { return type; }
        public AlertTypes.AlertSeverity getSeverity() { return severity; }
        public String getMessage() { return message; }
        public Map<String, Object> getMetadata() { return metadata; }
        public Instant getTimestamp() { return timestamp; }
        public boolean isResolved() { return resolved; }
        public Instant getResolvedAt() { return resolvedAt; }
        
        public void resolve() {
            this.resolved = true;
            this.resolvedAt = Instant.now();
        }
        
        public Duration getDuration() {
            Instant end = resolved ? resolvedAt : Instant.now();
            return Duration.between(timestamp, end);
        }
        
        @Override
        public String toString() {
            return String.format(
                "Alert{key='%s', type=%s, severity=%s, message='%s', resolved=%s}",
                key, type, severity, message, resolved
            );
        }
    }
    
    /**
     * Alert rule definition
     */
    public static class AlertRule {
        private final String name;
        private final AlertType type;
        private final AlertTypes.AlertSeverity threshold;
        private final Duration evaluationInterval;
        private final Consumer<Alert> action;
        
        public AlertRule(String name, AlertType type, AlertTypes.AlertSeverity threshold,
                        Duration evaluationInterval, Consumer<Alert> action) {
            this.name = name;
            this.type = type;
            this.threshold = threshold;
            this.evaluationInterval = evaluationInterval;
            this.action = action;
        }
        
        public String getName() { return name; }
        public AlertType getType() { return type; }
        public AlertTypes.AlertSeverity getThreshold() { return threshold; }
        public Duration getEvaluationInterval() { return evaluationInterval; }
        public Consumer<Alert> getAction() { return action; }
    }
    
    /**
     * Alert handler interface
     */
    public interface AlertHandler {
        void onAlert(Alert alert);
        void onAlertResolved(Alert alert);
    }
    
    /**
     * Alert statistics
     */
    public static class AlertStatistics {
        private final int activeCount;
        private final int historyCount;
        private final int criticalCount;
        private final int highCount;
        private final int mediumCount;
        private final int lowCount;
        
        public AlertStatistics(int activeCount, int historyCount, int criticalCount,
                             int highCount, int mediumCount, int lowCount) {
            this.activeCount = activeCount;
            this.historyCount = historyCount;
            this.criticalCount = criticalCount;
            this.highCount = highCount;
            this.mediumCount = mediumCount;
            this.lowCount = lowCount;
        }
        
        public int getActiveCount() { return activeCount; }
        public int getHistoryCount() { return historyCount; }
        public int getCriticalCount() { return criticalCount; }
        public int getHighCount() { return highCount; }
        public int getMediumCount() { return mediumCount; }
        public int getLowCount() { return lowCount; }
        
        public int getTotalSevereAlerts() { return criticalCount + highCount; }
        
        @Override
        public String toString() {
            return String.format(
                "AlertStatistics{active=%d, history=%d, critical=%d, high=%d, medium=%d, low=%d}",
                activeCount, historyCount, criticalCount, highCount, mediumCount, lowCount
            );
        }
    }
}
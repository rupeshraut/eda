package eda.eventbus.kafka.monitoring;

import java.time.Duration;
import java.time.Instant;

/**
 * Alert types for Kafka monitoring
 */
public class AlertTypes {
    
    /**
     * Alert triggered when consumer lag exceeds threshold
     */
    public static class ConsumerLagAlert {
        private final String consumerGroup;
        private final String topic;
        private final int partition;
        private final Duration lag;
        private final Instant timestamp;
        private final AlertSeverity severity;
        
        public ConsumerLagAlert(String consumerGroup, String topic, int partition, 
                              Duration lag, Instant timestamp) {
            this.consumerGroup = consumerGroup;
            this.topic = topic;
            this.partition = partition;
            this.lag = lag;
            this.timestamp = timestamp;
            this.severity = determineSeverity(lag);
        }
        
        public String getConsumerGroup() { return consumerGroup; }
        public String getTopic() { return topic; }
        public int getPartition() { return partition; }
        public Duration getLag() { return lag; }
        public Instant getTimestamp() { return timestamp; }
        public AlertSeverity getSeverity() { return severity; }
        
        private AlertSeverity determineSeverity(Duration lag) {
            if (lag.compareTo(Duration.ofMinutes(10)) > 0) {
                return AlertSeverity.CRITICAL;
            } else if (lag.compareTo(Duration.ofMinutes(5)) > 0) {
                return AlertSeverity.HIGH;
            } else if (lag.compareTo(Duration.ofMinutes(1)) > 0) {
                return AlertSeverity.MEDIUM;
            }
            return AlertSeverity.LOW;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ConsumerLagAlert{group='%s', topic='%s', partition=%d, lag=%s, severity=%s}",
                consumerGroup, topic, partition, lag, severity
            );
        }
    }
    
    /**
     * Alert triggered when throughput changes significantly
     */
    public static class ThroughputAlert {
        private final String identifier;
        private final double currentThroughput;
        private final double previousThroughput;
        private final double changePercent;
        private final Instant timestamp;
        private final AlertSeverity severity;
        
        public ThroughputAlert(String identifier, double currentThroughput, 
                             double previousThroughput, Instant timestamp) {
            this.identifier = identifier;
            this.currentThroughput = currentThroughput;
            this.previousThroughput = previousThroughput;
            this.changePercent = calculateChangePercent(currentThroughput, previousThroughput);
            this.timestamp = timestamp;
            this.severity = determineSeverity(Math.abs(changePercent));
        }
        
        public String getIdentifier() { return identifier; }
        public double getCurrentThroughput() { return currentThroughput; }
        public double getPreviousThroughput() { return previousThroughput; }
        public double getChangePercent() { return changePercent; }
        public Instant getTimestamp() { return timestamp; }
        public AlertSeverity getSeverity() { return severity; }
        
        public boolean isIncrease() { return changePercent > 0; }
        public boolean isDecrease() { return changePercent < 0; }
        
        private double calculateChangePercent(double current, double previous) {
            if (previous == 0) return current > 0 ? 100.0 : 0.0;
            return ((current - previous) / previous) * 100.0;
        }
        
        private AlertSeverity determineSeverity(double changePercent) {
            if (changePercent > 80) {
                return AlertSeverity.CRITICAL;
            } else if (changePercent > 50) {
                return AlertSeverity.HIGH;
            } else if (changePercent > 25) {
                return AlertSeverity.MEDIUM;
            }
            return AlertSeverity.LOW;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ThroughputAlert{id='%s', current=%.2f, previous=%.2f, change=%.1f%%, severity=%s}",
                identifier, currentThroughput, previousThroughput, changePercent, severity
            );
        }
    }
    
    /**
     * Alert triggered when error rate exceeds threshold
     */
    public static class ErrorRateAlert {
        private final String identifier;
        private final String errorType;
        private final double errorRate;
        private final long totalErrors;
        private final long totalMessages;
        private final Instant timestamp;
        private final AlertSeverity severity;
        private final Throwable lastError;
        
        public ErrorRateAlert(String identifier, String errorType, double errorRate,
                            long totalErrors, long totalMessages, Throwable lastError, Instant timestamp) {
            this.identifier = identifier;
            this.errorType = errorType;
            this.errorRate = errorRate;
            this.totalErrors = totalErrors;
            this.totalMessages = totalMessages;
            this.lastError = lastError;
            this.timestamp = timestamp;
            this.severity = determineSeverity(errorRate);
        }
        
        public String getIdentifier() { return identifier; }
        public String getErrorType() { return errorType; }
        public double getErrorRate() { return errorRate; }
        public long getTotalErrors() { return totalErrors; }
        public long getTotalMessages() { return totalMessages; }
        public Throwable getLastError() { return lastError; }
        public Instant getTimestamp() { return timestamp; }
        public AlertSeverity getSeverity() { return severity; }
        
        private AlertSeverity determineSeverity(double errorRate) {
            if (errorRate > 0.20) { // 20%
                return AlertSeverity.CRITICAL;
            } else if (errorRate > 0.10) { // 10%
                return AlertSeverity.HIGH;
            } else if (errorRate > 0.05) { // 5%
                return AlertSeverity.MEDIUM;
            }
            return AlertSeverity.LOW;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ErrorRateAlert{id='%s', type='%s', rate=%.2f%%, errors=%d, total=%d, severity=%s}",
                identifier, errorType, errorRate * 100, totalErrors, totalMessages, severity
            );
        }
    }
    
    /**
     * Alert triggered when connection issues are detected
     */
    public static class ConnectionAlert {
        private final String brokerId;
        private final String connectionStatus;
        private final Duration disconnectedDuration;
        private final Instant timestamp;
        private final AlertSeverity severity;
        private final String reason;
        
        public ConnectionAlert(String brokerId, String connectionStatus, 
                             Duration disconnectedDuration, String reason, Instant timestamp) {
            this.brokerId = brokerId;
            this.connectionStatus = connectionStatus;
            this.disconnectedDuration = disconnectedDuration;
            this.reason = reason;
            this.timestamp = timestamp;
            this.severity = determineSeverity(disconnectedDuration);
        }
        
        public String getBrokerId() { return brokerId; }
        public String getConnectionStatus() { return connectionStatus; }
        public Duration getDisconnectedDuration() { return disconnectedDuration; }
        public String getReason() { return reason; }
        public Instant getTimestamp() { return timestamp; }
        public AlertSeverity getSeverity() { return severity; }
        
        public boolean isConnected() { return "CONNECTED".equals(connectionStatus); }
        public boolean isDisconnected() { return "DISCONNECTED".equals(connectionStatus); }
        
        private AlertSeverity determineSeverity(Duration disconnectedDuration) {
            if (disconnectedDuration.compareTo(Duration.ofMinutes(10)) > 0) {
                return AlertSeverity.CRITICAL;
            } else if (disconnectedDuration.compareTo(Duration.ofMinutes(5)) > 0) {
                return AlertSeverity.HIGH;
            } else if (disconnectedDuration.compareTo(Duration.ofMinutes(1)) > 0) {
                return AlertSeverity.MEDIUM;
            }
            return AlertSeverity.LOW;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ConnectionAlert{broker='%s', status='%s', duration=%s, severity=%s, reason='%s'}",
                brokerId, connectionStatus, disconnectedDuration, severity, reason
            );
        }
    }
    
    /**
     * Alert triggered when rebalance frequency is too high
     */
    public static class RebalanceAlert {
        private final String consumerGroup;
        private final long rebalanceCount;
        private final Duration timeWindow;
        private final double frequency;
        private final Duration averageRebalanceTime;
        private final Instant timestamp;
        private final AlertSeverity severity;
        
        public RebalanceAlert(String consumerGroup, long rebalanceCount, Duration timeWindow,
                            Duration averageRebalanceTime, Instant timestamp) {
            this.consumerGroup = consumerGroup;
            this.rebalanceCount = rebalanceCount;
            this.timeWindow = timeWindow;
            this.frequency = calculateFrequency(rebalanceCount, timeWindow);
            this.averageRebalanceTime = averageRebalanceTime;
            this.timestamp = timestamp;
            this.severity = determineSeverity(frequency);
        }
        
        public String getConsumerGroup() { return consumerGroup; }
        public long getRebalanceCount() { return rebalanceCount; }
        public Duration getTimeWindow() { return timeWindow; }
        public double getFrequency() { return frequency; }
        public Duration getAverageRebalanceTime() { return averageRebalanceTime; }
        public Instant getTimestamp() { return timestamp; }
        public AlertSeverity getSeverity() { return severity; }
        
        private double calculateFrequency(long count, Duration window) {
            if (window.isZero()) return 0.0;
            return (double) count / window.toHours();
        }
        
        private AlertSeverity determineSeverity(double frequency) {
            if (frequency > 10) { // More than 10 rebalances per hour
                return AlertSeverity.CRITICAL;
            } else if (frequency > 5) { // More than 5 rebalances per hour
                return AlertSeverity.HIGH;
            } else if (frequency > 2) { // More than 2 rebalances per hour
                return AlertSeverity.MEDIUM;
            }
            return AlertSeverity.LOW;
        }
        
        @Override
        public String toString() {
            return String.format(
                "RebalanceAlert{group='%s', count=%d, frequency=%.1f/hour, avgTime=%s, severity=%s}",
                consumerGroup, rebalanceCount, frequency, averageRebalanceTime, severity
            );
        }
    }
    
    /**
     * Alert severity levels
     */
    public enum AlertSeverity {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4);
        
        private final int level;
        
        AlertSeverity(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
        
        public boolean isHigherThan(AlertSeverity other) {
            return this.level > other.level;
        }
        
        public boolean isAtLeast(AlertSeverity other) {
            return this.level >= other.level;
        }
    }
}
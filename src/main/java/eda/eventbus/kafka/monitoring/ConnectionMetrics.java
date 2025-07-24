package eda.eventbus.kafka.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics for Kafka broker connections
 */
public class ConnectionMetrics {
    private final ConcurrentHashMap<String, BrokerConnectionInfo> brokerConnections = new ConcurrentHashMap<>();
    private final AtomicLong totalConnectionAttempts = new AtomicLong();
    private final AtomicLong failedConnectionAttempts = new AtomicLong();
    private final AtomicLong totalDisconnections = new AtomicLong();
    private final Instant createdAt;
    
    public ConnectionMetrics() {
        this.createdAt = Instant.now();
    }
    
    /**
     * Record connection status for a broker
     */
    public void recordConnectionStatus(String brokerId, boolean connected) {
        BrokerConnectionInfo info = brokerConnections.computeIfAbsent(
            brokerId, k -> new BrokerConnectionInfo(brokerId));
        
        if (connected) {
            info.recordConnection();
        } else {
            info.recordDisconnection();
        }
    }
    
    /**
     * Record connection attempt
     */
    public void recordConnectionAttempt(String brokerId, boolean successful) {
        totalConnectionAttempts.incrementAndGet();
        
        if (!successful) {
            failedConnectionAttempts.incrementAndGet();
        }
        
        BrokerConnectionInfo info = brokerConnections.computeIfAbsent(
            brokerId, k -> new BrokerConnectionInfo(brokerId));
        info.recordConnectionAttempt(successful);
    }
    
    /**
     * Record disconnection
     */
    public void recordDisconnection(String brokerId, String reason) {
        totalDisconnections.incrementAndGet();
        
        BrokerConnectionInfo info = brokerConnections.computeIfAbsent(
            brokerId, k -> new BrokerConnectionInfo(brokerId));
        info.recordDisconnection(reason);
    }
    
    /**
     * Get connection info for a specific broker
     */
    public BrokerConnectionInfo getBrokerInfo(String brokerId) {
        return brokerConnections.get(brokerId);
    }
    
    /**
     * Get all broker connection information
     */
    public ConcurrentHashMap<String, BrokerConnectionInfo> getAllBrokerInfo() {
        return new ConcurrentHashMap<>(brokerConnections);
    }
    
    /**
     * Get total connection attempts
     */
    public long getTotalConnectionAttempts() {
        return totalConnectionAttempts.get();
    }
    
    /**
     * Get failed connection attempts
     */
    public long getFailedConnectionAttempts() {
        return failedConnectionAttempts.get();
    }
    
    /**
     * Get connection success rate
     */
    public double getConnectionSuccessRate() {
        long total = getTotalConnectionAttempts();
        return total > 0 ? 1.0 - ((double) getFailedConnectionAttempts() / total) : 1.0;
    }
    
    /**
     * Get total disconnections
     */
    public long getTotalDisconnections() {
        return totalDisconnections.get();
    }
    
    /**
     * Get number of connected brokers
     */
    public long getConnectedBrokerCount() {
        return brokerConnections.values().stream()
            .mapToLong(info -> info.isConnected() ? 1 : 0)
            .sum();
    }
    
    /**
     * Get total number of brokers
     */
    public int getTotalBrokerCount() {
        return brokerConnections.size();
    }
    
    /**
     * Check if all brokers are connected
     */
    public boolean areAllBrokersConnected() {
        return !brokerConnections.isEmpty() && 
               brokerConnections.values().stream().allMatch(BrokerConnectionInfo::isConnected);
    }
    
    /**
     * Check if connection health is good
     */
    public boolean isHealthy() {
        return getConnectionSuccessRate() > 0.95 && 
               getConnectedBrokerCount() > 0;
    }
    
    /**
     * Get broker with most disconnections
     */
    public String getMostUnstableBroker() {
        return brokerConnections.entrySet().stream()
            .max((e1, e2) -> Long.compare(
                e1.getValue().getDisconnectionCount(),
                e2.getValue().getDisconnectionCount()))
            .map(entry -> entry.getKey())
            .orElse(null);
    }
    
    /**
     * Get snapshot of current metrics
     */
    public ConnectionMetrics getSnapshot() {
        ConnectionMetrics snapshot = new ConnectionMetrics();
        snapshot.totalConnectionAttempts.set(this.totalConnectionAttempts.get());
        snapshot.failedConnectionAttempts.set(this.failedConnectionAttempts.get());
        snapshot.totalDisconnections.set(this.totalDisconnections.get());
        
        // Copy broker connection info
        this.brokerConnections.forEach((brokerId, info) -> 
            snapshot.brokerConnections.put(brokerId, info.copy()));
        
        return snapshot;
    }
    
    @Override
    public String toString() {
        return String.format(
            "ConnectionMetrics{brokers=%d, connected=%d, attempts=%d, successRate=%.2f%%, healthy=%s}",
            getTotalBrokerCount(), getConnectedBrokerCount(), getTotalConnectionAttempts(),
            getConnectionSuccessRate() * 100, isHealthy()
        );
    }
    
    /**
     * Information about connection to a specific broker
     */
    public static class BrokerConnectionInfo {
        private final String brokerId;
        private final AtomicLong connectionAttempts = new AtomicLong();
        private final AtomicLong failedAttempts = new AtomicLong();
        private final AtomicLong disconnectionCount = new AtomicLong();
        private final AtomicLong lastConnectionTime = new AtomicLong();
        private final AtomicLong lastDisconnectionTime = new AtomicLong();
        private volatile boolean connected = false;
        private volatile String lastDisconnectionReason = "";
        
        public BrokerConnectionInfo(String brokerId) {
            this.brokerId = brokerId;
        }
        
        public void recordConnection() {
            connected = true;
            lastConnectionTime.set(System.currentTimeMillis());
        }
        
        public void recordDisconnection() {
            connected = false;
            disconnectionCount.incrementAndGet();
            lastDisconnectionTime.set(System.currentTimeMillis());
        }
        
        public void recordDisconnection(String reason) {
            recordDisconnection();
            this.lastDisconnectionReason = reason;
        }
        
        public void recordConnectionAttempt(boolean successful) {
            connectionAttempts.incrementAndGet();
            if (!successful) {
                failedAttempts.incrementAndGet();
            }
        }
        
        public String getBrokerId() { return brokerId; }
        public boolean isConnected() { return connected; }
        public long getConnectionAttempts() { return connectionAttempts.get(); }
        public long getFailedAttempts() { return failedAttempts.get(); }
        public long getDisconnectionCount() { return disconnectionCount.get(); }
        public String getLastDisconnectionReason() { return lastDisconnectionReason; }
        
        public double getConnectionSuccessRate() {
            long total = getConnectionAttempts();
            return total > 0 ? 1.0 - ((double) getFailedAttempts() / total) : 1.0;
        }
        
        public Instant getLastConnectionTime() {
            long timestamp = lastConnectionTime.get();
            return timestamp > 0 ? Instant.ofEpochMilli(timestamp) : null;
        }
        
        public Instant getLastDisconnectionTime() {
            long timestamp = lastDisconnectionTime.get();
            return timestamp > 0 ? Instant.ofEpochMilli(timestamp) : null;
        }
        
        public Duration getConnectionDuration() {
            if (connected && lastConnectionTime.get() > 0) {
                return Duration.ofMillis(System.currentTimeMillis() - lastConnectionTime.get());
            }
            return Duration.ZERO;
        }
        
        public Duration getDisconnectionDuration() {
            if (!connected && lastDisconnectionTime.get() > 0) {
                return Duration.ofMillis(System.currentTimeMillis() - lastDisconnectionTime.get());
            }
            return Duration.ZERO;
        }
        
        public BrokerConnectionInfo copy() {
            BrokerConnectionInfo copy = new BrokerConnectionInfo(this.brokerId);
            copy.connectionAttempts.set(this.connectionAttempts.get());
            copy.failedAttempts.set(this.failedAttempts.get());
            copy.disconnectionCount.set(this.disconnectionCount.get());
            copy.lastConnectionTime.set(this.lastConnectionTime.get());
            copy.lastDisconnectionTime.set(this.lastDisconnectionTime.get());
            copy.connected = this.connected;
            copy.lastDisconnectionReason = this.lastDisconnectionReason;
            return copy;
        }
        
        @Override
        public String toString() {
            return String.format(
                "BrokerConnectionInfo{id='%s', connected=%s, attempts=%d, disconnections=%d, successRate=%.2f%%}",
                brokerId, connected, getConnectionAttempts(), getDisconnectionCount(),
                getConnectionSuccessRate() * 100
            );
        }
    }
}
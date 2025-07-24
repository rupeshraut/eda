package eda.examples;

import eda.eventbus.EventBusConfig;
import eda.eventbus.core.GenericEvent;
import eda.eventbus.kafka.KafkaEventBusConfig;
import eda.eventbus.kafka.KafkaIntegratedEventBus;
import eda.eventbus.kafka.monitoring.*;
import eda.eventbus.subscription.EventConsumer;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Comprehensive demonstration of Kafka Advanced Monitoring & Observability features
 */
public class KafkaMonitoringDemo {
    private static final Logger LOGGER = Logger.getLogger(KafkaMonitoringDemo.class.getName());

    public static void main(String[] args) {
        new KafkaMonitoringDemo().runDemo();
    }

    public void runDemo() {
        LOGGER.info("=== Kafka Advanced Monitoring & Observability Demo ===");

        // 1. Create event bus with monitoring enabled
        KafkaIntegratedEventBus<String> eventBus = createMonitoredEventBus();

        try {
            // 2. Demonstrate metrics collection
            demonstrateMetricsCollection(eventBus);

            // 3. Demonstrate alert configuration
            demonstrateAlertConfiguration(eventBus);

            // 4. Demonstrate health monitoring
            demonstrateHealthMonitoring(eventBus);

            // 5. Demonstrate metrics export
            demonstrateMetricsExport(eventBus);

            // 6. Demonstrate real-time monitoring
            demonstrateRealTimeMonitoring(eventBus);

            // Wait for monitoring data to accumulate
            LOGGER.info("Waiting for monitoring data to accumulate...");
            Thread.sleep(5000);

            // 7. Show final monitoring results
            showFinalMonitoringResults(eventBus);

        } catch (Exception e) {
            LOGGER.severe("Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            LOGGER.info("Shutting down event bus...");
            eventBus.shutdown();
        }

        LOGGER.info("=== Demo Complete ===");
    }

    private KafkaIntegratedEventBus<String> createMonitoredEventBus() {
        LOGGER.info("1. Creating Kafka Event Bus with Advanced Monitoring...");

        // Create Kafka configuration
        KafkaEventBusConfig kafkaConfig = KafkaEventBusConfig.builder()
            .bootstrapServers("localhost:9092")
            .build();

        // Create event bus configuration
        EventBusConfig eventBusConfig = EventBusConfig.builder()
            .enableMetrics(true)
            .build();

        // Create monitored event bus
        KafkaIntegratedEventBus<String> eventBus = KafkaIntegratedEventBus.<String>builder(String.class)
            .eventBusConfig(eventBusConfig)
            .kafkaConfig(kafkaConfig)
            .publishToKafka(true)
            .consumeFromKafka(true)
            .build();

        LOGGER.info("✓ Event bus created with monitoring enabled");
        return eventBus;
    }

    private void demonstrateMetricsCollection(KafkaIntegratedEventBus<String> eventBus) {
        LOGGER.info("2. Demonstrating Metrics Collection...");

        // Get metrics collector
        KafkaMetricsCollector metricsCollector = eventBus.getMetricsCollector();

        // Simulate some metrics
        simulateKafkaActivity(metricsCollector);

        // Show metrics collector features
        LOGGER.info("✓ Metrics collection enabled with features:");
        LOGGER.info("  - Topic-level metrics (publish/consume rates, throughput, errors)");
        LOGGER.info("  - Consumer group metrics (lag, processing time, health)");
        LOGGER.info("  - Producer metrics (batch size, buffer utilization, send times)");
        LOGGER.info("  - Connection metrics (broker health, disconnections)");
    }

    private void demonstrateAlertConfiguration(KafkaIntegratedEventBus<String> eventBus) {
        LOGGER.info("3. Demonstrating Alert Configuration...");

        // Configure consumer lag alerts
        eventBus.configureConsumerLagAlert(Duration.ofMinutes(5), lagAlert -> {
            LOGGER.warning("🚨 Consumer Lag Alert: " + lagAlert);
            LOGGER.warning("   Consumer Group: " + lagAlert.getConsumerGroup());
            LOGGER.warning("   Topic: " + lagAlert.getTopic() + ":" + lagAlert.getPartition());
            LOGGER.warning("   Lag: " + lagAlert.getLag());
            LOGGER.warning("   Severity: " + lagAlert.getSeverity());
        });

        // Configure error rate alerts
        eventBus.configureErrorRateAlert(0.05, errorAlert -> {
            LOGGER.warning("🚨 Error Rate Alert: " + errorAlert);
            LOGGER.warning("   Identifier: " + errorAlert.getIdentifier());
            LOGGER.warning("   Error Type: " + errorAlert.getErrorType());
            LOGGER.warning("   Error Rate: " + String.format("%.2f%%", errorAlert.getErrorRate() * 100));
            LOGGER.warning("   Severity: " + errorAlert.getSeverity());
        });

        // Configure throughput alerts
        eventBus.configureThroughputAlert(50.0, throughputAlert -> {
            LOGGER.warning("🚨 Throughput Alert: " + throughputAlert);
            LOGGER.warning("   Identifier: " + throughputAlert.getIdentifier());
            LOGGER.warning("   Change: " + String.format("%.1f%%", throughputAlert.getChangePercent()));
            LOGGER.warning("   Current: " + throughputAlert.getCurrentThroughput());
            LOGGER.warning("   Previous: " + throughputAlert.getPreviousThroughput());
        });

        // Configure custom alert handlers through AlertManager
        AlertManager alertManager = eventBus.getAlertManager();
        alertManager.addAlertHandler(new AlertManager.AlertHandler() {
            @Override
            public void onAlert(AlertManager.Alert alert) {
                LOGGER.info("📢 Alert Triggered: " + alert.getMessage());
                LOGGER.info("   Type: " + alert.getType());
                LOGGER.info("   Severity: " + alert.getSeverity());
                LOGGER.info("   Metadata: " + alert.getMetadata());
            }

            @Override
            public void onAlertResolved(AlertManager.Alert alert) {
                LOGGER.info("✅ Alert Resolved: " + alert.getKey());
                LOGGER.info("   Duration: " + alert.getDuration());
            }
        });

        LOGGER.info("✓ Alert configuration complete:");
        LOGGER.info("  - Consumer lag monitoring (threshold: 5 minutes)");
        LOGGER.info("  - Error rate monitoring (threshold: 5%)");
        LOGGER.info("  - Throughput change monitoring (threshold: 50%)");
        LOGGER.info("  - Custom alert handlers configured");
    }

    private void demonstrateHealthMonitoring(KafkaIntegratedEventBus<String> eventBus) {
        LOGGER.info("4. Demonstrating Health Monitoring...");

        try {
            // Get current health status
            CompletableFuture<KafkaHealthStatus> healthFuture = eventBus.getHealthStatus();
            KafkaHealthStatus health = healthFuture.get(5, TimeUnit.SECONDS);

            LOGGER.info("🏥 Current Health Status:");
            LOGGER.info("   Overall Health: " + health.getOverallHealth());
            LOGGER.info("   Health Score: " + String.format("%.2f", health.getHealthScore()));
            LOGGER.info("   Component Count: " + health.getTotalComponentCount());
            LOGGER.info("   Healthy Components: " + health.getHealthyComponentCount());
            LOGGER.info("   Issues Count: " + health.getHealthIssues().size());

            // Show component health details
            health.getComponentHealth().forEach((component, componentHealth) -> {
                LOGGER.info("   📊 " + component + ": " + componentHealth.getLevel() 
                           + " (" + componentHealth.getStatus() + ")");
                if (componentHealth.hasIssues()) {
                    componentHealth.getIssues().forEach(issue -> 
                        LOGGER.info("      ⚠️ " + issue));
                }
            });

        } catch (Exception e) {
            LOGGER.warning("Failed to get health status: " + e.getMessage());
        }

        LOGGER.info("✓ Health monitoring features:");
        LOGGER.info("  - Overall system health assessment");
        LOGGER.info("  - Component-level health tracking");
        LOGGER.info("  - Health score calculation (0.0 - 1.0)");
        LOGGER.info("  - Issue detection and reporting");
    }

    private void demonstrateMetricsExport(KafkaIntegratedEventBus<String> eventBus) {
        LOGGER.info("5. Demonstrating Metrics Export...");

        try {
            // Export Prometheus metrics
            CompletableFuture<String> prometheusFuture = eventBus.exportPrometheusMetrics();
            String prometheusMetrics = prometheusFuture.get(5, TimeUnit.SECONDS);
            
            LOGGER.info("📈 Prometheus Metrics Sample:");
            String[] lines = prometheusMetrics.split("\n");
            for (int i = 0; i < Math.min(10, lines.length); i++) {
                LOGGER.info("   " + lines[i]);
            }
            if (lines.length > 10) {
                LOGGER.info("   ... (" + (lines.length - 10) + " more lines)");
            }

            // Export JSON metrics
            CompletableFuture<String> jsonFuture = eventBus.exportJsonMetrics();
            String jsonMetrics = jsonFuture.get(5, TimeUnit.SECONDS);
            
            LOGGER.info("📋 JSON Metrics:");
            LOGGER.info("   " + jsonMetrics);

        } catch (Exception e) {
            LOGGER.warning("Failed to export metrics: " + e.getMessage());
        }

        LOGGER.info("✓ Metrics export features:");
        LOGGER.info("  - Prometheus format export");
        LOGGER.info("  - JSON format export");
        LOGGER.info("  - Real-time metrics snapshots");
        LOGGER.info("  - Integration with monitoring systems");
    }

    private void demonstrateRealTimeMonitoring(KafkaIntegratedEventBus<String> eventBus) {
        LOGGER.info("6. Demonstrating Real-time Monitoring...");

        try {
            // Get metrics snapshot
            CompletableFuture<KafkaMetricsSnapshot> snapshotFuture = eventBus.getMetricsSnapshot();
            KafkaMetricsSnapshot snapshot = snapshotFuture.get(5, TimeUnit.SECONDS);

            LOGGER.info("📊 Current Metrics Snapshot:");
            LOGGER.info("   Timestamp: " + snapshot.getTimestamp());
            LOGGER.info("   Topics: " + snapshot.getTopicMetrics().size());
            LOGGER.info("   Consumer Groups: " + snapshot.getConsumerGroupMetrics().size());
            LOGGER.info("   Producers: " + snapshot.getProducerMetrics().size());
            LOGGER.info("   Total Published: " + snapshot.getTotalPublishedMessages());
            LOGGER.info("   Total Consumed: " + snapshot.getTotalConsumedMessages());
            LOGGER.info("   Total Errors: " + snapshot.getTotalErrorCount());
            LOGGER.info("   Total Lag: " + snapshot.getTotalConsumerLag());
            LOGGER.info("   System Healthy: " + snapshot.isSystemHealthy());

            // Show summary statistics
            KafkaMetricsSnapshot.MetricsSummary summary = snapshot.getSummary();
            LOGGER.info("📈 Summary Statistics:");
            LOGGER.info("   " + summary);

        } catch (Exception e) {
            LOGGER.warning("Failed to get metrics snapshot: " + e.getMessage());
        }

        LOGGER.info("✓ Real-time monitoring features:");
        LOGGER.info("  - Live metrics snapshots");
        LOGGER.info("  - System-wide statistics");
        LOGGER.info("  - Performance indicators");
        LOGGER.info("  - Health assessments");
    }

    private void showFinalMonitoringResults(KafkaIntegratedEventBus<String> eventBus) {
        LOGGER.info("7. Final Monitoring Results...");

        try {
            // Get alert statistics
            AlertManager alertManager = eventBus.getAlertManager();
            AlertManager.AlertStatistics alertStats = alertManager.getStatistics();
            
            LOGGER.info("🚨 Alert Statistics:");
            LOGGER.info("   Active Alerts: " + alertStats.getActiveCount());
            LOGGER.info("   Alert History: " + alertStats.getHistoryCount());
            LOGGER.info("   Critical Alerts: " + alertStats.getCriticalCount());
            LOGGER.info("   High Priority: " + alertStats.getHighCount());
            LOGGER.info("   Medium Priority: " + alertStats.getMediumCount());
            LOGGER.info("   Low Priority: " + alertStats.getLowCount());

            // Show active alerts
            if (alertStats.getActiveCount() > 0) {
                LOGGER.info("🔴 Active Alerts:");
                alertManager.getActiveAlerts().forEach((key, alert) -> {
                    LOGGER.info("   " + alert);
                });
            }

            // Final health check
            KafkaHealthStatus finalHealth = eventBus.getHealthStatus().get(5, TimeUnit.SECONDS);
            LOGGER.info("🏥 Final Health Assessment:");
            LOGGER.info("   Overall: " + finalHealth.getOverallHealth());
            LOGGER.info("   Score: " + String.format("%.2f", finalHealth.getHealthScore()));
            LOGGER.info("   Status: " + (finalHealth.isHealthy() ? "✅ HEALTHY" : "⚠️ NEEDS ATTENTION"));

        } catch (Exception e) {
            LOGGER.warning("Failed to get final results: " + e.getMessage());
        }

        LOGGER.info("✅ Advanced Monitoring & Observability Demo Complete!");
        LOGGER.info("   All monitoring features have been demonstrated");
        LOGGER.info("   System is ready for production monitoring");
    }

    private void simulateKafkaActivity(KafkaMetricsCollector collector) {
        LOGGER.info("Simulating Kafka activity for monitoring demonstration...");

        // Simulate message publishing
        for (int i = 0; i < 100; i++) {
            collector.recordMessagePublished("user-events", 
                500 + (int)(Math.random() * 500), Duration.ofMillis(10 + (int)(Math.random() * 20)));
            collector.recordMessagePublished("order-events", 
                1000 + (int)(Math.random() * 1000), Duration.ofMillis(5 + (int)(Math.random() * 15)));
        }

        // Simulate message consumption
        for (int i = 0; i < 80; i++) {
            collector.recordMessageConsumed("user-events", "user-service-group", 
                500 + (int)(Math.random() * 500), Duration.ofMillis(50 + (int)(Math.random() * 100)));
            collector.recordMessageConsumed("order-events", "order-processor-group", 
                1000 + (int)(Math.random() * 1000), Duration.ofMillis(100 + (int)(Math.random() * 200)));
        }

        // Simulate consumer lag
        collector.recordConsumerLag("user-service-group", "user-events", 0, 100 + (int)(Math.random() * 500));
        collector.recordConsumerLag("order-processor-group", "order-events", 0, 50 + (int)(Math.random() * 200));

        // Simulate some errors
        for (int i = 0; i < 5; i++) {
            collector.recordProducerError("user-events", "TimeoutException", 
                new RuntimeException("Simulated timeout"));
            collector.recordConsumerError("user-service-group", "user-events", "DeserializationException", 
                new RuntimeException("Simulated deserialization error"));
        }

        // Simulate connection status
        collector.recordConnectionStatus("broker-1", true);
        collector.recordConnectionStatus("broker-2", true);
        collector.recordConnectionStatus("broker-3", false); // Simulate one broker down

        LOGGER.info("✓ Simulated activity: 100 published, 80 consumed, 5 errors, 3 brokers");
    }
}
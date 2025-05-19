package eda.monitor;

import eda.event.Event;
import eda.event.EventBus;
import eda.event.EventType;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Health Monitoring
 * <p>
 * Reports the health status of services in the system.
 */
public class HealthMonitor {
    private final EventBus eventBus;
    private final Map<String, ServiceHealth> servicesHealth = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public HealthMonitor(EventBus eventBus) {
        this.eventBus = eventBus;

        // Subscribe to health events
        eventBus.subscribe(EventType.SYSTEM_HEALTH, this::handleHealthEvent);
    }

    public void registerService(String serviceName) {
        servicesHealth.put(serviceName, new ServiceHealth(
                serviceName,
                "UNKNOWN",
                Instant.now(),
                Map.of()
        ));
    }

    public void startMonitoring(long intervalSeconds) {
        scheduler.scheduleAtFixedRate(this::checkAllServices, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    private void checkAllServices() {
        for (String service : servicesHealth.keySet()) {
            // In a real system, we'd check the service health here
            // For demo purposes, we'll just report the last known status
            reportHealthStatus(service);
        }
    }

    public void reportHealthStatus(String serviceName) {
        ServiceHealth health = servicesHealth.get(serviceName);
        if (health != null) {
            System.out.println("Health status of " + serviceName + ": " + health.status());
        }
    }

    private void handleHealthEvent(Event event) {
        var data = event.data();
        var serviceName = (String) data.get("serviceName");
        var status = (String) data.get("status");

        @SuppressWarnings("unchecked")
        var metrics = (Map<String, Object>) data.getOrDefault("metrics", Map.of());

        if (serviceName != null && status != null) {
            servicesHealth.put(serviceName, new ServiceHealth(
                    serviceName,
                    status,
                    Instant.now(),
                    metrics
            ));

            System.out.println("Health update from " + serviceName + ": " + status);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    public record ServiceHealth(
            String serviceName,
            String status,
            Instant lastChecked,
            Map<String, Object> metrics
    ) {
    }
}

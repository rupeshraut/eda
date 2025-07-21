package eda.eventbus.projection;

import java.util.List;
import java.util.Map;

/**
 * Overall health status of projections
 */
public class ProjectionHealthStatus {
    private final boolean healthy;
    private final int totalProjections;
    private final int runningProjections;
    private final int failedProjections;
    private final Map<String, ProjectionState> projectionStates;
    private final List<String> unhealthyProjections;
    
    public ProjectionHealthStatus(boolean healthy, int totalProjections, int runningProjections,
                                int failedProjections, Map<String, ProjectionState> projectionStates,
                                List<String> unhealthyProjections) {
        this.healthy = healthy;
        this.totalProjections = totalProjections;
        this.runningProjections = runningProjections;
        this.failedProjections = failedProjections;
        this.projectionStates = Map.copyOf(projectionStates);
        this.unhealthyProjections = List.copyOf(unhealthyProjections);
    }
    
    // Getters
    public boolean isHealthy() { return healthy; }
    public int getTotalProjections() { return totalProjections; }
    public int getRunningProjections() { return runningProjections; }
    public int getFailedProjections() { return failedProjections; }
    public Map<String, ProjectionState> getProjectionStates() { return projectionStates; }
    public List<String> getUnhealthyProjections() { return unhealthyProjections; }
    
    public double getHealthyPercentage() {
        return totalProjections > 0 ? (double) runningProjections / totalProjections : 0.0;
    }
    
    @Override
    public String toString() {
        return "ProjectionHealthStatus{" +
                "healthy=" + healthy +
                ", totalProjections=" + totalProjections +
                ", runningProjections=" + runningProjections +
                ", failedProjections=" + failedProjections +
                ", healthyPercentage=" + String.format("%.1f%%", getHealthyPercentage() * 100) +
                '}';
    }
}
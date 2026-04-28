package monitoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardSnapshot {

    private final int queueSize;
    private final int totalRequests;
    private final int cacheHits;
    private final int cacheMisses;
    private final double averageLatency;
    private final double hitRatio;
    private final boolean simulationPaused;
    private final int configuredRatePerSecond;
    private final List<ServerSnapshot> servers;
    private final List<SystemEvent> recentEvents;
    private final long capturedAtMillis;

    public DashboardSnapshot(
            int queueSize,
            int totalRequests,
            int cacheHits,
            int cacheMisses,
            double averageLatency,
            double hitRatio,
            boolean simulationPaused,
            int configuredRatePerSecond,
            List<ServerSnapshot> servers,
            List<SystemEvent> recentEvents,
            long capturedAtMillis) {
        this.queueSize = queueSize;
        this.totalRequests = totalRequests;
        this.cacheHits = cacheHits;
        this.cacheMisses = cacheMisses;
        this.averageLatency = averageLatency;
        this.hitRatio = hitRatio;
        this.simulationPaused = simulationPaused;
        this.configuredRatePerSecond = configuredRatePerSecond;
        this.servers = Collections.unmodifiableList(new ArrayList<>(servers));
        this.recentEvents = Collections.unmodifiableList(new ArrayList<>(recentEvents));
        this.capturedAtMillis = capturedAtMillis;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getCacheHits() {
        return cacheHits;
    }

    public int getCacheMisses() {
        return cacheMisses;
    }

    public double getAverageLatency() {
        return averageLatency;
    }

    public double getHitRatio() {
        return hitRatio;
    }

    public boolean isSimulationPaused() {
        return simulationPaused;
    }

    public int getConfiguredRatePerSecond() {
        return configuredRatePerSecond;
    }

    public List<ServerSnapshot> getServers() {
        return servers;
    }

    public List<SystemEvent> getRecentEvents() {
        return recentEvents;
    }

    public long getCapturedAtMillis() {
        return capturedAtMillis;
    }
}

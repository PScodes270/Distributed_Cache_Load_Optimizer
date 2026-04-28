package monitoring;

public class ServerSnapshot {

    private final int serverId;
    private final int activeLoad;
    private final int processedRequests;
    private final int cacheHits;
    private final int cacheMisses;
    private final long lastLatencyMillis;
    private final int cacheOccupancy;
    private final int cacheCapacity;
    private final boolean active;

    public ServerSnapshot(
            int serverId,
            int activeLoad,
            int processedRequests,
            int cacheHits,
            int cacheMisses,
            long lastLatencyMillis,
            int cacheOccupancy,
            int cacheCapacity,
            boolean active) {
        this.serverId = serverId;
        this.activeLoad = activeLoad;
        this.processedRequests = processedRequests;
        this.cacheHits = cacheHits;
        this.cacheMisses = cacheMisses;
        this.lastLatencyMillis = lastLatencyMillis;
        this.cacheOccupancy = cacheOccupancy;
        this.cacheCapacity = cacheCapacity;
        this.active = active;
    }

    public int getServerId() {
        return serverId;
    }

    public int getActiveLoad() {
        return activeLoad;
    }

    public int getProcessedRequests() {
        return processedRequests;
    }

    public int getCacheHits() {
        return cacheHits;
    }

    public int getCacheMisses() {
        return cacheMisses;
    }

    public long getLastLatencyMillis() {
        return lastLatencyMillis;
    }

    public int getCacheOccupancy() {
        return cacheOccupancy;
    }

    public int getCacheCapacity() {
        return cacheCapacity;
    }

    public boolean isActive() {
        return active;
    }

    public int getTotalCacheLookups() {
        return cacheHits + cacheMisses;
    }

    public double getCacheHitRatio() {
        int lookups = getTotalCacheLookups();
        return lookups == 0 ? 0 : (cacheHits * 100.0) / lookups;
    }

    public double getCacheFillRatio() {
        return cacheCapacity == 0 ? 0 : (cacheOccupancy * 100.0) / cacheCapacity;
    }
}

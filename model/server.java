package model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import Cache.LFUCache;
import Metrics.metrics;
import monitoring.SystemMonitor;

public class server implements Comparable<server> {

    private final int serverID;
    private final AtomicInteger activeLoad;
    private final AtomicInteger processedRequests;
    private final AtomicInteger cacheHits;
    private final AtomicInteger cacheMisses;
    private final AtomicLong lastLatencyMillis;

    private final int capacity = 2;
    private volatile boolean isActive = true;
    private final LFUCache cache;

    public server(int serverID) {
        this.serverID = serverID;
        this.activeLoad = new AtomicInteger(0);
        this.processedRequests = new AtomicInteger(0);
        this.cacheHits = new AtomicInteger(0);
        this.cacheMisses = new AtomicInteger(0);
        this.lastLatencyMillis = new AtomicLong(0);
        this.cache = new LFUCache(capacity);
        autoRecover();
    }

    public int getserverID() {
        return serverID;
    }

    public void autoRecover() {
        Thread recoveryThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);

                    if (!isActive) {
                        isActive = true;
                        System.out.println("Server " + serverID + " RECOVERED!");
                        SystemMonitor.logEvent("SERVER", "Server " + serverID + " recovered and is healthy again");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "server-recovery-" + serverID);

        recoveryThread.setDaemon(true);
        recoveryThread.start();
    }

    public int getcurrentLoad() {
        return activeLoad.get();
    }

    public int getProcessedRequests() {
        return processedRequests.get();
    }

    public int getServerCacheHits() {
        return cacheHits.get();
    }

    public int getServerCacheMisses() {
        return cacheMisses.get();
    }

    public long getLastLatencyMillis() {
        return lastLatencyMillis.get();
    }

    public int getCacheOccupancy() {
        return cache.size();
    }

    public int getCacheCapacity() {
        return cache.capacity();
    }

    public boolean isActive() {
        return isActive;
    }

    public void increaseLoad() {
        activeLoad.incrementAndGet();
    }

    public void decreaseLoad() {
        if (activeLoad.get() > 0) {
            activeLoad.decrementAndGet();
        }
    }

    public void fail() {
        isActive = false;
    }

    public void recover() {
        isActive = true;
    }

    public void failServer() {
        isActive = false;
    }

    public void recoverServer() {
        isActive = true;
    }

    public String processRequest(int requestId) {

        long start = System.currentTimeMillis();
        processedRequests.incrementAndGet();
        metrics.recordRequest();

        int value = cache.get(requestId);

        if (value != -1) {
            cacheHits.incrementAndGet();
            metrics.recordCacheHit();
            SystemMonitor.logEvent("CACHE_HIT", "Server " + serverID + " served request " + requestId + " from cache");

            long latency = System.currentTimeMillis() - start;
            lastLatencyMillis.set(latency);
            metrics.recordLatency(latency);

            return "CACHE HIT for request " + requestId + " on Server " + serverID;
        }

        cacheMisses.incrementAndGet();
        metrics.recordCacheMiss();
        SystemMonitor.logEvent("CACHE_MISS",
                "Server " + serverID + " processed request " + requestId + " after cache miss");

        try {
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int result = requestId * 10;
        cache.put(requestId, result);

        long latency = System.currentTimeMillis() - start;
        lastLatencyMillis.set(latency);
        metrics.recordLatency(latency);

        return "CACHE MISS -> processed request " + requestId + " on Server " + serverID;
    }

    @Override
    public int compareTo(server other) {
        return Integer.compare(this.getcurrentLoad(), other.getcurrentLoad());
    }

    @Override
    public String toString() {
        return "Server " + serverID
               + " | Active load: " + activeLoad.get()
               + " | Processed: " + processedRequests.get()
               + " | Active: " + isActive;
    }
}

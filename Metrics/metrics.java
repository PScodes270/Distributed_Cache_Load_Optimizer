package Metrics;

public class metrics {

    private static int totalRequests = 0;
    private static int cacheHits = 0;
    private static int cacheMisses = 0;
    private static long totalLatency = 0;

 
    public static synchronized void recordRequest() {
        totalRequests++;
    }


    public static synchronized void recordCacheHit() {
        cacheHits++;
    }


    public static synchronized void recordCacheMiss() {
        cacheMisses++;
    }

  
    public static synchronized void recordLatency(long latency) {
        totalLatency += latency;
    }
    public static synchronized int getTotalRequests() {
        return totalRequests;
    }

    public static synchronized int getCacheHits() {
        return cacheHits;
    }

    public static synchronized int getCacheMisses() {
        return cacheMisses;
    }

    public static synchronized double getAverageLatency() {
        return totalRequests == 0 ? 0 : (totalLatency * 1.0) / totalRequests;
    }

    public static synchronized double getHitRatio() {
        return totalRequests == 0 ? 0 : (cacheHits * 100.0) / totalRequests;
    }
  
    public static synchronized void printStats() {

        System.out.println("\n ===== SYSTEM METRICS =====");

        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Cache Hits: " + cacheHits);
        System.out.println("Cache Misses: " + cacheMisses);

        double hitRatio = getHitRatio();

        System.out.println("Cache Hit Ratio: " + hitRatio + "%");

        double avgLatency = getAverageLatency();

        System.out.println("Average Latency: " + avgLatency + " ms");

        System.out.println("============================\n");
    }
}

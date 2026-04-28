package Cache;

import java.util.HashMap;
import java.util.LinkedHashSet;

public class LFUCache {

    private int capacity;
    private int minFreq;

    private HashMap<Integer, Integer> valueMap;
    private HashMap<Integer, Integer> freqMap;
    private HashMap<Integer, LinkedHashSet<Integer>> listMap;

    public LFUCache(int capacity) {

        this.capacity = capacity;
        minFreq = 0;

        valueMap = new HashMap<>();
        freqMap = new HashMap<>();
        listMap = new HashMap<>();
    }

    public synchronized int get(int key) {

        if (!valueMap.containsKey(key)) {
            return -1;
        }

        int freq = freqMap.get(key);
        freqMap.put(key, freq + 1);

        listMap.get(freq).remove(key);

        if (freq == minFreq && listMap.get(freq).isEmpty()) {
            minFreq++;
        }

        listMap.computeIfAbsent(freq + 1, k -> new LinkedHashSet<>()).add(key);

        return valueMap.get(key);
    }

    public synchronized void put(int key, int value) {

        if (capacity == 0) {
            return;
        }

        if (valueMap.containsKey(key)) {
            valueMap.put(key, value);
            get(key);
            return;
        }

        if (valueMap.size() >= capacity) {
            LinkedHashSet<Integer> keys = listMap.get(minFreq);

            if (keys != null && !keys.isEmpty()) {
                int evict = keys.iterator().next();
                System.out.println("Evicting key " + evict
                        + " | Frequency = " + minFreq
                        + " (LFU + LRU tie-break)");
                keys.remove(evict);

                valueMap.remove(evict);
                freqMap.remove(evict);
            }
        }

        valueMap.put(key, value);
        freqMap.put(key, 1);

        minFreq = 1;

        listMap.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
    }

    public synchronized int size() {
        return valueMap.size();
    }

    public int capacity() {
        return capacity;
    }
}

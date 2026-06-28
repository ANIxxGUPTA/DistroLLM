package com.distrollm.metrics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LatencyTracker {
    
    // Inline Comment: Why ConcurrentLinkedDeque over ArrayList + synchronized?
    // 
    // Using a ConcurrentLinkedDeque provides non-blocking, lock-free insertions at both ends, 
    // relying internally on atomic compare-and-swap (CAS) operations. This allows multiple 
    // threads to concurrently record latency measurements without causing thread contention 
    // or blocking. 
    // If we used an ArrayList protected by a synchronized block, it would serialize access, 
    // creating a massive bottleneck under high throughput as every thread waits to acquire 
    // the lock just to append a metric.
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> store = new ConcurrentHashMap<>();

    public void record(String key, long latencyMs) {
        ConcurrentLinkedDeque<Long> deque = store.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(latencyMs);
        if (deque.size() > 1000) {
            deque.pollFirst();
        }
    }

    public double getPercentile(String key, double percentile) {
        ConcurrentLinkedDeque<Long> deque = store.get(key);
        if (deque == null || deque.isEmpty()) {
            return -1.0;
        }

        // Copy the deque to an array to snapshot the data for sorting.
        // This ensures thread safety without locking the data structure.
        Object[] snapshot = deque.toArray();
        if (snapshot.length == 0) {
            return -1.0;
        }

        long[] sorted = new long[snapshot.length];
        for (int i = 0; i < snapshot.length; i++) {
            sorted[i] = (Long) snapshot[i];
        }
        Arrays.sort(sorted);

        int index = (int) Math.ceil((percentile / 100.0) * sorted.length) - 1;
        index = Math.max(0, Math.min(index, sorted.length - 1));
        return sorted[index];
    }

    public double getP50(String key) { return getPercentile(key, 50); }
    public double getP95(String key) { return getPercentile(key, 95); }
    public double getP99(String key) { return getPercentile(key, 99); }

    public Map<String, Double> getSummary(String key) {
        Map<String, Double> summary = new HashMap<>();
        
        ConcurrentLinkedDeque<Long> deque = store.get(key);
        if (deque == null || deque.isEmpty()) {
            return summary;
        }

        Object[] snapshot = deque.toArray();
        long sum = 0;
        for (Object o : snapshot) {
            sum += (Long) o;
        }

        summary.put("p50", getP50(key));
        summary.put("p95", getP95(key));
        summary.put("p99", getP99(key));
        summary.put("count", (double) snapshot.length);
        summary.put("mean", snapshot.length > 0 ? (double) sum / snapshot.length : 0.0);
        return summary;
    }

    public Set<String> getAllKeys() {
        return store.keySet();
    }
}

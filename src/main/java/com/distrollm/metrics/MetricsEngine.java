package com.distrollm.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsEngine {

    private static volatile MetricsEngine instance;

    private final LatencyTracker latencyTracker;
    private final ConcurrentHashMap<String, AtomicLong> counters;
    private final ConcurrentHashMap<String, AtomicLong> gauges;

    private MetricsEngine() {
        this.latencyTracker = new LatencyTracker();
        this.counters = new ConcurrentHashMap<>();
        this.gauges = new ConcurrentHashMap<>();
    }

    public static MetricsEngine getInstance() {
        if (instance == null) {
            synchronized (MetricsEngine.class) {
                if (instance == null) {
                    instance = new MetricsEngine();
                }
            }
        }
        return instance;
    }

    public void increment(String counter) {
        counters.computeIfAbsent(counter, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void incrementBy(String counter, long delta) {
        counters.computeIfAbsent(counter, k -> new AtomicLong(0)).addAndGet(delta);
    }

    public void setGauge(String gauge, long value) {
        gauges.computeIfAbsent(gauge, k -> new AtomicLong(0)).set(value);
    }

    public void recordLatency(String key, long latencyMs) {
        latencyTracker.record(key, latencyMs);
    }

    public Map<String, Object> getSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        
        for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().get());
        }
        
        for (Map.Entry<String, AtomicLong> entry : gauges.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().get());
        }
        
        for (String key : latencyTracker.getAllKeys()) {
            snapshot.put(key + ".p50", latencyTracker.getP50(key));
            snapshot.put(key + ".p95", latencyTracker.getP95(key));
            snapshot.put(key + ".p99", latencyTracker.getP99(key));
        }

        return snapshot;
    }

    public String exportPrometheusText() {
        StringBuilder sb = new StringBuilder();
        
        for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
            String promKey = formatPrometheusKey(entry.getKey());
            sb.append("# HELP distrollm_").append(promKey).append(" Counter metric\n");
            sb.append("# TYPE distrollm_").append(promKey).append(" counter\n");
            sb.append("distrollm_").append(promKey).append(" ").append(entry.getValue().get()).append("\n");
        }
        
        for (Map.Entry<String, AtomicLong> entry : gauges.entrySet()) {
            String promKey = formatPrometheusKey(entry.getKey());
            sb.append("# HELP distrollm_").append(promKey).append(" Gauge metric\n");
            sb.append("# TYPE distrollm_").append(promKey).append(" gauge\n");
            sb.append("distrollm_").append(promKey).append(" ").append(entry.getValue().get()).append("\n");
        }
        
        for (String key : latencyTracker.getAllKeys()) {
            double p95 = latencyTracker.getP95(key);
            if (p95 >= 0) {
                sb.append("# HELP distrollm_latency_p95_ms Latency p95 in ms\n");
                sb.append("# TYPE distrollm_latency_p95_ms gauge\n");
                sb.append("distrollm_latency_p95_ms{endpoint=\"").append(key).append("\"} ").append(p95).append("\n");
            }
        }
        
        return sb.toString();
    }

    private String formatPrometheusKey(String key) {
        return key.replace(".", "_");
    }
}

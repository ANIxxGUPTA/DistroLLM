package com.distrollm.metrics;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricsReporter {

    private final ScheduledExecutorService daemon = Executors.newSingleThreadScheduledExecutor();

    public void start() {
        daemon.scheduleAtFixedRate(this::printReport, 0, 15, TimeUnit.SECONDS);
    }

    public void stop() {
        daemon.shutdownNow();
    }

    private void printReport() {
        Map<String, Object> snapshot = MetricsEngine.getInstance().getSnapshot();

        long total = (Long) snapshot.getOrDefault("requests.total", 0L);
        long success = (Long) snapshot.getOrDefault("requests.success", 0L);
        long failed = (Long) snapshot.getOrDefault("requests.failed", 0L);
        
        double successRate = total > 0 ? ((double) success / total) * 100.0 : 0.0;
        
        double p50 = 0, p95 = 0, p99 = 0;
        int count = 0;
        for (String key : snapshot.keySet()) {
            if (key.endsWith(".p50")) {
                p50 += (Double) snapshot.get(key);
                count++;
            } else if (key.endsWith(".p95")) {
                p95 += (Double) snapshot.get(key);
            } else if (key.endsWith(".p99")) {
                p99 += (Double) snapshot.get(key);
            }
        }
        
        if (count > 0) {
            p50 /= count; p95 /= count; p99 /= count;
        }

        long circuitEvents = (Long) snapshot.getOrDefault("circuit.open.count", 0L);
        long retries = (Long) snapshot.getOrDefault("retry.count", 0L);

        long queueDepth = (Long) snapshot.getOrDefault("queue.depth", 0L);
        long activeThreads = (Long) snapshot.getOrDefault("active.threads", 0L);

        System.out.println("=== DistroLLM Metrics Report ===");
        System.out.printf("Requests:  total=%d  success=%d  failed=%d  success_rate=%.1f%%\n", total, success, failed, successRate);
        System.out.printf("Latency:   p50=%.0fms  p95=%.0fms  p99=%.0fms\n", p50, p95, p99);
        System.out.printf("Circuit:   open_events=%d  retries=%d\n", circuitEvents, retries);
        System.out.printf("Queue:     depth=%d  active_threads=%d\n", queueDepth, activeThreads);
        System.out.println("================================");
    }
}

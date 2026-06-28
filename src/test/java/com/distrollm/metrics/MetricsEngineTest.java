package com.distrollm.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MetricsEngineTest {

    private MetricsEngine metrics;

    @BeforeEach
    public void setUp() {
        metrics = MetricsEngine.getInstance();
    }

    @Test
    public void testRecordLatenciesFromConcurrentThreads() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        
        for (int i = 0; i < 1000; i++) {
            final long latency = i; // Values 0 to 999
            executor.submit(() -> {
                metrics.recordLatency("testEndpoint", latency);
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        Map<String, Object> snapshot = metrics.getSnapshot();
        double p99 = (double) snapshot.getOrDefault("testEndpoint.p99", -1.0);
        
        // p99 of sorted array 0..999 is at index ~990
        assertTrue(p99 >= 985 && p99 <= 995, "p99 should be ~990, but was " + p99);
    }

    @Test
    public void testConcurrentIncrements() throws InterruptedException {
        String uniqueKey = "test.concurrent.increments";
        ExecutorService executor = Executors.newFixedThreadPool(50);
        
        for (int i = 0; i < 5000; i++) {
            executor.submit(() -> {
                metrics.increment(uniqueKey);
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        long count = (long) metrics.getSnapshot().get(uniqueKey);
        assertEquals(5000L, count, "Final count must perfectly match the number of increments");
    }

    @Test
    public void testPrometheusExport() {
        metrics.increment("requests.total");
        metrics.setGauge("active.threads", 12);
        metrics.recordLatency("endpoint1", 145);
        
        String output = metrics.exportPrometheusText();
        
        // Assert valid format and data
        assertTrue(output.contains("# HELP distrollm_requests_total"), "Output must contain help comment");
        assertTrue(output.contains("distrollm_requests_total"), "Output must contain counter key");
        
        assertTrue(output.contains("# TYPE distrollm_active_threads gauge"), "Output must define gauge type");
        assertTrue(output.contains("distrollm_active_threads 12"), "Output must contain the gauge value");
        
        assertTrue(output.contains("distrollm_latency_p95_ms{endpoint=\"endpoint1\"}"), "Output must contain formatted latency metric");
    }

    @Test
    public void testSingletonSafety() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Set<MetricsEngine> instances = new HashSet<>();
        
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                synchronized (instances) {
                    instances.add(MetricsEngine.getInstance());
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        assertEquals(1, instances.size(), "Only a single instance of MetricsEngine should exist globally");
    }
}

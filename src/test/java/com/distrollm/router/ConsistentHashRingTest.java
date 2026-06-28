package com.distrollm.router;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ConsistentHashRingTest {

    private ConsistentHashRing ring;

    @BeforeEach
    public void setUp() {
        ring = new ConsistentHashRing();
    }

    @Test
    public void testDistributionWithLowStdDeviation() {
        ModelEndpoint e1 = new ModelEndpoint("ep1", "http://localhost:8001", "llama");
        ModelEndpoint e2 = new ModelEndpoint("ep2", "http://localhost:8002", "llama");
        ModelEndpoint e3 = new ModelEndpoint("ep3", "http://localhost:8003", "llama");

        ring.addEndpoint(e1, 150);
        ring.addEndpoint(e2, 150);
        ring.addEndpoint(e3, 150);

        Map<String, Integer> counts = new HashMap<>();
        counts.put("ep1", 0);
        counts.put("ep2", 0);
        counts.put("ep3", 0);

        int totalQueries = 1000;
        for (int i = 0; i < totalQueries; i++) {
            String queryId = UUID.randomUUID().toString();
            ModelEndpoint target = ring.getEndpoint(queryId);
            counts.put(target.getId(), counts.get(target.getId()) + 1);
        }

        // Calculate standard deviation
        double mean = totalQueries / 3.0;
        double sumSquares = 0;
        for (int count : counts.values()) {
            sumSquares += Math.pow(count - mean, 2);
        }
        double stdDev = Math.sqrt(sumSquares / 3.0);
        
        // Ensure stdDev is less than 15% of the total queries (150)
        assertTrue(stdDev < (0.15 * totalQueries), "Std deviation should be < 15%");
    }

    @Test
    public void testRemovingEndpointReassignsOnlyItsQueries() {
        ModelEndpoint e1 = new ModelEndpoint("ep1", "http://localhost:8001", "llama");
        ModelEndpoint e2 = new ModelEndpoint("ep2", "http://localhost:8002", "llama");
        ModelEndpoint e3 = new ModelEndpoint("ep3", "http://localhost:8003", "llama");

        ring.addEndpoint(e1, 150);
        ring.addEndpoint(e2, 150);
        ring.addEndpoint(e3, 150);

        // Track original assignments
        Map<String, String> assignments = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String queryId = UUID.randomUUID().toString();
            assignments.put(queryId, ring.getEndpoint(queryId).getId());
        }

        // Remove ep2
        ring.removeEndpoint("ep2");

        int reassignedFromE1 = 0;
        int reassignedFromE3 = 0;

        for (Map.Entry<String, String> entry : assignments.entrySet()) {
            String queryId = entry.getKey();
            String originalEp = entry.getValue();
            String newEp = ring.getEndpoint(queryId).getId();

            if (originalEp.equals("ep1") && !newEp.equals("ep1")) reassignedFromE1++;
            if (originalEp.equals("ep3") && !newEp.equals("ep3")) reassignedFromE3++;
        }

        // The key property of consistent hashing: removing a node should not affect the mapping of keys to OTHER nodes
        assertEquals(0, reassignedFromE1, "Queries mapped to ep1 should not be reassigned");
        assertEquals(0, reassignedFromE3, "Queries mapped to ep3 should not be reassigned");
    }

    @Test
    public void testConcurrentReadsWhileModifying() throws InterruptedException {
        ModelEndpoint e1 = new ModelEndpoint("ep1", "http://localhost:8001", "llama");
        ring.addEndpoint(e1, 150);

        ExecutorService readers = Executors.newFixedThreadPool(10);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        AtomicInteger readCount = new AtomicInteger(0);
        
        // Start 10 readers
        for (int i = 0; i < 10; i++) {
            readers.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    try {
                        ring.getEndpoint(UUID.randomUUID().toString());
                        readCount.incrementAndGet();
                    } catch (Exception e) {
                        exceptionCount.incrementAndGet();
                    }
                }
            });
        }

        // 1 writer modifying the ring concurrently
        Thread writer = new Thread(() -> {
            for (int i = 2; i <= 20; i++) {
                try {
                    ModelEndpoint ep = new ModelEndpoint("ep" + i, "http://localhost:800" + i, "llama");
                    ring.addEndpoint(ep, 150);
                    Thread.sleep(5); // Simulate time between additions
                    if (i % 2 == 0) {
                        ring.removeEndpoint("ep" + (i - 1)); // Remove some endpoints too
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                }
            }
        });
        writer.start();

        readers.shutdown();
        readers.awaitTermination(5, TimeUnit.SECONDS);
        writer.join();

        assertEquals(0, exceptionCount.get(), "No exceptions should be thrown during concurrent access");
        assertEquals(10000, readCount.get(), "All reads should complete successfully");
    }
}

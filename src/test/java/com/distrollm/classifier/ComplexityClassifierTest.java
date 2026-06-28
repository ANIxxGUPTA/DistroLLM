package com.distrollm.classifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ComplexityClassifierTest {

    private ComplexityClassifier classifier;

    @BeforeEach
    public void setUp() {
        classifier = new ComplexityClassifier();
    }

    @Test
    public void testSimplePrompt() {
        assertEquals(QueryComplexity.SIMPLE, classifier.classify("Hello"));
    }

    @Test
    public void testMediumPrompt() {
        // "Explain" (+2), "difference between" (+2) -> Score 4 (Medium)
        assertEquals(QueryComplexity.MEDIUM, classifier.classify("Explain the difference between TCP and UDP protocols"));
    }

    @Test
    public void testComplexPrompt() {
        // Contains "architecture" (+2), "why does" (+2), "how does" (+2) -> +6 Max keyword
        // Length > 150 -> +1
        // Multiple ? -> +2
        // Code snippet "public class" -> +3
        // Total Score = 12 (> 8) -> Complex
        String prompt = "Can you analyze this architecture? ??? " +
                "public class MyClass { } \n" +
                "how does it work? why does it fail? design a better one.";
        
        assertEquals(QueryComplexity.COMPLEX, classifier.classify(prompt));
    }

    @Test
    public void testClassifyWithReason() {
        Map<String, Object> result = classifier.classifyWithReason("Explain the difference between TCP and UDP protocols");
        
        assertNotNull(result.get("complexity"));
        assertEquals(QueryComplexity.MEDIUM, result.get("complexity"));
        
        @SuppressWarnings("unchecked")
        List<String> reasons = (List<String>) result.get("reasons");
        assertNotNull(reasons);
        assertFalse(reasons.isEmpty());
        assertTrue(reasons.stream().anyMatch(r -> r.toLowerCase().contains("explain")));
    }

    @Test
    public void testConcurrentClassificationNoRaceConditions() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                try {
                    QueryComplexity res = classifier.classify("Hello world, how does this work? public class A {}");
                    if (res != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    fail("Exception occurred during concurrent classification: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
        
        assertTrue(terminated, "Executor did not terminate in time");
        assertEquals(1000, successCount.get(), "All 1000 concurrent threads should complete classification successfully");
    }
}

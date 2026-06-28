package com.distrollm.router;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class CircuitBreakerTest {

    private CircuitBreaker breaker;

    @BeforeEach
    public void setUp() {
        breaker = new CircuitBreaker("ep1");
    }

    @Test
    public void testThreeConsecutiveFailuresOpensCircuit() {
        assertEquals(CircuitBreakerState.CLOSED, breaker.getState());
        
        breaker.recordFailure();
        assertEquals(CircuitBreakerState.CLOSED, breaker.getState());
        
        breaker.recordFailure();
        assertEquals(CircuitBreakerState.CLOSED, breaker.getState());
        
        breaker.recordFailure();
        assertEquals(CircuitBreakerState.OPEN, breaker.getState());
    }

    @Test
    public void testOpenStateRejectsRequestsImmediately() {
        breaker.recordFailure();
        breaker.recordFailure();
        breaker.recordFailure();
        
        assertEquals(CircuitBreakerState.OPEN, breaker.getState());
        
        // This should return false instantly without waiting
        assertFalse(breaker.allowRequest());
    }

    @Test
    public void testTransitionToHalfOpenAfterTimeElapsed() {
        breaker.recordFailure();
        breaker.recordFailure();
        breaker.recordFailure();
        
        assertEquals(CircuitBreakerState.OPEN, breaker.getState());
        
        // Mock the last failure time to 31 seconds ago
        breaker.setLastFailureTime(System.currentTimeMillis() - 31_000);
        
        // The next request should be allowed and state should become HALF_OPEN
        assertTrue(breaker.allowRequest());
        assertEquals(CircuitBreakerState.HALF_OPEN, breaker.getState());
    }

    @Test
    public void testTwoSuccessesInHalfOpenTransitionsToClosed() {
        breaker.recordFailure();
        breaker.recordFailure();
        breaker.recordFailure();
        breaker.setLastFailureTime(System.currentTimeMillis() - 31_000);
        breaker.allowRequest(); // Transitions to HALF_OPEN
        
        breaker.recordSuccess(); // 1st success
        assertEquals(CircuitBreakerState.HALF_OPEN, breaker.getState());
        
        breaker.recordSuccess(); // 2nd success
        assertEquals(CircuitBreakerState.CLOSED, breaker.getState());
    }

    @Test
    public void testOneFailureInHalfOpenTransitionsToOpen() {
        breaker.recordFailure();
        breaker.recordFailure();
        breaker.recordFailure();
        breaker.setLastFailureTime(System.currentTimeMillis() - 31_000);
        breaker.allowRequest(); // Transitions to HALF_OPEN
        
        breaker.recordFailure(); // 1st failure in HALF_OPEN
        assertEquals(CircuitBreakerState.OPEN, breaker.getState());
    }

    @Test
    public void testConcurrentFailuresDoNotCorruptState() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                try {
                    breaker.recordFailure();
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(0, exceptionCount.get(), "No exceptions should be thrown during concurrent access");
        assertEquals(CircuitBreakerState.OPEN, breaker.getState(), "Circuit must be OPEN after concurrent failures");
    }
}

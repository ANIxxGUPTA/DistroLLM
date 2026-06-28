package com.distrollm.router;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CircuitBreaker {
    // We use AtomicReference and compareAndSet (CAS) for state transitions to ensure 
    // thread safety without the overhead of synchronized blocks. Synchronized blocks 
    // can cause thread contention and block threads in a high-throughput environment, 
    // whereas CAS operations rely on hardware-level atomic instructions, providing 
    // non-blocking, highly performant concurrency control.
    private final AtomicReference<CircuitBreakerState> state = new AtomicReference<>(CircuitBreakerState.CLOSED);
    
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong lastStateChangeTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean probeInFlight = new AtomicBoolean(false);

    private final int failureThreshold = 3;
    private final int successThreshold = 2;
    private final long openDurationMs = 30_000;
    private final String endpointId;

    public CircuitBreaker(String endpointId) {
        this.endpointId = endpointId;
    }

    public boolean allowRequest() {
        CircuitBreakerState currentState = state.get();

        if (currentState == CircuitBreakerState.CLOSED) {
            return true;
        }

        if (currentState == CircuitBreakerState.OPEN) {
            long now = System.currentTimeMillis();
            if (now - lastFailureTime.get() >= openDurationMs) {
                // Time has elapsed, try to transition to HALF_OPEN
                if (state.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN)) {
                    lastStateChangeTime.set(now);
                    probeInFlight.set(true); // First probe gets through
                    return true;
                } else {
                    // Another thread changed the state concurrently, re-evaluate
                    currentState = state.get();
                }
            } else {
                return false; // Fast-fail
            }
        }

        if (currentState == CircuitBreakerState.HALF_OPEN) {
            // Only allow if no probe is currently in flight
            return probeInFlight.compareAndSet(false, true);
        }

        return false;
    }

    public void recordSuccess() {
        CircuitBreakerState currentState = state.get();

        if (currentState == CircuitBreakerState.CLOSED) {
            failureCount.set(0);
        } else if (currentState == CircuitBreakerState.HALF_OPEN) {
            int currentSuccesses = successCount.incrementAndGet();
            if (currentSuccesses >= successThreshold) {
                if (state.compareAndSet(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.CLOSED)) {
                    failureCount.set(0);
                    successCount.set(0);
                    lastStateChangeTime.set(System.currentTimeMillis());
                }
            }
            // Allow the next probe to be sent if we haven't transitioned yet
            probeInFlight.set(false);
        }
    }

    public void recordFailure() {
        CircuitBreakerState currentState = state.get();

        if (currentState == CircuitBreakerState.CLOSED) {
            int currentFailures = failureCount.incrementAndGet();
            if (currentFailures >= failureThreshold) {
                if (state.compareAndSet(CircuitBreakerState.CLOSED, CircuitBreakerState.OPEN)) {
                    lastFailureTime.set(System.currentTimeMillis());
                    lastStateChangeTime.set(System.currentTimeMillis());
                }
            }
        } else if (currentState == CircuitBreakerState.HALF_OPEN) {
            if (state.compareAndSet(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.OPEN)) {
                lastFailureTime.set(System.currentTimeMillis());
                lastStateChangeTime.set(System.currentTimeMillis());
                successCount.set(0);
            }
            // Reset probe in flight so a future HALF_OPEN transition can send probes
            probeInFlight.set(false);
        }
    }

    public CircuitBreakerState getState() {
        return state.get();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("state", state.get());
        stats.put("failureCount", failureCount.get());
        stats.put("successCount", successCount.get());
        stats.put("lastFailureTime", lastFailureTime.get());
        stats.put("endpointId", endpointId);
        return stats;
    }

    // For testing purposes
    public void setLastFailureTime(long timeMs) {
        lastFailureTime.set(timeMs);
    }
}

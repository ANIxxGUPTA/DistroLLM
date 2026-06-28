package com.distrollm.router;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

public class RetryPolicy {
    private final Map<String, CircuitBreaker> breakers;
    private final Map<String, AtomicLong> stats;

    public RetryPolicy(Map<String, CircuitBreaker> breakers, Map<String, AtomicLong> stats) {
        this.breakers = breakers;
        this.stats = stats;
    }

    public QueryResult executeWithRetry(Callable<QueryResult> task, String endpointId) throws Exception {
        CircuitBreaker breaker = breakers.computeIfAbsent(endpointId, CircuitBreaker::new);
        int maxAttempts = 3;
        long delayMs = 100;
        Throwable lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (attempt > 1 && stats != null && stats.containsKey("retriedCount")) {
                stats.get("retriedCount").incrementAndGet();
            }

            if (!breaker.allowRequest()) {
                throw new CircuitOpenException(endpointId, breaker.getState());
            }

            try {
                QueryResult result = task.call();
                if (result != null && result.isSuccess()) {
                    breaker.recordSuccess();
                    return result;
                } else {
                    breaker.recordFailure();
                    lastError = new RuntimeException("Task returned unsuccessful result");
                }
            } catch (Exception e) {
                breaker.recordFailure();
                lastError = e;
            }

            if (attempt < maxAttempts) {
                Thread.sleep(delayMs);
                delayMs *= 2; // Exponential backoff: 100ms, 200ms...
            }
        }

        throw new MaxRetriesExceededException(maxAttempts, lastError, endpointId);
    }
}

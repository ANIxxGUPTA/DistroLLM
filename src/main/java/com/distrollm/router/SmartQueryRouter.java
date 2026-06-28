package com.distrollm.router;

import com.distrollm.classifier.ComplexityClassifier;
import com.distrollm.classifier.OllamaClient;
import com.distrollm.classifier.QueryComplexity;
import com.distrollm.metrics.MetricsEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SmartQueryRouter {
    private final WorkerPool workerPool;
    private final EndpointRegistry endpointRegistry;
    private final ComplexityClassifier classifier;
    private final OllamaClient ollamaClient;
    
    private final ConcurrentHashMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final RetryPolicy retryPolicy;
    
    private final ConcurrentHashMap<String, AtomicLong> stats = new ConcurrentHashMap<>();

    public SmartQueryRouter(WorkerPool workerPool, EndpointRegistry endpointRegistry) {
        this.workerPool = workerPool;
        this.endpointRegistry = endpointRegistry;
        this.classifier = new ComplexityClassifier();
        this.ollamaClient = new OllamaClient();
        
        for (QueryComplexity qc : QueryComplexity.values()) {
            stats.put(qc.name(), new AtomicLong(0));
        }
        stats.put("circuitOpenCount", new AtomicLong(0));
        stats.put("retriedCount", new AtomicLong(0));
        
        this.retryPolicy = new RetryPolicy(breakers, stats);
    }

    public CompletableFuture<QueryResult> routeQuery(String prompt) {
        return CompletableFuture.supplyAsync(() -> routeQuerySync(prompt));
    }
    
    public QueryResult routeQuerySync(String prompt) {
        MetricsEngine metrics = MetricsEngine.getInstance();
        metrics.increment("requests.total");

        QueryComplexity complexity = classifier.classify(prompt);
        stats.get(complexity.name()).incrementAndGet();
        
        String queryId = UUID.randomUUID().toString();
        ModelEndpoint endpoint = endpointRegistry.getEndpointForQuery(queryId);
        
        int rerouteAttempts = 0;
        QueryResult finalResult = null;
        
        while (endpoint != null && rerouteAttempts < 3) {
            String endpointId = endpoint.getId();
            
            QueryTask task = new QueryTask(
                UUID.fromString(queryId), 
                prompt, 
                ollamaClient, 
                complexity.getTargetModel(), 
                complexity.getMaxTokens(), 
                complexity.getTimeoutMs()
            );
            
            Callable<QueryResult> callableTask = () -> workerPool.submit(task).get();
            
            try {
                finalResult = retryPolicy.executeWithRetry(callableTask, endpointId);
                break;
            } catch (CircuitOpenException e) {
                stats.get("circuitOpenCount").incrementAndGet();
                metrics.increment("circuit.open.count");
                
                endpoint.markUnhealthy();
                endpoint = endpointRegistry.getEndpointForQuery(queryId);
                rerouteAttempts++;
                metrics.increment("retry.count");
            } catch (MaxRetriesExceededException e) {
                endpoint.markUnhealthy();
                endpoint = endpointRegistry.getEndpointForQuery(queryId);
                rerouteAttempts++;
                metrics.increment("retry.count");
            } catch (Exception e) {
                finalResult = new QueryResult(UUID.fromString(queryId), "Error: " + e.getMessage(), 0, endpointId, false);
                break;
            }
        }
        
        if (finalResult == null) {
            finalResult = new QueryResult(UUID.fromString(queryId), "No healthy endpoints available to process request", 0, "unknown", false);
        }

        if (finalResult.isSuccess()) {
            metrics.increment("requests.success");
        } else {
            metrics.increment("requests.failed");
        }
        
        metrics.recordLatency(complexity.name(), finalResult.getLatencyMs());
        
        if (finalResult.getModelEndpoint() != null && !finalResult.getModelEndpoint().equals("unknown")) {
            metrics.recordLatency(finalResult.getModelEndpoint(), finalResult.getLatencyMs());
        }

        metrics.setGauge("active.threads", workerPool.getActiveCount());
        metrics.setGauge("queue.depth", workerPool.getQueueSize());

        return finalResult;
    }

    public Map<String, Object> getMetricsSnapshot() {
        return MetricsEngine.getInstance().getSnapshot();
    }

    public Map<String, Long> getRoutingStats() {
        Map<String, Long> formattedStats = new HashMap<>();
        for (Map.Entry<String, AtomicLong> entry : stats.entrySet()) {
            formattedStats.put(entry.getKey(), entry.getValue().get());
        }
        return formattedStats;
    }

    // Added for Phase 6 APIs
    public String getCircuitState(String endpointId) {
        CircuitBreaker breaker = breakers.get(endpointId);
        return breaker != null ? breaker.getState().name() : "CLOSED";
    }

    public void shutdown() {
        workerPool.gracefulShutdown();
    }
}

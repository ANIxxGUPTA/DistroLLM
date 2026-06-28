package com.distrollm.router;

import com.distrollm.classifier.ComplexityClassifier;
import com.distrollm.classifier.OllamaClient;
import com.distrollm.classifier.QueryComplexity;

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
    
    // Added breakers for Circuit Breaker pattern
    private final ConcurrentHashMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final RetryPolicy retryPolicy;
    
    // Updated to track stats dynamically using String keys
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
        QueryComplexity complexity = classifier.classify(prompt);
        stats.get(complexity.name()).incrementAndGet();
        
        String queryId = UUID.randomUUID().toString();
        ModelEndpoint endpoint = endpointRegistry.getEndpointForQuery(queryId);
        
        int rerouteAttempts = 0;
        
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
            
            // Wrap the WorkerPool submission within a Callable for the RetryPolicy
            Callable<QueryResult> callableTask = () -> workerPool.submit(task).get();
            
            try {
                // Execute the task utilizing circuit breakers and exponential backoff retries
                return retryPolicy.executeWithRetry(callableTask, endpointId);
            } catch (CircuitOpenException e) {
                stats.get("circuitOpenCount").incrementAndGet();
                
                // If the circuit is open, we mark the endpoint as unhealthy in the registry
                // so the next call to getEndpointForQuery() finds a different, healthy node
                endpoint.markUnhealthy();
                endpoint = endpointRegistry.getEndpointForQuery(queryId);
                rerouteAttempts++;
            } catch (MaxRetriesExceededException e) {
                endpoint.markUnhealthy();
                endpoint = endpointRegistry.getEndpointForQuery(queryId);
                rerouteAttempts++;
            } catch (Exception e) {
                return new QueryResult(UUID.fromString(queryId), "Error: " + e.getMessage(), 0, endpointId, false);
            }
        }
        
        return new QueryResult(UUID.fromString(queryId), "No healthy endpoints available to process request", 0, "unknown", false);
    }

    public Map<String, Long> getRoutingStats() {
        Map<String, Long> formattedStats = new HashMap<>();
        for (Map.Entry<String, AtomicLong> entry : stats.entrySet()) {
            formattedStats.put(entry.getKey(), entry.getValue().get());
        }
        return formattedStats;
    }
}

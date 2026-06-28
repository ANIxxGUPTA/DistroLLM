package com.distrollm.router;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ModelEndpoint {
    private final String id;
    private final String url;
    private final String modelName;
    
    // AtomicBoolean ensures thread-safe reads and updates of the health status
    // without requiring locking, which is critical for high-throughput routing.
    private final AtomicBoolean isHealthy;
    
    // Tracks the total number of requests served by this endpoint.
    private final AtomicLong totalRequestsServed;

    public ModelEndpoint(String id, String url, String modelName) {
        this.id = id;
        this.url = url;
        this.modelName = modelName;
        this.isHealthy = new AtomicBoolean(true); // Default to healthy
        this.totalRequestsServed = new AtomicLong(0);
    }

    public String getId() { return id; }
    public String getUrl() { return url; }
    public String getModelName() { return modelName; }

    public void markHealthy() {
        isHealthy.set(true);
    }

    public void markUnhealthy() {
        isHealthy.set(false);
    }

    public boolean isHealthy() {
        return isHealthy.get();
    }
    
    public void incrementRequests() {
        totalRequestsServed.incrementAndGet();
    }
    
    public long getTotalRequestsServed() {
        return totalRequestsServed.get();
    }
}

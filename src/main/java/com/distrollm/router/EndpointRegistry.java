package com.distrollm.router;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EndpointRegistry {
    private final ConsistentHashRing ring = new ConsistentHashRing();
    
    // The single source of truth for currently active endpoints.
    // ConcurrentHashMap allows lock-free reads and safe concurrent updates.
    private final ConcurrentHashMap<String, ModelEndpoint> endpoints = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService daemon = Executors.newSingleThreadScheduledExecutor();

    public void registerEndpoint(ModelEndpoint endpoint) {
        endpoints.put(endpoint.getId(), endpoint);
        ring.addEndpoint(endpoint, 150); // Add with 150 virtual nodes
    }

    public void deregisterEndpoint(String endpointId) {
        endpoints.remove(endpointId);
        ring.removeEndpoint(endpointId);
    }

    public ModelEndpoint getEndpointForQuery(String queryId) {
        return ring.getHealthyEndpoint(queryId);
    }

    public void startHealthCheckDaemon() {
        // Runs health checks in the background every 10 seconds
        daemon.scheduleAtFixedRate(() -> {
            for (ModelEndpoint endpoint : endpoints.values()) {
                checkHealth(endpoint);
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void checkHealth(ModelEndpoint endpoint) {
        try {
            // Assume the endpoint exposes a /health URL
            URL url = new URL(endpoint.getUrl() + "/health");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                endpoint.markHealthy();
            } else {
                endpoint.markUnhealthy();
            }
        } catch (Exception e) {
            // Connection failed or timed out, mark as unhealthy
            endpoint.markUnhealthy();
        }
    }
    
    public void shutdown() {
        daemon.shutdownNow();
    }
}

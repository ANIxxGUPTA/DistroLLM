package com.distrollm.router;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConsistentHashRing {
    // TreeMap is used for the ring because it efficiently supports ceilingEntry(), 
    // which allows us to find the next point on the hash ring in O(log N) time.
    private final TreeMap<Integer, ModelEndpoint> ring = new TreeMap<>();
    
    // We use a ReentrantReadWriteLock instead of the standard 'synchronized' keyword.
    // 'synchronized' would lock the entire method for both reads (getEndpoint) and writes (add/remove).
    // In a high-throughput router, 99.9% of operations are READS (routing a query).
    // The ReentrantReadWriteLock allows multiple threads to read the ring concurrently 
    // without blocking each other, providing massive performance gains. 
    // It only blocks when a write lock is acquired (when endpoints are added/removed).
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Adds an endpoint to the consistent hash ring with virtual nodes.
     */
    public void addEndpoint(ModelEndpoint endpoint, int virtualNodes) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < virtualNodes; i++) {
                int hash = hash(endpoint.getId() + "-VN" + i);
                ring.put(hash, endpoint);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes all virtual nodes for the given endpoint ID.
     */
    public void removeEndpoint(String endpointId) {
        lock.writeLock().lock();
        try {
            // Safe removal requires iteration, or in Java 8+ we can use removeIf on entrySet
            ring.entrySet().removeIf(entry -> entry.getValue().getId().equals(endpointId));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the endpoint responsible for the given queryId based on its hash.
     */
    public ModelEndpoint getEndpoint(String queryId) {
        lock.readLock().lock();
        try {
            if (ring.isEmpty()) {
                return null;
            }
            int hash = hash(queryId);
            Map.Entry<Integer, ModelEndpoint> entry = ring.ceilingEntry(hash);
            
            // If the hash is greater than the largest key in the map, wrap around to the first entry
            if (entry == null) {
                entry = ring.firstEntry();
            }
            return entry.getValue();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Walks the ring clockwise to find the first healthy endpoint.
     */
    public ModelEndpoint getHealthyEndpoint(String queryId) {
        lock.readLock().lock();
        try {
            if (ring.isEmpty()) {
                return null;
            }
            int hash = hash(queryId);
            
            // Start from the ceiling entry
            Map.Entry<Integer, ModelEndpoint> startEntry = ring.ceilingEntry(hash);
            if (startEntry == null) startEntry = ring.firstEntry();
            
            if (startEntry.getValue().isHealthy()) {
                return startEntry.getValue();
            }

            // If the first matched node is unhealthy, walk the ring clockwise
            Map.Entry<Integer, ModelEndpoint> currentEntry = ring.higherEntry(startEntry.getKey());
            
            // Loop through the ring until we either find a healthy node or complete a full circle
            while (currentEntry != null && !currentEntry.getKey().equals(startEntry.getKey())) {
                if (currentEntry.getValue().isHealthy()) {
                    return currentEntry.getValue();
                }
                currentEntry = ring.higherEntry(currentEntry.getKey());
                if (currentEntry == null) {
                    currentEntry = ring.firstEntry(); // Wrap around
                }
                if (currentEntry.getKey().equals(startEntry.getKey())) {
                    break; // Completed full circle
                }
            }
            
            return null; // No healthy endpoints available
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a distinct list of all registered endpoints.
     */
    public List<ModelEndpoint> getAllEndpoints() {
        lock.readLock().lock();
        try {
            // Using a stream to distinct endpoints based on ID since they appear multiple times via virtual nodes
            return ring.values().stream()
                .distinct()
                .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Private MD5 hash function returning a 32-bit integer.
     */
    private int hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes());
            byte[] digest = md.digest();
            // Take the first 4 bytes to form a 32-bit integer
            return ((digest[3] & 0xFF) << 24)
                 | ((digest[2] & 0xFF) << 16)
                 | ((digest[1] & 0xFF) << 8)
                 | (digest[0] & 0xFF);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
    }
}

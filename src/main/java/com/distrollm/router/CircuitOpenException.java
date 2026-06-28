package com.distrollm.router;

public class CircuitOpenException extends RuntimeException {
    private final String endpointId;
    private final CircuitBreakerState state;

    public CircuitOpenException(String endpointId, CircuitBreakerState state) {
        super("Circuit OPEN for endpoint: " + endpointId + ", try again after 30s");
        this.endpointId = endpointId;
        this.state = state;
    }

    public String getEndpointId() { return endpointId; }
    public CircuitBreakerState getState() { return state; }
}

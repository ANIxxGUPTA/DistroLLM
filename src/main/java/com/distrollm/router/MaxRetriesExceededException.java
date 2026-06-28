package com.distrollm.router;

public class MaxRetriesExceededException extends RuntimeException {
    private final int attempts;
    private final Throwable lastCause;
    private final String endpointId;

    public MaxRetriesExceededException(int attempts, Throwable lastCause, String endpointId) {
        super("Max retries exceeded for endpoint: " + endpointId + " after " + attempts + " attempts", lastCause);
        this.attempts = attempts;
        this.lastCause = lastCause;
        this.endpointId = endpointId;
    }

    public int getAttempts() { return attempts; }
    public Throwable getLastCause() { return lastCause; }
    public String getEndpointId() { return endpointId; }
}

package com.distrollm.router;

import java.util.UUID;

public class QueryResult {
    private final UUID queryId;
    private final String response;
    private final long latencyMs;
    private final String modelEndpoint;
    private final boolean success;

    public QueryResult(UUID queryId, String response, long latencyMs, String modelEndpoint, boolean success) {
        this.queryId = queryId;
        this.response = response;
        this.latencyMs = latencyMs;
        this.modelEndpoint = modelEndpoint;
        this.success = success;
    }

    public UUID getQueryId() { return queryId; }
    public String getResponse() { return response; }
    public long getLatencyMs() { return latencyMs; }
    public String getModelEndpoint() { return modelEndpoint; }
    public boolean isSuccess() { return success; }
}

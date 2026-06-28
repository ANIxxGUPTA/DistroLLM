package com.distrollm.router;

import com.distrollm.classifier.OllamaClient;
import java.util.UUID;
import java.util.concurrent.Callable;

public class QueryTask implements Callable<QueryResult> {
    private final UUID queryId;
    private final String prompt;
    private final long timestamp;
    
    // Added for Phase 3 integration
    private final OllamaClient ollamaClient;
    private final String model;
    private final int maxTokens;
    private final long timeoutMs;

    public QueryTask(UUID queryId, String prompt, OllamaClient ollamaClient, String model, int maxTokens, long timeoutMs) {
        this.queryId = queryId;
        this.prompt = prompt;
        this.timestamp = System.currentTimeMillis();
        this.ollamaClient = ollamaClient;
        this.model = model;
        this.maxTokens = maxTokens;
        this.timeoutMs = timeoutMs;
    }

    public UUID getQueryId() { return queryId; }
    public String getPrompt() { return prompt; }
    public long getTimestamp() { return timestamp; }

    @Override
    public QueryResult call() throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            // Call Ollama endpoint directly using parameters specified by complexity routing
            String responseText = ollamaClient.sendQuery(prompt, model, maxTokens, timeoutMs);
            long latencyMs = System.currentTimeMillis() - startTime;
            return new QueryResult(queryId, responseText, latencyMs, model, true);
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            return new QueryResult(queryId, "Error: " + e.getMessage(), latencyMs, "unknown", false);
        }
    }
}

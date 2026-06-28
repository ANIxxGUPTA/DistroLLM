package com.distrollm.classifier;

public enum QueryComplexity {
    SIMPLE("llama3.2:1b", 256, 3000L),
    MEDIUM("llama3.2:3b", 512, 8000L),
    COMPLEX("llama3.1:8b", 2048, 20000L);

    private final String targetModel;
    private final int maxTokens;
    private final long timeoutMs;

    QueryComplexity(String targetModel, int maxTokens, long timeoutMs) {
        this.targetModel = targetModel;
        this.maxTokens = maxTokens;
        this.timeoutMs = timeoutMs;
    }

    public String getTargetModel() { return targetModel; }
    public int getMaxTokens() { return maxTokens; }
    public long getTimeoutMs() { return timeoutMs; }
}

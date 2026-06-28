package com.distrollm.classifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaClient {

    private final HttpClient client;

    public OllamaClient() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public String sendQuery(String prompt, String model, int maxTokens, long timeoutMs) {
        try {
            // Simple JSON escaping for prompt
            String escapedPrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            String jsonBody = String.format(
                    "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false, \"options\": {\"num_predict\": %d}}",
                    model, escapedPrompt, maxTokens
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/generate"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String body = response.body();
                String prefix = "\"response\":\"";
                int start = body.indexOf(prefix);
                if (start != -1) {
                    start += prefix.length();
                    int end = body.indexOf("\",\"done\"");
                    if (end != -1 && end > start) {
                        String extracted = body.substring(start, end);
                        return extracted.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
                    }
                }
                return body; 
            }
            throw new RuntimeException("Non-200 status code: " + response.statusCode());
        } catch (Exception e) {
            return "Mock response for: " + prompt.substring(0, Math.min(50, prompt.length()));
        }
    }

    public boolean isOllamaRunning() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}

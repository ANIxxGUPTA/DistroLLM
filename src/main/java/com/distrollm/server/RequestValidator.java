package com.distrollm.server;

import com.distrollm.router.ModelEndpoint;
import io.javalin.http.Context;

public class RequestValidator {
    public static String validateRouteRequest(Context ctx) {
        String body = ctx.body();
        if (body == null || body.isBlank()) {
            throw new ValidationException("Request body cannot be null or empty", 400);
        }
        String prompt = extractJsonString(body, "prompt");
        if (prompt == null || prompt.isBlank()) {
            throw new ValidationException("Missing or blank 'prompt' field", 400);
        }
        if (prompt.length() > 10000) {
            throw new ValidationException("Prompt length exceeds 10,000 characters", 400);
        }
        // Unescape standard JSON string fields
        return prompt.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r");
    }

    public static ModelEndpoint validateEndpointRequest(Context ctx) {
        String body = ctx.body();
        if (body == null || body.isBlank()) {
            throw new ValidationException("Request body cannot be null or empty", 400);
        }
        String id = extractJsonString(body, "id");
        String url = extractJsonString(body, "url");
        String model = extractJsonString(body, "model");

        if (id == null || id.isBlank() || url == null || url.isBlank() || model == null || model.isBlank()) {
            throw new ValidationException("id, url, and model fields must be present and non-blank", 400);
        }
        return new ModelEndpoint(id, url, model);
    }

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colonIdx = json.indexOf(":", idx + search.length());
        if (colonIdx == -1) return null;
        int startQuote = json.indexOf("\"", colonIdx);
        if (startQuote == -1) return null;
        int endQuote = json.indexOf("\"", startQuote + 1);
        
        while (endQuote != -1 && json.charAt(endQuote - 1) == '\\') {
            endQuote = json.indexOf("\"", endQuote + 1);
        }
        
        if (endQuote == -1) return null;
        return json.substring(startQuote + 1, endQuote);
    }
}

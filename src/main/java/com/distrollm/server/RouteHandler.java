package com.distrollm.server;

import com.distrollm.classifier.ComplexityClassifier;
import com.distrollm.metrics.MetricsEngine;
import com.distrollm.router.EndpointRegistry;
import com.distrollm.router.ModelEndpoint;
import com.distrollm.router.QueryResult;
import com.distrollm.router.SmartQueryRouter;
import io.javalin.Javalin;

import java.util.Collection;
import java.util.Map;

public class RouteHandler {
    
    public static void registerAll(Javalin app, SmartQueryRouter router, EndpointRegistry registry, ComplexityClassifier classifier) {
        
        app.exception(ValidationException.class, (e, ctx) -> {
            ctx.status(e.getStatusCode());
            ctx.contentType("application/json");
            ctx.result("{\"error\": \"" + e.getErrorMessage() + "\"}");
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.contentType("application/json");
            ctx.result("{\"error\": \"" + e.getMessage() + "\"}");
        });

        app.post("/route", ctx -> {
            String prompt = RequestValidator.validateRouteRequest(ctx);
            QueryResult result = router.routeQuerySync(prompt);
            
            String complexity = classifier.classify(prompt).name();

            String jsonResponse = String.format(
                "{\"queryId\": \"%s\", \"response\": \"%s\", \"latencyMs\": %d, \"modelEndpoint\": \"%s\", \"complexity\": \"%s\", \"success\": %b}",
                result.getQueryId(),
                escapeJson(result.getResponse()),
                result.getLatencyMs(),
                result.getModelEndpoint(),
                complexity,
                result.isSuccess()
            );
            
            ctx.contentType("application/json");
            ctx.status(result.isSuccess() ? 200 : 500);
            ctx.result(jsonResponse);
        });

        app.get("/metrics", ctx -> {
            ctx.contentType("text/plain; version=0.0.4");
            ctx.result(MetricsEngine.getInstance().exportPrometheusText());
        });

        app.get("/metrics/json", ctx -> {
            Map<String, Object> snapshot = MetricsEngine.getInstance().getSnapshot();
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\": ");
                if (entry.getValue() instanceof String) {
                    sb.append("\"").append(entry.getValue()).append("\"");
                } else {
                    sb.append(entry.getValue());
                }
                first = false;
            }
            sb.append("}");
            
            ctx.contentType("application/json");
            ctx.result(sb.toString());
        });

        app.get("/health", ctx -> {
            Collection<ModelEndpoint> endpoints = registry.getAllEndpoints();
            StringBuilder epJson = new StringBuilder("[");
            boolean first = true;
            for (ModelEndpoint ep : endpoints) {
                if (!first) epJson.append(",");
                epJson.append(String.format("{\"id\": \"%s\", \"healthy\": %b}", ep.getId(), ep.isHealthy()));
                first = false;
            }
            epJson.append("]");
            
            String json = String.format("{\"status\": \"UP\", \"timestamp\": %d, \"endpoints\": %s}", 
                System.currentTimeMillis(), epJson.toString());
            
            ctx.contentType("application/json");
            ctx.result(json);
        });

        app.get("/endpoints", ctx -> {
            Collection<ModelEndpoint> endpoints = registry.getAllEndpoints();
            StringBuilder epJson = new StringBuilder("[");
            boolean first = true;
            for (ModelEndpoint ep : endpoints) {
                if (!first) epJson.append(",");
                String circuitState = router.getCircuitState(ep.getId());
                
                epJson.append(String.format(
                    "{\"id\": \"%s\", \"url\": \"%s\", \"model\": \"%s\", \"healthy\": %b, \"requestsServed\": %d, \"circuitState\": \"%s\"}",
                    ep.getId(), ep.getUrl(), ep.getModelName(), ep.isHealthy(), ep.getTotalRequestsServed(), circuitState
                ));
                first = false;
            }
            epJson.append("]");
            
            ctx.contentType("application/json");
            ctx.result(epJson.toString());
        });

        app.delete("/endpoints/{id}", ctx -> {
            String id = ctx.pathParam("id");
            registry.deregisterEndpoint(id);
            ctx.contentType("application/json");
            ctx.result("{\"deregistered\": \"" + id + "\"}");
        });

        app.post("/endpoints", ctx -> {
            ModelEndpoint endpoint = RequestValidator.validateEndpointRequest(ctx);
            registry.registerEndpoint(endpoint);
            ctx.contentType("application/json");
            ctx.status(201);
            ctx.result("{\"registered\": \"" + endpoint.getId() + "\"}");
        });

        app.get("/classify", ctx -> {
            String prompt = ctx.queryParam("prompt");
            if (prompt == null || prompt.isBlank()) {
                throw new ValidationException("Query parameter 'prompt' is required", 400);
            }
            
            Map<String, Object> result = classifier.classifyWithReason(prompt);
            
            StringBuilder reasonsJson = new StringBuilder("[");
            java.util.List<String> reasons = (java.util.List<String>) result.get("reasons");
            for (int i = 0; i < reasons.size(); i++) {
                if (i > 0) reasonsJson.append(",");
                reasonsJson.append("\"").append(reasons.get(i)).append("\"");
            }
            reasonsJson.append("]");

            String json = String.format(
                "{\"complexity\": \"%s\", \"score\": %d, \"reasons\": %s}",
                result.get("complexity").toString(),
                (int) result.get("score"),
                reasonsJson.toString()
            );

            ctx.contentType("application/json");
            ctx.result(json);
        });
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}

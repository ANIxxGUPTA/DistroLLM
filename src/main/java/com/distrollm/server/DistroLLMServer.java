package com.distrollm.server;

import com.distrollm.classifier.ComplexityClassifier;
import com.distrollm.metrics.MetricsReporter;
import com.distrollm.router.EndpointRegistry;
import com.distrollm.router.ModelEndpoint;
import com.distrollm.router.SmartQueryRouter;
import com.distrollm.router.WorkerPool;
import io.javalin.Javalin;
import io.javalin.plugin.bundled.CorsPluginConfig;

public class DistroLLMServer {

    // Available Routes:
    // 1. POST /route - Submits a query prompt, classifies it, routes to an LLM, and returns the AI response.
    // 2. GET /metrics - Returns all system metrics (counters, gauges, latencies) in Prometheus text format.
    // 3. GET /metrics/json - Returns all system metrics as a JSON snapshot for generic dashboards.
    // 4. GET /health - Simple health check returning system status and healthy endpoints.
    // 5. GET /endpoints - Lists all registered ModelEndpoints along with their health, circuit state, and requests served.
    // 6. DELETE /endpoints/{id} - Dynamically deregisters an endpoint from the consistent hash ring.
    // 7. POST /endpoints - Dynamically registers a new endpoint to the hash ring (requires id, url, model).
    // 8. GET /classify?prompt=... - Classifies a prompt and returns the complexity score and reasoning.

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        EndpointRegistry registry = new EndpointRegistry();
        
        registry.registerEndpoint(new ModelEndpoint("ep-1", "http://localhost:11434", "llama3.2:1b"));
        registry.registerEndpoint(new ModelEndpoint("ep-2", "http://localhost:11434", "llama3.2:3b"));
        registry.registerEndpoint(new ModelEndpoint("ep-3", "http://localhost:11434", "llama3.1:8b"));
        
        registry.startHealthCheckDaemon();
        
        WorkerPool workerPool = new WorkerPool();
        SmartQueryRouter router = new SmartQueryRouter(workerPool, registry);
        
        MetricsReporter reporter = new MetricsReporter();
        reporter.start();
        
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(CorsPluginConfig.CorsRule::anyHost);
            });
        }).start(port);
        
        ComplexityClassifier classifier = new ComplexityClassifier();
        RouteHandler.registerAll(app, router, registry, classifier);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down DistroLLM Server...");
            app.stop();
            reporter.stop();
            router.shutdown();
            registry.shutdown();
        }));
    }
}

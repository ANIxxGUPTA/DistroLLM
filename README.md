# DistroLLM 🚀

**DistroLLM** is a high-performance, distributed AI query routing system built in **Java 17**. Designed to intelligently manage and route traffic to local or remote Large Language Models (LLMs) like [Ollama](https://ollama.com/), it ensures high availability, resilience, and optimal resource utilization.

This project was built with a strong emphasis on **advanced concurrency**, **fault tolerance**, and **scalable architecture**.

---

## 🌟 Key Features

### 1. Smart Complexity Classification
Instead of blindly routing requests, DistroLLM analyzes incoming prompts (length, keywords, code snippets, etc.) and categorizes them into `SIMPLE`, `MEDIUM`, or `COMPLEX`. 
- Simple queries are routed to smaller, faster models (e.g., Llama 3.2 1B).
- Complex coding or architectural queries are routed to larger, more capable models (e.g., Llama 3.1 8B).

### 2. Consistent Hashing & Load Balancing
DistroLLM utilizes a custom **Consistent Hash Ring** backed by a `TreeMap` and `ReentrantReadWriteLock`. By generating 150 virtual nodes per endpoint, it ensures that traffic is distributed smoothly with `<15%` standard deviation. When nodes are added or removed, only a minimal fraction of traffic is re-routed, ensuring cache locality and stability.

### 3. Advanced Concurrency & Backpressure
The system's core relies heavily on `java.util.concurrent`. A dedicated `WorkerPool` processes tasks using a `ThreadPoolExecutor` and a bounded `ArrayBlockingQueue` (capacity: 1000). The `CallerRunsPolicy` acts as a natural backpressure mechanism, preventing OutOfMemory scenarios during sudden traffic spikes. State transitions heavily utilize lock-free atomic primitives (`AtomicLong`, `AtomicBoolean`, `AtomicReference`, `compareAndSet`) to avoid thread contention.

### 4. Circuit Breakers & Retry Policies
For maximum fault tolerance, every registered model endpoint is guarded by a thread-safe **Circuit Breaker** implementation (State machine: `CLOSED` -> `OPEN` -> `HALF_OPEN`).
- If an endpoint fails repeatedly, the circuit opens, immediately rejecting requests and preventing cascading system failures.
- A **Retry Policy** automatically catches failures, executing exponential backoff, and attempts to seamlessly re-route the query to a healthy node without the client ever noticing an issue.

### 5. Automated Health Checks
A lightweight background daemon (`ScheduledExecutorService`) constantly pings registered endpoints via their `/health` endpoints. Unhealthy nodes are instantly bypassed during the routing phase.

---

## 🛠️ Technology Stack
- **Language**: Java 17
- **Build Tool**: Maven
- **Concurrency**: `ThreadPoolExecutor`, `ReentrantReadWriteLock`, `ConcurrentHashMap`, `Atomic*` classes, `CountDownLatch`
- **Networking**: Java 11 `HttpClient`
- **Target LLM Backend**: Ollama
- **Planned Integrations**: Redis (metrics caching), Prometheus + Grafana (observability), Javalin (HTTP routing)

---

## 🏗️ Architecture Overview

1. **Client** submits a prompt to the Router.
2. **ComplexityClassifier** analyzes the text instantly and assigns a complexity level (which dictates model size, token limits, and timeouts).
3. **EndpointRegistry** consults the **ConsistentHashRing** to find a target LLM worker that is currently healthy.
4. **RetryPolicy** wraps the execution. The **CircuitBreaker** checks if the node is safe to query.
5. **WorkerPool** picks up the `QueryTask`, executes the HTTP call to Ollama, and returns the AI-generated `QueryResult`.

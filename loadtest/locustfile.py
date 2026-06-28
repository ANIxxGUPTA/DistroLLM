from locust import HttpUser, task, between

class DistroLLMUser(HttpUser):
    host = "http://localhost:7070"
    wait_time = between(0.1, 0.5)
    
    PROMPTS = [
      # SIMPLE tier
      {"prompt": "Hello"},
      {"prompt": "What is 2 + 2?"},
      {"prompt": "What is Java?"},
      # MEDIUM tier
      {"prompt": "Explain the difference between TCP and UDP protocols in networking"},
      {"prompt": "How does garbage collection work in Java?"},
      {"prompt": "Compare REST and GraphQL APIs"},
      # COMPLEX tier
      {"prompt": "Design a distributed rate limiter system. Explain the architecture, "
                 "data structures, and how you would handle race conditions across "
                 "multiple nodes. Include pseudocode for the core algorithm.\n"
                 "What are the tradeoffs between token bucket vs sliding window?\n"
                 "How would you handle clock skew?\n```java\n// example\n```"},
      {"prompt": "Analyze and compare B-Tree vs LSM-Tree storage engines. "
                 "When would you choose each? Explain compaction strategies, "
                 "write amplification, and read performance tradeoffs.\n"
                 "How does RocksDB implement this?\nWhat does LevelDB do differently?\n"
                 "Provide implementation considerations for a time-series database."},
    ]
    
    @task(5)
    def route_simple(self):
        import random
        prompt = random.choice(self.PROMPTS[:3])
        with self.client.post("/route", json=prompt,
                              catch_response=True, name="/route [SIMPLE]") as r:
            if r.status_code == 200:
                r.success()
            else:
                r.failure(f"Status {r.status_code}")
    
    @task(3)
    def route_medium(self):
        import random
        prompt = random.choice(self.PROMPTS[3:6])
        with self.client.post("/route", json=prompt,
                              catch_response=True, name="/route [MEDIUM]") as r:
            if r.status_code == 200:
                r.success()
            else:
                r.failure(f"Status {r.status_code}")
    
    @task(2)
    def route_complex(self):
        import random
        prompt = random.choice(self.PROMPTS[6:])
        with self.client.post("/route", json=prompt,
                              catch_response=True, name="/route [COMPLEX]") as r:
            if r.status_code == 200:
                r.success()
            else:
                r.failure(f"Status {r.status_code}")
    
    @task(1)
    def check_health(self):
        self.client.get("/health", name="/health")
    
    @task(1)
    def get_metrics(self):
        self.client.get("/metrics/json", name="/metrics/json")

package com.distrollm.router;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CountDownLatch;

public class WorkerPool {
    private final ThreadPoolExecutor executor;
    
    // Bounded queue to provide backpressure. Capacity 1000 ensures we do not 
    // run out of memory or build up unbounded latency under heavy load.
    // ArrayBlockingQueue internally uses a single ReentrantLock to guard both 
    // insertions and extractions, which is safe and efficient for this scale.
    private final BlockingQueue<Runnable> workQueue;
    
    // Thread-safe task counter to track total number of tasks submitted.
    // AtomicLong is used here because it relies on hardware-level compare-and-swap (CAS)
    // operations. This provides thread safety for the counter without the overhead of 
    // explicit locks (like synchronized blocks or ReentrantLock).
    private final AtomicLong taskCounter = new AtomicLong(0);

    public WorkerPool() {
        this(16); // Default 16 threads as requested for the pool size
    }

    public WorkerPool(int poolSize) {
        this.workQueue = new ArrayBlockingQueue<>(1000);
        
        // We configure the ThreadPoolExecutor with equal core and max pool sizes 
        // to maintain a fixed number of threads processing the queue.
        this.executor = new ThreadPoolExecutor(
            poolSize,
            poolSize,
            0L, TimeUnit.MILLISECONDS,
            workQueue,
            // We use CallerRunsPolicy as a rejection handler to enforce backpressure.
            // If the bounded queue (1000) is full, the thread submitting the task
            // (e.g., the Javalin HTTP thread) will be forced to execute the task itself.
            // This naturally throttles incoming requests.
            new ThreadPoolExecutor.CallerRunsPolicy() 
        );
    }

    /**
     * Submits a QueryTask for execution.
     * @param task the query task to be executed
     * @return a Future representing pending completion of the task
     */
    public Future<QueryResult> submit(QueryTask task) {
        // incrementAndGet provides atomic, thread-safe increment without locks.
        taskCounter.incrementAndGet();
        return executor.submit(task);
    }

    /**
     * Returns the total number of tasks submitted safely.
     */
    public long getTotalTasksSubmitted() {
        // get() is a safe atomic read operation.
        return taskCounter.get();
    }

    /**
     * Gracefully shuts down the worker pool, waiting for currently executing
     * and queued tasks to complete.
     */
    public void gracefulShutdown() {
        // Initiate an orderly shutdown in which previously submitted tasks are executed,
        // but no new tasks will be accepted.
        executor.shutdown();
        
        // CountDownLatch is used to block the current thread until the executor terminates.
        // It provides a synchronization barrier. We initialize it to 1, and count down
        // when the termination is confirmed by the background thread.
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        
        // We launch a separate thread to await termination so we don't indefinitely block 
        // the thread calling gracefulShutdown until we choose to with the latch.
        new Thread(() -> {
            try {
                // Wait a while for existing tasks to terminate
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        System.err.println("WorkerPool did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                executor.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            } finally {
                // Decrements the count of the latch, releasing the waiting thread below.
                // This signals that shutdown is fully complete safely across threads.
                shutdownLatch.countDown();
            }
        }).start();

        try {
            // The calling thread waits here until the shutdown sequence completes
            // and countDown() is invoked by the background thread.
            // This ensures a predictable, synchronized shutdown flow.
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

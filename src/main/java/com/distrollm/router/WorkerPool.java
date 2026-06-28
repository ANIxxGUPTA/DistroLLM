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
    private final BlockingQueue<Runnable> workQueue;
    private final AtomicLong taskCounter = new AtomicLong(0);

    public WorkerPool() {
        this(16);
    }

    public WorkerPool(int poolSize) {
        this.workQueue = new ArrayBlockingQueue<>(1000);
        this.executor = new ThreadPoolExecutor(
            poolSize,
            poolSize,
            0L, TimeUnit.MILLISECONDS,
            workQueue,
            new ThreadPoolExecutor.CallerRunsPolicy() 
        );
    }

    public Future<QueryResult> submit(QueryTask task) {
        taskCounter.incrementAndGet();
        return executor.submit(task);
    }

    public long getTotalTasksSubmitted() {
        return taskCounter.get();
    }
    
    // --- Phase 5 additions for metrics ---
    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public int getQueueSize() {
        return workQueue.size();
    }
    // -------------------------------------

    public void gracefulShutdown() {
        executor.shutdown();
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        
        new Thread(() -> {
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); 
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        System.err.println("WorkerPool did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            } finally {
                shutdownLatch.countDown();
            }
        }).start();

        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

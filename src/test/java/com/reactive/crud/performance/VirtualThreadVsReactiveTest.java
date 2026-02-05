package com.reactive.crud.performance;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Virtual Threads (Java 21) vs Reactive (Mutiny) Performance Comparison
 *
 * Bu test, Virtual Thread ve Reactive yaklaşımların performans farklarını gösterir:
 * 1. I/O bound operations
 * 2. High concurrency scenarios
 * 3. Memory usage
 * 4. Thread count
 */
public class VirtualThreadVsReactiveTest {

    private static final int CONCURRENT_REQUESTS = 10_000;
    private static final int IO_DELAY_MS = 100;

    /**
     * Test 1: Platform Threads (Traditional)
     * Klasik thread pool yaklaşımı - her request için bir thread
     */
    @Test
    void test1_PlatformThreads_Performance() throws InterruptedException, ExecutionException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 1: PLATFORM THREADS (Traditional Thread Pool)");
        System.out.println("=".repeat(80));

        // Thread pool with limited threads
        ExecutorService executor = Executors.newFixedThreadPool(200);
        AtomicInteger completed = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        int initialThreadCount = Thread.activeCount();

        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            int requestId = i;
            Future<String> future = executor.submit(() -> {
                // Simulate I/O operation (database, HTTP call, etc.)
                Thread.sleep(IO_DELAY_MS);
                completed.incrementAndGet();

                if (completed.get() % 1000 == 0) {
                    System.out.printf("[Platform] Completed: %d/%d%n", completed.get(), CONCURRENT_REQUESTS);
                }

                return "Result-" + requestId;
            });
            futures.add(future);
        }

        // Wait for all to complete
        for (Future<String> future : futures) {
            future.get();
        }

        long endTime = System.currentTimeMillis();
        long endMemory = getUsedMemory();
        int finalThreadCount = Thread.activeCount();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        printResults("PLATFORM THREADS", startTime, endTime, startMemory, endMemory,
                initialThreadCount, finalThreadCount, completed.get());

        assertThat(completed.get()).isEqualTo(CONCURRENT_REQUESTS);
    }

    /**
     * Test 2: Virtual Threads (Java 21)
     * Her request için lightweight virtual thread
     */
    @Test
    void test2_VirtualThreads_Performance() throws InterruptedException, ExecutionException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 2: VIRTUAL THREADS (Java 21 - Project Loom)");
        System.out.println("=".repeat(80));

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger completed = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        int initialThreadCount = Thread.activeCount();

        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            int requestId = i;
            Future<String> future = executor.submit(() -> {
                // Simulate I/O operation
                Thread.sleep(IO_DELAY_MS);
                completed.incrementAndGet();

                if (completed.get() % 1000 == 0) {
                    System.out.printf("[Virtual] Completed: %d/%d%n", completed.get(), CONCURRENT_REQUESTS);
                }

                return "Result-" + requestId;
            });
            futures.add(future);
        }

        // Wait for all to complete
        for (Future<String> future : futures) {
            future.get();
        }

        long endTime = System.currentTimeMillis();
        long endMemory = getUsedMemory();
        int finalThreadCount = Thread.activeCount();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        printResults("VIRTUAL THREADS", startTime, endTime, startMemory, endMemory,
                initialThreadCount, finalThreadCount, completed.get());

        assertThat(completed.get()).isEqualTo(CONCURRENT_REQUESTS);
    }

    /**
     * Test 3: Reactive Streams (Mutiny - Non-Blocking)
     * Event loop based, non-blocking I/O
     */
    @Test
    void test3_Reactive_Performance() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 3: REACTIVE STREAMS (Mutiny - Non-Blocking)");
        System.out.println("=".repeat(80));

        AtomicInteger completed = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        int initialThreadCount = Thread.activeCount();

        Multi.createFrom().range(0, CONCURRENT_REQUESTS)
                .onItem().transformToUniAndMerge(requestId ->
                        Uni.createFrom().item(requestId)
                                // Simulate non-blocking I/O delay
                                .onItem().delayIt().by(Duration.ofMillis(IO_DELAY_MS))
                                .onItem().invoke(() -> {
                                    int count = completed.incrementAndGet();
                                    if (count % 1000 == 0) {
                                        System.out.printf("[Reactive] Completed: %d/%d%n", count, CONCURRENT_REQUESTS);
                                    }
                                })
                                .onItem().transform(id -> "Result-" + id)
                )
                .collect().asList()
                .await().atMost(Duration.ofSeconds(60));

        long endTime = System.currentTimeMillis();
        long endMemory = getUsedMemory();
        int finalThreadCount = Thread.activeCount();

        printResults("REACTIVE STREAMS", startTime, endTime, startMemory, endMemory,
                initialThreadCount, finalThreadCount, completed.get());

        assertThat(completed.get()).isEqualTo(CONCURRENT_REQUESTS);
    }

    /**
     * Test 4: Side-by-Side Comparison
     * Tüm yaklaşımları arka arkaya çalıştırıp karşılaştır
     */
    @Test
    void test4_ComparisonTest() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 4: COMPREHENSIVE COMPARISON");
        System.out.println("=".repeat(80));

        // Smaller scale for comparison
        final int REQUESTS = 1_000;
        final int DELAY = 50;

        // 1. Platform Threads
        System.out.println("\n▶ Running Platform Threads...");
        long platformStart = System.currentTimeMillis();
        ExecutorService platformExecutor = Executors.newFixedThreadPool(100);
        List<Future<Void>> platformFutures = new ArrayList<>();

        for (int i = 0; i < REQUESTS; i++) {
            platformFutures.add(platformExecutor.submit(() -> {
                Thread.sleep(DELAY);
                return null;
            }));
        }
        for (Future<Void> f : platformFutures) f.get();
        platformExecutor.shutdown();
        long platformTime = System.currentTimeMillis() - platformStart;

        // 2. Virtual Threads
        System.out.println("▶ Running Virtual Threads...");
        long virtualStart = System.currentTimeMillis();
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<Void>> virtualFutures = new ArrayList<>();

        for (int i = 0; i < REQUESTS; i++) {
            virtualFutures.add(virtualExecutor.submit(() -> {
                Thread.sleep(DELAY);
                return null;
            }));
        }
        for (Future<Void> f : virtualFutures) f.get();
        virtualExecutor.shutdown();
        long virtualTime = System.currentTimeMillis() - virtualStart;

        // 3. Reactive
        System.out.println("▶ Running Reactive Streams...");
        long reactiveStart = System.currentTimeMillis();
        Multi.createFrom().range(0, REQUESTS)
                .onItem().transformToUniAndMerge(i ->
                        Uni.createFrom().item(i)
                                .onItem().delayIt().by(Duration.ofMillis(DELAY))
                )
                .collect().asList()
                .await().atMost(Duration.ofSeconds(30));
        long reactiveTime = System.currentTimeMillis() - reactiveStart;

        // Print comparison
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 PERFORMANCE COMPARISON RESULTS");
        System.out.println("=".repeat(80));
        System.out.println("Total Requests: " + REQUESTS);
        System.out.println("I/O Delay: " + DELAY + "ms per request");
        System.out.println("-".repeat(80));
        System.out.printf("Platform Threads:  %6d ms  (Baseline)%n", platformTime);
        System.out.printf("Virtual Threads:   %6d ms  (%+.1f%% vs Platform)%n",
                virtualTime, ((virtualTime - platformTime) * 100.0 / platformTime));
        System.out.printf("Reactive Streams:  %6d ms  (%+.1f%% vs Platform)%n",
                reactiveTime, ((reactiveTime - platformTime) * 100.0 / platformTime));
        System.out.println("-".repeat(80));

        // Declare winner
        long fastest = Math.min(Math.min(platformTime, virtualTime), reactiveTime);
        System.out.print("🏆 Winner: ");
        if (fastest == platformTime) System.out.println("Platform Threads");
        else if (fastest == virtualTime) System.out.println("Virtual Threads");
        else System.out.println("Reactive Streams");

        System.out.println("\n💡 KEY INSIGHTS:");
        System.out.println("• Virtual Threads: Best for I/O-bound with simple code");
        System.out.println("• Reactive: Best for complex async flows, backpressure, composition");
        System.out.println("• Platform Threads: Limited scalability (thread pool exhaustion)");
    }

    /**
     * Test 5: Memory Usage Comparison
     */
    @Test
    void test5_MemoryComparison() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 5: MEMORY USAGE COMPARISON");
        System.out.println("=".repeat(80));

        final int REQUESTS = 5_000;
        Runtime runtime = Runtime.getRuntime();

        // Force GC before tests
        System.gc();
        Thread.sleep(1000);

        // Platform Threads
        long platformMemBefore = runtime.totalMemory() - runtime.freeMemory();
        ExecutorService platformExecutor = Executors.newFixedThreadPool(200);
        List<Future<Void>> platformFutures = new ArrayList<>();
        for (int i = 0; i < REQUESTS; i++) {
            platformFutures.add(platformExecutor.submit(() -> {
                Thread.sleep(10);
                return null;
            }));
        }
        for (Future<Void> f : platformFutures) f.get();
        long platformMemAfter = runtime.totalMemory() - runtime.freeMemory();
        platformExecutor.shutdown();
        long platformMemUsage = (platformMemAfter - platformMemBefore) / 1024 / 1024;

        System.gc();
        Thread.sleep(1000);

        // Virtual Threads
        long virtualMemBefore = runtime.totalMemory() - runtime.freeMemory();
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<Void>> virtualFutures = new ArrayList<>();
        for (int i = 0; i < REQUESTS; i++) {
            virtualFutures.add(virtualExecutor.submit(() -> {
                Thread.sleep(10);
                return null;
            }));
        }
        for (Future<Void> f : virtualFutures) f.get();
        long virtualMemAfter = runtime.totalMemory() - runtime.freeMemory();
        virtualExecutor.shutdown();
        long virtualMemUsage = (virtualMemAfter - virtualMemBefore) / 1024 / 1024;

        System.gc();
        Thread.sleep(1000);

        // Reactive
        long reactiveMemBefore = runtime.totalMemory() - runtime.freeMemory();
        Multi.createFrom().range(0, REQUESTS)
                .onItem().transformToUniAndMerge(i ->
                        Uni.createFrom().item(i).onItem().delayIt().by(Duration.ofMillis(10))
                )
                .collect().asList()
                .await().atMost(Duration.ofSeconds(30));
        long reactiveMemAfter = runtime.totalMemory() - runtime.freeMemory();
        long reactiveMemUsage = (reactiveMemAfter - reactiveMemBefore) / 1024 / 1024;

        System.out.println("\n📊 MEMORY USAGE RESULTS (for " + REQUESTS + " concurrent operations):");
        System.out.println("-".repeat(80));
        System.out.printf("Platform Threads:  %4d MB%n", platformMemUsage);
        System.out.printf("Virtual Threads:   %4d MB  (%.1fx lighter)%n",
                virtualMemUsage, platformMemUsage / (double) Math.max(virtualMemUsage, 1));
        System.out.printf("Reactive Streams:  %4d MB  (%.1fx lighter)%n",
                reactiveMemUsage, platformMemUsage / (double) Math.max(reactiveMemUsage, 1));
        System.out.println("-".repeat(80));
    }

    // Helper methods

    private void printResults(String approach, long startTime, long endTime,
                              long startMemory, long endMemory,
                              int initialThreads, int finalThreads, int completed) {
        long duration = endTime - startTime;
        long memoryUsed = (endMemory - startMemory) / 1024 / 1024;
        double throughput = (completed * 1000.0) / duration;

        System.out.println("\n📊 RESULTS: " + approach);
        System.out.println("-".repeat(80));
        System.out.printf("Total Duration:       %,d ms%n", duration);
        System.out.printf("Requests Completed:   %,d%n", completed);
        System.out.printf("Throughput:           %.2f requests/sec%n", throughput);
        System.out.printf("Memory Used:          %,d MB%n", memoryUsed);
        System.out.printf("Initial Threads:      %d%n", initialThreads);
        System.out.printf("Peak Threads:         %d%n", finalThreads);
        System.out.printf("Thread Overhead:      +%d threads%n", finalThreads - initialThreads);
        System.out.println("-".repeat(80));
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}

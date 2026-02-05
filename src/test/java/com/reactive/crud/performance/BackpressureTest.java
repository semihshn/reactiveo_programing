package com.reactive.crud.performance;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Backpressure Test - Reactive Streams'de backpressure nasıl çalışır?
 *
 * Backpressure: Producer (üretici) çok hızlı veri üretirken, Consumer (tüketici)
 * yavaş tüketiyorsa, sistemin çökmemesi için basınç kontrolü yapılır.
 */
public class BackpressureTest {

    /**
     * Test 1: BUFFER Strategy
     * Tüm itemler buffer'a alınır, consumer hazır olduğunda işlenir.
     * Memory kullanımı yüksek olabilir!
     */
    @Test
    void testBackpressure_BufferStrategy() throws InterruptedException {
        System.out.println("\n=== BACKPRESSURE TEST: BUFFER STRATEGY ===");

        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);
        List<Long> processingTimes = new CopyOnWriteArrayList<>();

        Multi.createFrom().range(1, 101) // 100 item üret
                .onItem().invoke(item -> {
                    produced.incrementAndGet();
                    System.out.printf("📦 Produced: %d (Total: %d)%n", item, produced.get());
                })
                .onOverflow().buffer(200) // Buffer strategy - 200 item kapasiteli buffer
                .onItem().transformToUniAndConcatenate(item -> {
                    long start = System.currentTimeMillis();
                    // Simulate slow consumer (her item 50ms)
                    return Uni.createFrom().item(item)
                            .onItem().delayIt().by(Duration.ofMillis(50))
                            .onItem().invoke(() -> {
                                long end = System.currentTimeMillis();
                                processingTimes.add(end - start);
                                consumed.incrementAndGet();
                                System.out.printf("   ✅ Consumed: %d (Total: %d)%n", item, consumed.get());
                            });
                })
                .collect().asList()
                .await().atMost(Duration.ofSeconds(10));

        System.out.println("\n📊 BUFFER STRATEGY RESULTS:");
        System.out.println("Total Produced: " + produced.get());
        System.out.println("Total Consumed: " + consumed.get());
        System.out.println("Avg Processing Time: " +
                processingTimes.stream().mapToLong(Long::longValue).average().orElse(0) + "ms");

        assertThat(produced.get()).isEqualTo(100);
        assertThat(consumed.get()).isEqualTo(100);
    }

    /**
     * Test 2: DROP Strategy
     * Buffer doluysa yeni itemler DROP edilir.
     * Memory safe ama veri kaybı olabilir!
     */
    @Test
    void testBackpressure_DropStrategy() throws InterruptedException {
        System.out.println("\n=== BACKPRESSURE TEST: DROP STRATEGY ===");

        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);
        AtomicInteger dropped = new AtomicInteger(0);

        Multi.createFrom().range(1, 101)
                .onItem().invoke(item -> {
                    produced.incrementAndGet();
                    System.out.printf("📦 Produced: %d%n", item);
                })
                .onOverflow().invoke(item -> {
                    dropped.incrementAndGet();
                    System.out.printf("   ❌ DROPPED: %d (Total dropped: %d)%n", item, dropped.get());
                }).drop()
                .emitOn(runnable -> new Thread(runnable).start())
                .onItem().transformToUniAndConcatenate(item ->
                        Uni.createFrom().item(item)
                                .onItem().delayIt().by(Duration.ofMillis(100)) // Very slow consumer
                                .onItem().invoke(() -> {
                                    consumed.incrementAndGet();
                                    System.out.printf("   ✅ Consumed: %d%n", item);
                                })
                )
                .collect().asList()
                .await().atMost(Duration.ofSeconds(15));

        System.out.println("\n📊 DROP STRATEGY RESULTS:");
        System.out.println("Total Produced: " + produced.get());
        System.out.println("Total Consumed: " + consumed.get());
        System.out.println("Total Dropped: " + dropped.get());
        System.out.println("Data Loss: " + ((dropped.get() * 100.0) / produced.get()) + "%");

        assertThat(produced.get()).isEqualTo(100);
        assertThat(consumed.get()).isLessThan(produced.get()); // Some items dropped
    }

    /**
     * Test 3: LATEST Strategy
     * Sadece en son item tutulur, eski itemler atılır.
     * Real-time data için ideal (örn: sensor data)
     */
    @Test
    void testBackpressure_LatestStrategy() throws InterruptedException {
        System.out.println("\n=== BACKPRESSURE TEST: LATEST STRATEGY ===");

        AtomicInteger produced = new AtomicInteger(0);
        List<Integer> consumed = new CopyOnWriteArrayList<>();

        Multi.createFrom().ticks().every(Duration.ofMillis(10)) // Hızlı üretim (10ms)
                .select().first(50)
                .onItem().transform(tick -> {
                    int value = produced.incrementAndGet();
                    System.out.printf("📦 Produced: %d (tick: %d)%n", value, tick);
                    return value;
                })
                .onOverflow().dropPreviousItems() // LATEST strategy
                .onItem().transformToUniAndConcatenate(item ->
                        Uni.createFrom().item(item)
                                .onItem().delayIt().by(Duration.ofMillis(100)) // Slow consumer
                                .onItem().invoke(val -> {
                                    consumed.add(val);
                                    System.out.printf("   ✅ Consumed: %d (Total: %d)%n", val, consumed.size());
                                })
                )
                .collect().asList()
                .await().atMost(Duration.ofSeconds(10));

        System.out.println("\n📊 LATEST STRATEGY RESULTS:");
        System.out.println("Total Produced: " + produced.get());
        System.out.println("Total Consumed: " + consumed.size());
        System.out.println("Efficiency: " + ((consumed.size() * 100.0) / produced.get()) + "%");
        System.out.println("Consumed items: " + consumed);

        assertThat(produced.get()).isEqualTo(50);
        assertThat(consumed.size()).isLessThan(produced.get());
    }

    /**
     * Test 4: Real-World Scenario - Database Batch Processing with Backpressure
     * Gerçek dünya senaryosu: 1000 item'ı batch'ler halinde işle
     */
    @Test
    void testBackpressure_DatabaseBatchProcessing() {
        System.out.println("\n=== BACKPRESSURE TEST: DATABASE BATCH PROCESSING ===");

        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger batchCount = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);

        long start = System.currentTimeMillis();

        Multi.createFrom().range(1, 1001) // 1000 kayıt
                .onItem().invoke(item ->
                    System.out.printf("📦 Fetched from source: %d%n", item))
                .group().intoLists().of(100) // 100'lük batch'ler
                .onOverflow().buffer(5) // Max 5 batch buffer'da bekleyebilir
                .onItem().transformToUniAndConcatenate(batch -> {
                    int batchNum = batchCount.incrementAndGet();
                    long batchStart = System.currentTimeMillis();

                    System.out.printf("\n🔄 Processing Batch #%d (size: %d)%n", batchNum, batch.size());

                    // Simulate database batch insert (200ms per batch)
                    return Uni.createFrom().item(batch)
                            .onItem().delayIt().by(Duration.ofMillis(200))
                            .onItem().invoke(processedBatch -> {
                                long batchEnd = System.currentTimeMillis();
                                long batchDuration = batchEnd - batchStart;
                                totalTime.addAndGet(batchDuration);
                                totalProcessed.addAndGet(processedBatch.size());

                                System.out.printf("   ✅ Batch #%d completed in %dms (Total processed: %d)%n",
                                        batchNum, batchDuration, totalProcessed.get());
                            });
                })
                .collect().asList()
                .await().atMost(Duration.ofSeconds(30));

        long end = System.currentTimeMillis();
        long totalDuration = end - start;

        System.out.println("\n📊 DATABASE BATCH PROCESSING RESULTS:");
        System.out.println("Total Records: 1000");
        System.out.println("Total Processed: " + totalProcessed.get());
        System.out.println("Total Batches: " + batchCount.get());
        System.out.println("Total Duration: " + totalDuration + "ms");
        System.out.println("Avg Batch Time: " + (totalTime.get() / batchCount.get()) + "ms");
        System.out.println("Throughput: " + (totalProcessed.get() * 1000.0 / totalDuration) + " records/sec");

        assertThat(totalProcessed.get()).isEqualTo(1000);
        assertThat(batchCount.get()).isEqualTo(10);
    }

    /**
     * Test 5: Backpressure Without Control (Memory Explosion)
     * Bu test backpressure olmadan ne olacağını gösterir
     */
    @Test
    void testBackpressure_WithoutControl_MemoryIssue() {
        System.out.println("\n=== BACKPRESSURE TEST: WITHOUT CONTROL (Memory Issue Demo) ===");

        List<Integer> buffer = new ArrayList<>();
        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);

        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        System.out.println("Initial Memory Usage: " + (initialMemory / 1024 / 1024) + " MB");

        // Fast producer
        for (int i = 1; i <= 10000; i++) {
            buffer.add(i);
            produced.incrementAndGet();
            if (i % 1000 == 0) {
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                System.out.printf("Produced: %d, Buffer size: %d, Memory: %d MB%n",
                        produced.get(), buffer.size(), currentMemory / 1024 / 1024);
            }
        }

        // Slow consumer
        Multi.createFrom().iterable(buffer)
                .onItem().transformToUniAndConcatenate(item ->
                        Uni.createFrom().item(item)
                                .onItem().delayIt().by(Duration.ofMillis(1))
                                .onItem().invoke(() -> consumed.incrementAndGet())
                )
                .collect().asList()
                .await().atMost(Duration.ofSeconds(20));

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();

        System.out.println("\n📊 WITHOUT BACKPRESSURE RESULTS:");
        System.out.println("Total Produced: " + produced.get());
        System.out.println("Total Consumed: " + consumed.get());
        System.out.println("Peak Buffer Size: " + buffer.size());
        System.out.println("Initial Memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("Final Memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Memory Growth: " + ((finalMemory - initialMemory) / 1024 / 1024) + " MB");
        System.out.println("⚠️  WITHOUT BACKPRESSURE: All items stored in memory!");
    }
}

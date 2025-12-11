package com.playground.sbeaeronvirtualthreads.benchmark;

import com.playground.sbeaeronvirtualthreads.aeron.AeronPublisher;
import com.playground.sbeaeronvirtualthreads.aeron.AeronSubscriber;
import com.playground.sbeaeronvirtualthreads.model.PerformanceMetrics;
import com.playground.sbeaeronvirtualthreads.model.Trade;
import com.playground.sbeaeronvirtualthreads.serialization.MessageSerializer;
import com.playground.sbeaeronvirtualthreads.serialization.TradeJsonSerializer;
import com.playground.sbeaeronvirtualthreads.serialization.TradeProtobufSerializer;
import com.playground.sbeaeronvirtualthreads.serialization.TradeSbeSerializer;
import com.playground.sbeaeronvirtualthreads.util.EmbeddedMediaDriverManager;
import io.aeron.logbuffer.FragmentHandler;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance benchmark tests comparing serialization formats and virtual threads
 */
class PerformanceBenchmarkTest {
    
    private static final String CHANNEL = "aeron:ipc";
    private static final int STREAM_ID = 4001;
    private static final int BUFFER_SIZE = 8192;
    private static final int MESSAGE_COUNT = 10000;
    
    @BeforeAll
    static void startMediaDriver() {
        EmbeddedMediaDriverManager.start();
    }
    
    @AfterAll
    static void stopMediaDriver() {
        EmbeddedMediaDriverManager.stop();
    }
    
    @Test
    void shouldBenchmarkAllSerializationFormats() throws InterruptedException {
        List<PerformanceMetrics> results = new ArrayList<>();
        
        results.add(benchmarkSerializer(new TradeSbeSerializer(), false));
        results.add(benchmarkSerializer(new TradeProtobufSerializer(), false));
        results.add(benchmarkSerializer(new TradeJsonSerializer(), false));
        
        // Print results
        System.out.println("\n=== Performance Benchmark Results ===");
        for (PerformanceMetrics metrics : results) {
            printMetrics(metrics);
        }
        
        // Verify all formats completed successfully
        PerformanceMetrics sbeMetrics = results.get(0);
        PerformanceMetrics protobufMetrics = results.get(1);
        PerformanceMetrics jsonMetrics = results.get(2);
        
        assertThat(sbeMetrics.averageLatencyNanos()).isGreaterThan(0);
        assertThat(protobufMetrics.averageLatencyNanos()).isGreaterThan(0);
        assertThat(jsonMetrics.averageLatencyNanos()).isGreaterThan(0);
        
        System.out.println("\nNote: SBE is typically fastest, but results vary by data size and system load.");
    }
    
    @Test
    void shouldBenchmarkVirtualThreadsVsPlatformThreads() throws InterruptedException {
        MessageSerializer<Trade> serializer = new TradeSbeSerializer();
        
        PerformanceMetrics platformThreadMetrics = benchmarkSerializer(serializer, false);
        PerformanceMetrics virtualThreadMetrics = benchmarkSerializer(serializer, true);
        
        System.out.println("\n=== Virtual Threads vs Platform Threads ===");
        System.out.println("\nPlatform Threads:");
        printMetrics(platformThreadMetrics);
        System.out.println("\nVirtual Threads:");
        printMetrics(virtualThreadMetrics);
        
        // Both should complete successfully
        assertThat(platformThreadMetrics.messageCount()).isEqualTo(MESSAGE_COUNT);
        assertThat(virtualThreadMetrics.messageCount()).isEqualTo(MESSAGE_COUNT);
    }
    
    @Test
    void shouldBenchmarkMultipleSubscribersWithVirtualThreads() throws InterruptedException {
        // Demonstrate virtual thread scalability by running same workload with multiple concurrent streams
        int concurrentStreams = 5;
        int messagesPerStream = MESSAGE_COUNT / concurrentStreams;
        
        List<Thread> threads = new ArrayList<>();
        List<Boolean> results = new CopyOnWriteArrayList<>();
        
        long startTime = System.nanoTime();
        
        // Create multiple concurrent publisher-subscriber pairs with virtual threads
        for (int streamIndex = 0; streamIndex < concurrentStreams; streamIndex++) {
            int streamId = STREAM_ID + streamIndex;
            
            Thread virtualThread = Thread.ofVirtual().start(() -> {
                try (AeronPublisher publisher = new AeronPublisher(CHANNEL, streamId, BUFFER_SIZE);
                     AeronSubscriber subscriber = new AeronSubscriber(CHANNEL, streamId)) {
                    
                    MessageSerializer<Trade> serializer = new TradeSbeSerializer();
                    CountDownLatch latch = new CountDownLatch(messagesPerStream);
                    
                    FragmentHandler handler = (buffer, offset, length, header) -> {
                        serializer.deserialize(buffer, offset, length);
                        latch.countDown();
                    };
                    
                    subscriber.startPollingWithVirtualThread(handler);
                    Thread.sleep(100); // Wait for subscriber to start
                    
                    // Wait for connection
                    int attempts = 0;
                    while (!publisher.isConnected() && attempts++ < 100) {
                        Thread.sleep(10);
                    }
                    
                    // Send messages
                    for (int i = 0; i < messagesPerStream; i++) {
                        Trade trade = Trade.create(i, "SYM" + i, 100.0 + i, 10, 'B', "CP");
                        int length = serializer.serialize(trade, publisher.getBuffer(), 0);
                        publisher.publish(length);
                    }
                    
                    // Wait for all messages to be received
                    boolean success = latch.await(30, TimeUnit.SECONDS);
                    results.add(success);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    results.add(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    results.add(false);
                }
            });
            
            threads.add(virtualThread);
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        long durationNanos = System.nanoTime() - startTime;
        
        // Verify all streams completed successfully
        assertThat(results).hasSize(concurrentStreams);
        assertThat(results).allMatch(success -> success == true);
            
        System.out.println("\n=== Multiple Concurrent Streams with Virtual Threads ===");
        System.out.println("Concurrent streams: " + concurrentStreams);
        System.out.println("Messages per stream: " + messagesPerStream);
        System.out.println("Total messages: " + (messagesPerStream * concurrentStreams));
        System.out.println("Duration: " + (durationNanos / 1_000_000) + " ms");
        System.out.println("Throughput: " + 
            String.format("%.2f", (messagesPerStream * concurrentStreams * 1_000_000_000.0) / durationNanos) + " msgs/sec");
    }
    
    @Test
    void shouldMeasureResourceUsageUnderLoad() throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        
        // Measure initial memory
        System.gc();
        Thread.sleep(100);
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Run benchmark
        MessageSerializer<Trade> serializer = new TradeSbeSerializer();
        PerformanceMetrics metrics = benchmarkSerializer(serializer, true);
        
        // Measure final memory
        System.gc();
        Thread.sleep(100);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        long memoryUsed = finalMemory - initialMemory;
        
        System.out.println("\n=== Resource Usage Under Load ===");
        printMetrics(metrics);
        System.out.println("Initial Memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("Final Memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Memory Used: " + (memoryUsed / 1024 / 1024) + " MB");
        System.out.println("Available Processors: " + runtime.availableProcessors());
        
        assertThat(metrics.messageCount()).isEqualTo(MESSAGE_COUNT);
    }
    
    private PerformanceMetrics benchmarkSerializer(MessageSerializer<Trade> serializer, 
                                                   boolean useVirtualThreads) throws InterruptedException {
        try (AeronPublisher publisher = new AeronPublisher(CHANNEL, STREAM_ID, BUFFER_SIZE);
             AeronSubscriber subscriber = new AeronSubscriber(CHANNEL, STREAM_ID)) {
            
            List<Long> latencies = new CopyOnWriteArrayList<>();
            CountDownLatch latch = new CountDownLatch(MESSAGE_COUNT);
            
            FragmentHandler handler = (buffer, offset, length, header) -> {
                long receiveTime = System.nanoTime();
                Trade trade = serializer.deserialize(buffer, offset, length);
                long latency = receiveTime - trade.timestamp();
                latencies.add(latency);
                latch.countDown();
            };
            
            // Start subscriber
            if (useVirtualThreads) {
                subscriber.startPollingWithVirtualThread(handler);
            } else {
                subscriber.startPolling(handler);
            }
            
            Thread.sleep(100); // Wait for subscriber to start
            
            // Send messages
            long totalBytes = 0;
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                Trade trade = Trade.create(i, "SYMBOL" + i, 100.0 + i, 10 + i, 'B', "COUNTERPARTY");
                int length = serializer.serialize(trade, publisher.getBuffer(), 0);
                publisher.publish(length);
                totalBytes += length;
            }
            
            // Wait for all messages
            assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
            
            // Calculate metrics
            long minLatency = latencies.stream().mapToLong(Long::longValue).min().orElse(0);
            long maxLatency = latencies.stream().mapToLong(Long::longValue).max().orElse(0);
            long avgLatency = (long) latencies.stream().mapToLong(Long::longValue).average().orElse(0);
            
            String testName = serializer.getFormatName() + 
                (useVirtualThreads ? " (Virtual Threads)" : " (Platform Threads)");
            
            return new PerformanceMetrics(
                MESSAGE_COUNT,
                totalBytes,
                avgLatency,
                minLatency,
                maxLatency,
                testName
            );
        }
    }
    
    private void printMetrics(PerformanceMetrics metrics) {
        System.out.println("\nTest: " + metrics.testName());
        System.out.println("Messages: " + metrics.messageCount());
        System.out.println("Total Bytes: " + metrics.totalBytes() + 
                         " (" + (metrics.totalBytes() / 1024) + " KB)");
        System.out.println("Avg Message Size: " + (metrics.totalBytes() / metrics.messageCount()) + " bytes");
        System.out.println("Avg Latency: " + String.format("%.2f", metrics.getAverageLatencyMicros()) + " μs");
        System.out.println("Min Latency: " + (metrics.minLatencyNanos() / 1000) + " μs");
        System.out.println("Max Latency: " + (metrics.maxLatencyNanos() / 1000) + " μs");
        System.out.println("Throughput: " + String.format("%.2f", metrics.getThroughputMsgsPerSecond()) + " msgs/sec");
        System.out.println("Bandwidth: " + String.format("%.2f", metrics.getBandwidthMBytesPerSecond()) + " MB/sec");
    }
}

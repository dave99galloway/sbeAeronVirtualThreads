package com.playground.sbeaeronvirtualthreads;

import com.playground.sbeaeronvirtualthreads.aeron.AeronPublisher;
import com.playground.sbeaeronvirtualthreads.aeron.AeronSubscriber;
import com.playground.sbeaeronvirtualthreads.model.Trade;
import com.playground.sbeaeronvirtualthreads.monitoring.ResourceMonitor;
import com.playground.sbeaeronvirtualthreads.serialization.*;
import com.playground.sbeaeronvirtualthreads.util.EmbeddedMediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Main application demonstrating Aeron with different serialization formats
 * and Virtual Threads
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final String CHANNEL = "aeron:ipc";
    private static final int STREAM_ID = 5001;
    private static final int BUFFER_SIZE = 8192;
    private static final int MESSAGE_COUNT = 1000;
    
    public static void main(String[] args) throws Exception {
        log.info("Starting Aeron Virtual Threads Demo");
        
        // Start embedded media driver
        EmbeddedMediaDriver mediaDriver = new EmbeddedMediaDriver();
        
        try {
            Main demo = new Main();
            
            // Run demonstrations
            demo.demonstrateSerializationFormats();
            Thread.sleep(1000);
            
            demo.demonstrateVirtualThreads();
            Thread.sleep(1000);
            
            demo.demonstrateResourceUsage();
            
            log.info("Demo completed");
        } finally {
            mediaDriver.close();
        }
    }
    
    private void demonstrateSerializationFormats() throws Exception {
        log.info("\n\n=== Demonstrating Different Serialization Formats ===");
        
        List<MessageSerializer<Trade>> serializers = List.of(
            new TradeSbeSerializer(),
            new TradeProtobufSerializer(),
            new TradeJsonSerializer()
        );
        
        for (MessageSerializer<Trade> serializer : serializers) {
            log.info("\nTesting {} format", serializer.getFormatName());
            runSerializationTest(serializer, false);
        }
    }
    
    private void demonstrateVirtualThreads() throws Exception {
        log.info("\n\n=== Demonstrating Virtual Threads vs Platform Threads ===");
        
        MessageSerializer<Trade> serializer = new TradeSbeSerializer();
        
        log.info("\nUsing Platform Threads:");
        runSerializationTest(serializer, false);
        
        log.info("\nUsing Virtual Threads:");
        runSerializationTest(serializer, true);
    }
    
    private void demonstrateResourceUsage() throws Exception {
        log.info("\n\n=== Demonstrating Resource Usage Monitoring ===");
        
        ResourceMonitor monitor = new ResourceMonitor();
        MessageSerializer<Trade> serializer = new TradeSbeSerializer();
        
        // Test with platform threads
        ResourceMonitor.ResourceSnapshot beforePlatform = monitor.takeSnapshot();
        monitor.printSnapshot(beforePlatform, "Before Platform Threads Test");
        
        runSerializationTest(serializer, false);
        
        ResourceMonitor.ResourceSnapshot afterPlatform = monitor.takeSnapshot();
        monitor.printSnapshot(afterPlatform, "After Platform Threads Test");
        monitor.printDifference(beforePlatform, afterPlatform, "Platform Threads Impact");
        
        Thread.sleep(1000);
        System.gc();
        Thread.sleep(1000);
        
        // Test with virtual threads
        ResourceMonitor.ResourceSnapshot beforeVirtual = monitor.takeSnapshot();
        monitor.printSnapshot(beforeVirtual, "Before Virtual Threads Test");
        
        runSerializationTest(serializer, true);
        
        ResourceMonitor.ResourceSnapshot afterVirtual = monitor.takeSnapshot();
        monitor.printSnapshot(afterVirtual, "After Virtual Threads Test");
        monitor.printDifference(beforeVirtual, afterVirtual, "Virtual Threads Impact");
    }
    
    private void runSerializationTest(MessageSerializer<Trade> serializer, 
                                     boolean useVirtualThreads) throws Exception {
        try (AeronPublisher publisher = new AeronPublisher(CHANNEL, STREAM_ID, BUFFER_SIZE);
             AeronSubscriber subscriber = new AeronSubscriber(CHANNEL, STREAM_ID)) {
            
            List<Long> latencies = new ArrayList<>();
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
            long startTime = System.nanoTime();
            long totalBytes = 0;
            
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                Trade trade = Trade.create(
                    i, 
                    "SYMBOL" + (i % 10), 
                    100.0 + (i % 100), 
                    10 + (i % 90), 
                    i % 2 == 0 ? 'B' : 'S',
                    "COUNTERPARTY_" + (i % 5)
                );
                
                int length = serializer.serialize(trade, publisher.getBuffer(), 0);
                publisher.publishWithRetry(new byte[0], 0, length);
                totalBytes += length;
            }
            
            long endTime = System.nanoTime();
            
            // Wait for all messages
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            
            if (!completed) {
                log.error("Test did not complete in time!");
                return;
            }
            
            // Calculate and print metrics
            long minLatency = latencies.stream().mapToLong(Long::longValue).min().orElse(0);
            long maxLatency = latencies.stream().mapToLong(Long::longValue).max().orElse(0);
            long avgLatency = (long) latencies.stream().mapToLong(Long::longValue).average().orElse(0);
            long duration = endTime - startTime;
            
            log.info("Results for {} ({})", 
                serializer.getFormatName(), 
                useVirtualThreads ? "Virtual Threads" : "Platform Threads");
            log.info("  Messages: {}", MESSAGE_COUNT);
            log.info("  Total Bytes: {} ({} KB)", totalBytes, totalBytes / 1024);
            log.info("  Avg Message Size: {} bytes", totalBytes / MESSAGE_COUNT);
            log.info("  Duration: {} ms", duration / 1_000_000);
            log.info("  Throughput: {:.2f} msgs/sec", (MESSAGE_COUNT * 1_000_000_000.0) / duration);
            log.info("  Bandwidth: {:.2f} MB/sec", 
                (totalBytes * 1_000_000_000.0 / duration) / (1024 * 1024));
            log.info("  Avg Latency: {:.2f} μs", avgLatency / 1000.0);
            log.info("  Min Latency: {} μs", minLatency / 1000);
            log.info("  Max Latency: {} μs", maxLatency / 1000);
        }
    }
}

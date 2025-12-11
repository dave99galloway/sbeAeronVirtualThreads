package com.playground.sbeaeronvirtualthreads.benchmark;

import com.playground.sbeaeronvirtualthreads.aeron.AeronPublisher;
import com.playground.sbeaeronvirtualthreads.aeron.AeronSubscriber;
import com.playground.sbeaeronvirtualthreads.model.Trade;
import com.playground.sbeaeronvirtualthreads.serialization.*;
import com.playground.sbeaeronvirtualthreads.util.EmbeddedMediaDriverManager;
import io.aeron.logbuffer.FragmentHandler;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmark for Aeron messaging with different serialization formats
 * and Virtual Threads
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class AeronSerializationBenchmark {
    
    private static final String CHANNEL = "aeron:ipc";
    private static final int STREAM_ID = 3001;
    private static final int BUFFER_SIZE = 8192;
    
    private AeronPublisher publisher;
    private AeronSubscriber subscriber;
    private Trade testTrade;
    
    @Setup(Level.Trial)
    public void setupTrial() throws InterruptedException {
        EmbeddedMediaDriverManager.start();
        
        publisher = new AeronPublisher(CHANNEL, STREAM_ID, BUFFER_SIZE);
        subscriber = new AeronSubscriber(CHANNEL, STREAM_ID);
        
        testTrade = Trade.create(1L, "AAPL", 150.25, 100, 'B', "COUNTERPARTY_A");
        
        // Wait for connection
        int attempts = 0;
        while (!subscriber.isConnected() && attempts++ < 100) {
            Thread.sleep(10);
        }
    }
    
    @TearDown(Level.Trial)
    public void tearDownTrial() {
        if (subscriber != null) {
            subscriber.close();
        }
        if (publisher != null) {
            publisher.close();
        }
        EmbeddedMediaDriverManager.stop();
    }
    
    @Benchmark
    public void benchmarkSbeSerialization() throws InterruptedException {
        MessageSerializer<Trade> serializer = new TradeSbeSerializer();
        benchmarkWithSerializer(serializer, false);
    }
    
    @Benchmark
    public void benchmarkProtobufSerialization() throws InterruptedException {
        MessageSerializer<Trade> serializer = new TradeProtobufSerializer();
        benchmarkWithSerializer(serializer, false);
    }
    
    @Benchmark
    public void benchmarkJsonSerialization() throws InterruptedException {
        MessageSerializer<Trade> serializer = new TradeJsonSerializer();
        benchmarkWithSerializer(serializer, false);
    }
    
    @Benchmark
    public void benchmarkSbeWithVirtualThreads() throws InterruptedException {
        MessageSerializer<Trade> serializer = new TradeSbeSerializer();
        benchmarkWithSerializer(serializer, true);
    }
    
    @Benchmark
    public void benchmarkProtobufWithVirtualThreads() throws InterruptedException {
        MessageSerializer<Trade> serializer = new TradeProtobufSerializer();
        benchmarkWithSerializer(serializer, true);
    }
    
    @Benchmark
    public void benchmarkJsonWithVirtualThreads() throws InterruptedException {
        MessageSerializer<Trade> serializer = new TradeJsonSerializer();
        benchmarkWithSerializer(serializer, true);
    }
    
    private void benchmarkWithSerializer(MessageSerializer<Trade> serializer, boolean useVirtualThreads) 
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        FragmentHandler handler = (buffer, offset, length, header) -> {
            serializer.deserialize(buffer, offset, length);
            latch.countDown();
        };
        
        // Start subscriber
        if (useVirtualThreads) {
            subscriber.startPollingWithVirtualThread(handler);
        } else {
            subscriber.startPolling(handler);
        }
        
        Thread.sleep(10); // Brief wait for subscriber to start
        
        // Publish message
        int length = serializer.serialize(testTrade, publisher.getBuffer(), 0);
        publisher.publish(length);
        
        // Wait for message to be received
        latch.await(5, TimeUnit.SECONDS);
        
        // Stop subscriber
        subscriber.stopPolling();
    }
}

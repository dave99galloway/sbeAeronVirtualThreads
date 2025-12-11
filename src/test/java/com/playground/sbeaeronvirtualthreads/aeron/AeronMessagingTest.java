package com.playground.sbeaeronvirtualthreads.aeron;

import com.playground.sbeaeronvirtualthreads.model.Trade;
import com.playground.sbeaeronvirtualthreads.serialization.MessageSerializer;
import com.playground.sbeaeronvirtualthreads.serialization.TradeJsonSerializer;
import com.playground.sbeaeronvirtualthreads.serialization.TradeProtobufSerializer;
import com.playground.sbeaeronvirtualthreads.serialization.TradeSbeSerializer;
import com.playground.sbeaeronvirtualthreads.util.EmbeddedMediaDriverManager;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Aeron messaging with different serialization formats
 */
class AeronMessagingTest {
    private static final String CHANNEL = "aeron:ipc";
    private static final int STREAM_ID = 1001;
    private static final int BUFFER_SIZE = 8192;
    
    private AeronPublisher publisher;
    private AeronSubscriber subscriber;
    
    @BeforeAll
    static void startMediaDriver() {
        EmbeddedMediaDriverManager.start();
    }
    
    @AfterAll
    static void stopMediaDriver() {
        EmbeddedMediaDriverManager.stop();
    }
    
    @BeforeEach
    void setUp() throws InterruptedException {
        // Create publisher first, then subscriber
        publisher = new AeronPublisher(CHANNEL, STREAM_ID, BUFFER_SIZE);
        Thread.sleep(50); // Give publisher time to register
        
        subscriber = new AeronSubscriber(CHANNEL, STREAM_ID);
        Thread.sleep(50); // Give subscriber time to register
        
        // Wait for publication to be connected (has subscribers)
        int attempts = 0;
        while (!publisher.isConnected() && attempts++ < 100) {
            Thread.sleep(10);
        }
        
        assertThat(publisher.isConnected()).withFailMessage("Publisher failed to connect after 1 second").isTrue();
    }
    
    @AfterEach
    void tearDown() {
        if (subscriber != null) {
            subscriber.close();
        }
        if (publisher != null) {
            publisher.close();
        }
    }
    
    @Test
    void shouldPublishAndReceiveMessageUsingSbe() throws InterruptedException {
        // Given
        Trade trade = Trade.create(1L, "AAPL", 150.25, 100, 'B', "COUNTERPARTY_A");
        MessageSerializer<Trade> serializer = new TradeSbeSerializer();
        
        List<Trade> receivedTrades = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        FragmentHandler handler = (buffer, offset, length, header) -> {
            Trade received = serializer.deserialize(buffer, offset, length);
            receivedTrades.add(received);
            latch.countDown();
        };
        
        subscriber.startPolling(handler);
        Thread.sleep(100); // Give subscriber time to start
        
        // When
        int length = serializer.serialize(trade, publisher.getBuffer(), 0);
        publisher.publish(length);
        
        // Then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedTrades).hasSize(1);
        Trade received = receivedTrades.get(0);
        assertThat(received.tradeId()).isEqualTo(trade.tradeId());
        assertThat(received.symbol()).isEqualTo(trade.symbol());
        assertThat(received.price()).isEqualTo(trade.price());
        assertThat(received.quantity()).isEqualTo(trade.quantity());
    }
    
    @Test
    void shouldPublishAndReceiveMessageUsingProtobuf() throws InterruptedException {
        // Given
        Trade trade = Trade.create(2L, "GOOGL", 2800.50, 50, 'S', "COUNTERPARTY_B");
        MessageSerializer<Trade> serializer = new TradeProtobufSerializer();
        
        List<Trade> receivedTrades = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        FragmentHandler handler = (buffer, offset, length, header) -> {
            Trade received = serializer.deserialize(buffer, offset, length);
            receivedTrades.add(received);
            latch.countDown();
        };
        
        subscriber.startPolling(handler);
        Thread.sleep(100);
        
        // When
        int length = serializer.serialize(trade, publisher.getBuffer(), 0);
        publisher.publish(length);
        
        // Then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedTrades).hasSize(1);
        Trade received = receivedTrades.get(0);
        assertThat(received.tradeId()).isEqualTo(trade.tradeId());
        assertThat(received.symbol()).isEqualTo(trade.symbol());
    }
    
    @Test
    void shouldPublishAndReceiveMessageUsingJson() throws InterruptedException {
        // Given
        Trade trade = Trade.create(3L, "MSFT", 380.75, 200, 'B', "COUNTERPARTY_C");
        MessageSerializer<Trade> serializer = new TradeJsonSerializer();
        
        List<Trade> receivedTrades = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        FragmentHandler handler = (buffer, offset, length, header) -> {
            Trade received = serializer.deserialize(buffer, offset, length);
            receivedTrades.add(received);
            latch.countDown();
        };
        
        subscriber.startPolling(handler);
        Thread.sleep(100);
        
        // When
        int length = serializer.serialize(trade, publisher.getBuffer(), 0);
        publisher.publish(length);
        
        // Then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedTrades).hasSize(1);
        Trade received = receivedTrades.get(0);
        assertThat(received.tradeId()).isEqualTo(trade.tradeId());
        assertThat(received.symbol()).isEqualTo(trade.symbol());
    }
    
    @ParameterizedTest
    @ValueSource(ints = {10, 100, 1000})
    void shouldHandleMultipleMessages(int messageCount) throws InterruptedException {
        // Given
        MessageSerializer<Trade> serializer = new TradeSbeSerializer();
        CountDownLatch latch = new CountDownLatch(messageCount);
        
        FragmentHandler handler = (buffer, offset, length, header) -> {
            serializer.deserialize(buffer, offset, length);
            latch.countDown();
        };
        
        subscriber.startPolling(handler);
        Thread.sleep(100);
        
        // When
        for (int i = 0; i < messageCount; i++) {
            Trade trade = Trade.create(i, "SYM" + i, 100.0 + i, 10, 'B', "CP");
            int length = serializer.serialize(trade, publisher.getBuffer(), 0);
            publisher.publish(length);
        }
        
        // Then
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(subscriber.getMessagesReceived()).isGreaterThanOrEqualTo(messageCount);
    }
    
    @Test
    void shouldUseVirtualThreadForSubscription() throws InterruptedException {
        // Given
        Trade trade = Trade.create(100L, "TSLA", 250.00, 75, 'B', "COUNTERPARTY_D");
        MessageSerializer<Trade> serializer = new TradeSbeSerializer();
        
        List<Trade> receivedTrades = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        FragmentHandler handler = (buffer, offset, length, header) -> {
            Trade received = serializer.deserialize(buffer, offset, length);
            receivedTrades.add(received);
            latch.countDown();
        };
        
        // When - use virtual thread
        subscriber.startPollingWithVirtualThread(handler);
        Thread.sleep(100);
        
        int length = serializer.serialize(trade, publisher.getBuffer(), 0);
        publisher.publish(length);
        
        // Then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedTrades).hasSize(1);
    }
}

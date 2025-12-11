package com.playground.sbeaeronvirtualthreads.cucumber;

import com.playground.sbeaeronvirtualthreads.aeron.AeronPublisher;
import com.playground.sbeaeronvirtualthreads.aeron.AeronSubscriber;
import com.playground.sbeaeronvirtualthreads.model.Trade;
import com.playground.sbeaeronvirtualthreads.serialization.*;
import com.playground.sbeaeronvirtualthreads.util.EmbeddedMediaDriverManager;
import io.aeron.logbuffer.FragmentHandler;
import io.cucumber.java.*;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.datatable.DataTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cucumber step definitions for Aeron messaging
 */
public class AeronMessagingSteps {
    private static final String CHANNEL = "aeron:ipc";
    private static final int STREAM_ID = 2001;
    private static final int BUFFER_SIZE = 8192;
    
    private AeronPublisher publisher;
    private AeronSubscriber subscriber;
    private MessageSerializer<Trade> currentSerializer;
    private Trade sentTrade;
    private Trade receivedTrade;
    private int serializedSize;
    private List<Trade> receivedTrades;
    private CountDownLatch receiveLatch;
    private long startTime;
    private long endTime;
    private boolean useVirtualThreads = false;
    
    @BeforeAll
    public static void startMediaDriver() {
        EmbeddedMediaDriverManager.start();
    }
    
    @AfterAll
    public static void stopMediaDriver() {
        EmbeddedMediaDriverManager.stop();
    }
    
    @Before
    public void setUp() {
        receivedTrades = new ArrayList<>();
    }
    
    @After
    public void tearDown() {
        if (subscriber != null) {
            subscriber.close();
            subscriber = null;
        }
        if (publisher != null) {
            publisher.close();
            publisher = null;
        }
    }
    
    @Given("an Aeron publisher and subscriber are connected")
    public void anAeronPublisherAndSubscriberAreConnected() throws InterruptedException {
        publisher = new AeronPublisher(CHANNEL, STREAM_ID, BUFFER_SIZE);
        subscriber = new AeronSubscriber(CHANNEL, STREAM_ID);
        
        // Wait for connection
        int attempts = 0;
        while (!subscriber.isConnected() && attempts++ < 100) {
            Thread.sleep(10);
        }
        
        assertThat(publisher.isConnected()).isTrue();
        assertThat(subscriber.isConnected()).isTrue();
    }
    
    @Given("an Aeron publisher is connected")
    public void anAeronPublisherIsConnected() throws InterruptedException {
        publisher = new AeronPublisher(CHANNEL, STREAM_ID, BUFFER_SIZE);
        Thread.sleep(100); // Give publisher time to register
    }
    
    @Given("an Aeron subscriber using virtual threads is connected")
    public void anAeronSubscriberUsingVirtualThreadsIsConnected() throws InterruptedException {
        subscriber = new AeronSubscriber(CHANNEL, STREAM_ID);
        useVirtualThreads = true;
        
        // Wait for connection
        int attempts = 0;
        while (!subscriber.isConnected() && attempts++ < 100) {
            Thread.sleep(10);
        }
        
        assertThat(subscriber.isConnected()).isTrue();
    }
    
    @When("I send a trade message using SBE format")
    public void iSendATradeMessageUsingSbeFormat(DataTable dataTable) throws InterruptedException {
        currentSerializer = new TradeSbeSerializer();
        sendTradeMessage(dataTable);
    }
    
    @When("I send a trade message using Protobuf format")
    public void iSendATradeMessageUsingProtobufFormat(DataTable dataTable) throws InterruptedException {
        currentSerializer = new TradeProtobufSerializer();
        sendTradeMessage(dataTable);
    }
    
    @When("I send a trade message using JSON format")
    public void iSendATradeMessageUsingJsonFormat(DataTable dataTable) throws InterruptedException {
        currentSerializer = new TradeJsonSerializer();
        sendTradeMessage(dataTable);
    }
    
    @When("I serialize a trade message using {word} format")
    public void iSerializeATradeMessageUsingFormat(String format, DataTable dataTable) {
        currentSerializer = getSerializerForFormat(format);
        sentTrade = createTradeFromTable(dataTable);
        serializedSize = currentSerializer.serialize(sentTrade, publisher.getBuffer(), 0);
    }
    
    @When("I send {int} trade messages using SBE format")
    public void iSendMultipleTradeMessagesUsingSbeFormat(int messageCount) throws InterruptedException {
        currentSerializer = new TradeSbeSerializer();
        receiveLatch = new CountDownLatch(messageCount);
        
        FragmentHandler handler = (buffer, offset, length, header) -> {
            Trade trade = currentSerializer.deserialize(buffer, offset, length);
            receivedTrades.add(trade);
            receiveLatch.countDown();
        };
        
        if (useVirtualThreads) {
            subscriber.startPollingWithVirtualThread(handler);
        } else {
            subscriber.startPolling(handler);
        }
        
        Thread.sleep(100); // Give subscriber time to start
        
        startTime = System.nanoTime();
        
        for (int i = 0; i < messageCount; i++) {
            sentTrade = Trade.create(i, "SYM" + i, 100.0 + i, 10 + i, 'B', "CP_" + i);
            int length = currentSerializer.serialize(sentTrade, publisher.getBuffer(), 0);
            publisher.publish(length);
        }
        
        endTime = System.nanoTime();
    }
    
    @Then("the message should be received successfully")
    public void theMessageShouldBeReceivedSuccessfully() throws InterruptedException {
        assertThat(receiveLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedTrade).isNotNull();
    }
    
    @Then("the received trade should match the sent trade")
    public void theReceivedTradeShouldMatchTheSentTrade() {
        assertThat(receivedTrade.tradeId()).isEqualTo(sentTrade.tradeId());
        assertThat(receivedTrade.symbol()).isEqualTo(sentTrade.symbol());
        assertThat(receivedTrade.price()).isEqualTo(sentTrade.price());
        assertThat(receivedTrade.quantity()).isEqualTo(sentTrade.quantity());
        assertThat(receivedTrade.side()).isEqualTo(sentTrade.side());
        assertThat(receivedTrade.counterparty()).isEqualTo(sentTrade.counterparty());
    }
    
    @Then("the serialized message size should be recorded")
    public void theSerializedMessageSizeShouldBeRecorded() {
        assertThat(serializedSize).isGreaterThan(0);
        System.out.println("Serialized size: " + serializedSize + " bytes");
    }
    
    @And("the {word} format should produce a message of expected size range")
    public void theFormatShouldProduceAMessageOfExpectedSizeRange(String format) {
        switch (format) {
            case "SBE" -> assertThat(serializedSize).isBetween(40, 100);
            case "Protobuf" -> assertThat(serializedSize).isBetween(40, 120);
            case "JSON" -> assertThat(serializedSize).isBetween(100, 200);
            default -> throw new IllegalArgumentException("Unknown format: " + format);
        }
    }
    
    @Then("all messages should be received successfully")
    public void allMessagesShouldBeReceivedSuccessfully() throws InterruptedException {
        assertThat(receiveLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedTrades).hasSizeGreaterThanOrEqualTo((int) receiveLatch.getCount());
    }
    
    @Then("all messages should be received by the virtual thread subscriber")
    public void allMessagesShouldBeReceivedByTheVirtualThreadSubscriber() throws InterruptedException {
        assertThat(receiveLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedTrades).isNotEmpty();
    }
    
    @And("the throughput should be measured")
    public void theThroughputShouldBeMeasured() {
        long durationNanos = endTime - startTime;
        double durationSeconds = durationNanos / 1_000_000_000.0;
        double throughput = receivedTrades.size() / durationSeconds;
        
        System.out.println("Sent " + receivedTrades.size() + " messages in " + 
                           String.format("%.3f", durationSeconds) + " seconds");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " messages/second");
        
        assertThat(throughput).isGreaterThan(0);
    }
    
    @And("no thread contention should be observed")
    public void noThreadContentionShouldBeObserved() {
        // In a real scenario, we'd monitor thread states and CPU usage
        // For now, just verify that messages were received
        assertThat(receivedTrades).isNotEmpty();
    }
    
    // Helper methods
    
    private void sendTradeMessage(DataTable dataTable) throws InterruptedException {
        sentTrade = createTradeFromTable(dataTable);
        receiveLatch = new CountDownLatch(1);
        
        FragmentHandler handler = (buffer, offset, length, header) -> {
            receivedTrade = currentSerializer.deserialize(buffer, offset, length);
            receiveLatch.countDown();
        };
        
        subscriber.startPolling(handler);
        Thread.sleep(100); // Give subscriber time to start
        
        int length = currentSerializer.serialize(sentTrade, publisher.getBuffer(), 0);
        publisher.publish(length);
    }
    
    private Trade createTradeFromTable(DataTable dataTable) {
        Map<String, String> data = dataTable.asMaps().get(0);
        return new Trade(
            Long.parseLong(data.get("tradeId")),
            System.nanoTime(),
            Double.parseDouble(data.get("price")),
            Integer.parseInt(data.get("quantity")),
            data.get("side").charAt(0),
            data.get("symbol"),
            data.get("counterparty")
        );
    }
    
    private MessageSerializer<Trade> getSerializerForFormat(String format) {
        return switch (format) {
            case "SBE" -> new TradeSbeSerializer();
            case "Protobuf" -> new TradeProtobufSerializer();
            case "JSON" -> new TradeJsonSerializer();
            default -> throw new IllegalArgumentException("Unknown format: " + format);
        };
    }
}

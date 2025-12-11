package com.playground.sbeaeronvirtualthreads.serialization;

import com.playground.sbeaeronvirtualthreads.model.Trade;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for comparing serialization formats
 */
class SerializationComparisonTest {
    
    private static final int BUFFER_SIZE = 8192;
    
    @ParameterizedTest(name = "{0}")
    @MethodSource("serializerProvider")
    void shouldSerializeAndDeserializeTrade(String name, MessageSerializer<Trade> serializer) {
        // Given
        Trade trade = Trade.create(123L, "AAPL", 150.25, 100, 'B', "COUNTERPARTY_A");
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
        
        // When
        int length = serializer.serialize(trade, buffer, 0);
        Trade deserialized = serializer.deserialize(buffer, 0, length);
        
        // Then
        assertThat(deserialized.tradeId()).isEqualTo(trade.tradeId());
        assertThat(deserialized.symbol()).isEqualTo(trade.symbol());
        assertThat(deserialized.price()).isEqualTo(trade.price());
        assertThat(deserialized.quantity()).isEqualTo(trade.quantity());
        assertThat(deserialized.side()).isEqualTo(trade.side());
        assertThat(deserialized.counterparty()).isEqualTo(trade.counterparty());
    }
    
    @Test
    void shouldCompareSizesBetweenFormats() {
        // Given
        Trade trade = Trade.create(123L, "AAPL", 150.25, 100, 'B', "COUNTERPARTY_A");
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
        
        MessageSerializer<Trade> sbeSerializer = new TradeSbeSerializer();
        MessageSerializer<Trade> protobufSerializer = new TradeProtobufSerializer();
        MessageSerializer<Trade> jsonSerializer = new TradeJsonSerializer();
        
        // When
        int sbeSize = sbeSerializer.serialize(trade, buffer, 0);
        int protobufSize = protobufSerializer.serialize(trade, buffer, 0);
        int jsonSize = jsonSerializer.serialize(trade, buffer, 0);
        
        // Then
        System.out.println("SBE size: " + sbeSize + " bytes");
        System.out.println("Protobuf size: " + protobufSize + " bytes");
        System.out.println("JSON size: " + jsonSize + " bytes");
        
        // SBE should be the most compact
        assertThat(sbeSize).isLessThan(protobufSize);
        assertThat(protobufSize).isLessThan(jsonSize);
        
        // Verify all are valid
        assertThat(sbeSize).isGreaterThan(0);
        assertThat(protobufSize).isGreaterThan(0);
        assertThat(jsonSize).isGreaterThan(0);
    }
    
    @Test
    void shouldMeasureSerializationPerformance() {
        // Given
        Trade trade = Trade.create(123L, "AAPL", 150.25, 100, 'B', "COUNTERPARTY_A");
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
        
        MessageSerializer<Trade> sbeSerializer = new TradeSbeSerializer();
        MessageSerializer<Trade> protobufSerializer = new TradeProtobufSerializer();
        MessageSerializer<Trade> jsonSerializer = new TradeJsonSerializer();
        
        int iterations = 10000;
        
        // Warmup
        for (int i = 0; i < 1000; i++) {
            sbeSerializer.serialize(trade, buffer, 0);
            protobufSerializer.serialize(trade, buffer, 0);
            jsonSerializer.serialize(trade, buffer, 0);
        }
        
        // Measure SBE
        long sbeStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            sbeSerializer.serialize(trade, buffer, 0);
        }
        long sbeTime = System.nanoTime() - sbeStart;
        
        // Measure Protobuf
        long protobufStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            protobufSerializer.serialize(trade, buffer, 0);
        }
        long protobufTime = System.nanoTime() - protobufStart;
        
        // Measure JSON
        long jsonStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            jsonSerializer.serialize(trade, buffer, 0);
        }
        long jsonTime = System.nanoTime() - jsonStart;
        
        // Then
        System.out.println("SBE avg: " + (sbeTime / iterations) + " ns");
        System.out.println("Protobuf avg: " + (protobufTime / iterations) + " ns");
        System.out.println("JSON avg: " + (jsonTime / iterations) + " ns");
        
        // SBE should be fastest
        assertThat(sbeTime).isLessThan(protobufTime);
        assertThat(protobufTime).isLessThan(jsonTime);
    }
    
    static Stream<Arguments> serializerProvider() {
        return Stream.of(
            Arguments.of("SBE", new TradeSbeSerializer()),
            Arguments.of("Protobuf", new TradeProtobufSerializer()),
            Arguments.of("JSON", new TradeJsonSerializer())
        );
    }
}

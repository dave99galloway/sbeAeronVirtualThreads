package com.playground.sbeaeronvirtualthreads.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playground.sbeaeronvirtualthreads.model.Trade;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.io.IOException;

/**
 * JSON serializer for Trade messages using Jackson
 */
public class TradeJsonSerializer implements MessageSerializer<Trade> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public int serialize(Trade message, MutableDirectBuffer buffer, int offset) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(message);
            buffer.putBytes(offset, jsonBytes);
            return jsonBytes.length;
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize trade to JSON", e);
        }
    }
    
    @Override
    public Trade deserialize(DirectBuffer buffer, int offset, int length) {
        try {
            byte[] jsonBytes = new byte[length];
            buffer.getBytes(offset, jsonBytes);
            return objectMapper.readValue(jsonBytes, Trade.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize trade from JSON", e);
        }
    }
    
    @Override
    public String getFormatName() {
        return "JSON";
    }
}

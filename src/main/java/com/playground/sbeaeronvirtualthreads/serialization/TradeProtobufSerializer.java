package com.playground.sbeaeronvirtualthreads.serialization;

import com.playground.sbeaeronvirtualthreads.model.Trade;
import com.playground.sbeaeronvirtualthreads.proto.TradeMessage;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.io.IOException;

/**
 * Protobuf serializer for Trade messages
 */
public class TradeProtobufSerializer implements MessageSerializer<Trade> {
    
    @Override
    public int serialize(Trade message, MutableDirectBuffer buffer, int offset) {
        TradeMessage protoMsg = TradeMessage.newBuilder()
            .setTradeId(message.tradeId())
            .setTimestamp(message.timestamp())
            .setPrice(message.price())
            .setQuantity(message.quantity())
            .setSide(String.valueOf(message.side()))
            .setSymbol(message.symbol())
            .setCounterparty(message.counterparty())
            .build();
            
        byte[] protoBytes = protoMsg.toByteArray();
        buffer.putBytes(offset, protoBytes);
        return protoBytes.length;
    }
    
    @Override
    public Trade deserialize(DirectBuffer buffer, int offset, int length) {
        try {
            byte[] protoBytes = new byte[length];
            buffer.getBytes(offset, protoBytes);
            
            TradeMessage protoMsg = TradeMessage.parseFrom(protoBytes);
            
            return new Trade(
                protoMsg.getTradeId(),
                protoMsg.getTimestamp(),
                protoMsg.getPrice(),
                protoMsg.getQuantity(),
                protoMsg.getSide().charAt(0),
                protoMsg.getSymbol(),
                protoMsg.getCounterparty()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize trade from Protobuf", e);
        }
    }
    
    @Override
    public String getFormatName() {
        return "Protobuf";
    }
}

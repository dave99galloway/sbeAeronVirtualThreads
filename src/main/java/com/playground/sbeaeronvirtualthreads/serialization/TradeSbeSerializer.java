package com.playground.sbeaeronvirtualthreads.serialization;

import com.playground.sbeaeronvirtualthreads.model.Trade;
import com.playground.sbeaeronvirtualthreads.sbe.MessageHeaderEncoder;
import com.playground.sbeaeronvirtualthreads.sbe.MessageHeaderDecoder;
import com.playground.sbeaeronvirtualthreads.sbe.TradeMessageEncoder;
import com.playground.sbeaeronvirtualthreads.sbe.TradeMessageDecoder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * SBE serializer for Trade messages
 */
public class TradeSbeSerializer implements MessageSerializer<Trade> {
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final TradeMessageEncoder encoder = new TradeMessageEncoder();
    private final TradeMessageDecoder decoder = new TradeMessageDecoder();
    
    @Override
    public int serialize(Trade message, MutableDirectBuffer buffer, int offset) {
        encoder.wrapAndApplyHeader(buffer, offset, headerEncoder);
        
        encoder.tradeId(message.tradeId());
        encoder.timestamp(message.timestamp());
        encoder.price(message.price());
        encoder.quantity(message.quantity());
        encoder.side((byte) message.side());
        encoder.symbol(message.symbol());
        encoder.counterparty(message.counterparty());
        
        return headerEncoder.encodedLength() + encoder.encodedLength();
    }
    
    @Override
    public Trade deserialize(DirectBuffer buffer, int offset, int length) {
        headerDecoder.wrap(buffer, offset);
        
        decoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        
        long tradeId = decoder.tradeId();
        long timestamp = decoder.timestamp();
        double price = decoder.price();
        int quantity = decoder.quantity();
        char side = (char) decoder.side();
        String symbol = decoder.symbol();
        String counterparty = decoder.counterparty();
        
        return new Trade(tradeId, timestamp, price, quantity, side, symbol, counterparty);
    }
    
    @Override
    public String getFormatName() {
        return "SBE";
    }
}

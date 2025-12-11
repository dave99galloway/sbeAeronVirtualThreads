package com.playground.sbeaeronvirtualthreads.model;

/**
 * Represents market data snapshot
 */
public record MarketData(
    long timestamp,
    double bidPrice,
    double askPrice,
    int bidSize,
    int askSize,
    String symbol
) {
    public static MarketData create(String symbol, double bidPrice, double askPrice, int bidSize, int askSize) {
        return new MarketData(System.nanoTime(), bidPrice, askPrice, bidSize, askSize, symbol);
    }
}

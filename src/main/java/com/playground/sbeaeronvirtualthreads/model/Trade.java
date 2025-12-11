package com.playground.sbeaeronvirtualthreads.model;

/**
 * Represents a trade message
 */
public record Trade(
    long tradeId,
    long timestamp,
    double price,
    int quantity,
    char side,
    String symbol,
    String counterparty
) {
    public static Trade create(long tradeId, String symbol, double price, int quantity, char side, String counterparty) {
        return new Trade(tradeId, System.nanoTime(), price, quantity, side, symbol, counterparty);
    }
}

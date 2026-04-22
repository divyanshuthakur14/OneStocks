package com.onestocks.app.dto;

import java.math.BigDecimal;

public record StockDetailResponse(
        String symbol,
        String name,
        String sector,
        String description,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal dayChangePercent,
        HoldingInfo userHolding,
        BigDecimal walletBalance
) {
    public record HoldingInfo(
            Integer quantity,
            BigDecimal averageBuyPrice,
            BigDecimal currentValue,
            BigDecimal profitLoss,
            BigDecimal profitLossPercent
    ) {}
}

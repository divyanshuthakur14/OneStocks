package com.onestocks.app.dto;

import java.math.BigDecimal;

public record HoldingDTO(
        String symbol,
        String stockName,
        int quantity,
        BigDecimal averageBuyPrice,
        BigDecimal currentPrice,
        BigDecimal currentValue,
        BigDecimal profitLoss,
        BigDecimal profitLossPercent
) {}

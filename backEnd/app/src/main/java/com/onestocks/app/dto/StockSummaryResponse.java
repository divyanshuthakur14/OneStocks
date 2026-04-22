package com.onestocks.app.dto;

import java.math.BigDecimal;

public record StockSummaryResponse(
        String symbol,
        String name,
        String sector,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal dayChangePercent
) {}

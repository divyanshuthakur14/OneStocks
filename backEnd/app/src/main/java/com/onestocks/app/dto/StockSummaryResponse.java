package com.onestocks.app.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockSummaryResponse(
        Long id,
        String symbol,
        String name,
        String sector,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal dayChangePercent,
        Instant updatedAt
) {}

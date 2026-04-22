package com.onestocks.app.dto;

import com.onestocks.app.model.TransactionStatus;
import com.onestocks.app.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        String symbol,
        String stockName,
        TransactionType type,
        Integer quantity,
        BigDecimal pricePerShare,
        BigDecimal totalAmount,
        TransactionStatus status,
        Instant createdAt,
        BigDecimal walletBalanceAfter
) {}

package com.onestocks.app.dto;

import com.onestocks.app.model.TransactionStatus;
import com.onestocks.app.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionDTO(
        Instant createdAt,
        String stockName,
        TransactionType type,
        TransactionStatus status,
        Integer quantity,
        BigDecimal pricePerShare,
        BigDecimal totalAmount

) {
}

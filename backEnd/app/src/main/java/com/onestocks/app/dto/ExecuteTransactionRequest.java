package com.onestocks.app.dto;

import com.onestocks.app.model.TransactionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExecuteTransactionRequest(
        @NotBlank(message = "Symbol is required")
        String symbol,

        @NotNull(message = "Type is required (BUY or SELL)")
        TransactionType type,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        @Max(value = 10000, message = "Quantity cannot exceed 10000 shares per transaction")
        Integer quantity
) {}

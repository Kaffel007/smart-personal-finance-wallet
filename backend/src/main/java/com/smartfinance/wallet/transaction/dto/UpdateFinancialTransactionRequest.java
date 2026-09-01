package com.smartfinance.wallet.transaction.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateFinancialTransactionRequest(
        @NotNull @Positive Long categoryId,
        @NotNull @DecimalMin(value = "0.0000", inclusive = false) @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotNull @PastOrPresent LocalDate transactionDate,
        @Size(max = 255) String description
) {}

package com.smartfinance.wallet.budget.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateBudgetRequest(
        @NotNull @Positive Long categoryId,
        @Min(2000) @Max(2100) int year,
        @Min(1) @Max(12) int month,
        @NotNull @DecimalMin(value = "0.0000", inclusive = false) @Digits(integer = 15, fraction = 4) BigDecimal amount
) {}

package com.smartfinance.wallet.savingsgoal.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSavingsGoalRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin(value = "0.0000", inclusive = false)
        @Digits(integer = 15, fraction = 4) BigDecimal targetAmount,
        @DecimalMin(value = "0.0000") @Digits(integer = 15, fraction = 4) BigDecimal savedAmount,
        LocalDate targetDate
) {}

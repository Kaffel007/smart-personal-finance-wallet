package com.smartfinance.wallet.budget.dto;

import com.smartfinance.wallet.budget.entity.Budget;
import java.math.BigDecimal;

public record BudgetResponse(Long id, Long categoryId, String categoryName, int year, int month,
        BigDecimal amount, BigDecimal spent, BigDecimal remaining, BigDecimal usagePercent) {
    public static BudgetResponse from(Budget budget, BigDecimal spent, BigDecimal usagePercent) {
        return new BudgetResponse(budget.getId(), budget.getCategory().getId(), budget.getCategory().getName(),
                budget.getYear(), budget.getMonth(), budget.getAmount(), spent,
                budget.getAmount().subtract(spent), usagePercent);
    }
}

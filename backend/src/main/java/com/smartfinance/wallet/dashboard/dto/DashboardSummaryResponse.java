package com.smartfinance.wallet.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        int year,
        int month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        long transactionCount,
        BigDecimal totalBudget,
        BigDecimal budgetSpent,
        BigDecimal budgetRemaining,
        BigDecimal budgetUsagePercent,
        long budgetCount,
        BigDecimal totalSavingsTarget,
        BigDecimal totalSavingsSaved,
        BigDecimal savingsRemaining,
        BigDecimal savingsProgressPercent,
        long savingsGoalCount
) {}

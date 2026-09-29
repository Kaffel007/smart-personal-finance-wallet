package com.smartfinance.wallet.dashboard.service;

import com.smartfinance.wallet.budget.repository.BudgetRepository;
import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.dashboard.dto.DashboardSummaryResponse;
import com.smartfinance.wallet.savingsgoal.repository.SavingsGoalRepository;
import com.smartfinance.wallet.transaction.repository.FinancialTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Set;

@Service
public class DashboardService {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final FinancialTransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;

    public DashboardService(FinancialTransactionRepository transactionRepository,
            BudgetRepository budgetRepository, SavingsGoalRepository savingsGoalRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);

        BigDecimal totalIncome = transactionRepository.sumByUserAndTypeAndPeriod(
                userId, CategoryType.INCOME, startDate, endDate);
        BigDecimal totalExpense = transactionRepository.sumByUserAndTypeAndPeriod(
                userId, CategoryType.EXPENSE, startDate, endDate);
        long transactionCount = transactionRepository.countByUserAndPeriod(userId, startDate, endDate);

        BigDecimal totalBudget = budgetRepository.sumAmountByUserAndPeriod(userId, year, month);
        Set<Long> budgetCategoryIds = budgetRepository.findCategoryIdsByUserAndPeriod(userId, year, month);
        BigDecimal budgetSpent = budgetCategoryIds.isEmpty() ? BigDecimal.ZERO
                : transactionRepository.sumExpensesByCategoriesAndPeriod(
                        userId, budgetCategoryIds, startDate, endDate);
        long budgetCount = budgetRepository.countByUserIdAndYearAndMonth(userId, year, month);

        BigDecimal totalSavingsTarget = savingsGoalRepository.sumTargetAmountByUserId(userId);
        BigDecimal totalSavingsSaved = savingsGoalRepository.sumSavedAmountByUserId(userId);
        long savingsGoalCount = savingsGoalRepository.countByUserId(userId);

        return new DashboardSummaryResponse(year, month, totalIncome, totalExpense,
                totalIncome.subtract(totalExpense), transactionCount, totalBudget, budgetSpent,
                totalBudget.subtract(budgetSpent), percentage(budgetSpent, totalBudget), budgetCount,
                totalSavingsTarget, totalSavingsSaved,
                totalSavingsTarget.subtract(totalSavingsSaved),
                percentage(totalSavingsSaved, totalSavingsTarget), savingsGoalCount);
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return value.multiply(ONE_HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }
}

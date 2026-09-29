package com.smartfinance.wallet.dashboard.service;

import com.smartfinance.wallet.budget.repository.BudgetRepository;
import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.dashboard.dto.DashboardSummaryResponse;
import com.smartfinance.wallet.savingsgoal.repository.SavingsGoalRepository;
import com.smartfinance.wallet.transaction.repository.FinancialTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTests {
    @Mock FinancialTransactionRepository transactionRepository;
    @Mock BudgetRepository budgetRepository;
    @Mock SavingsGoalRepository savingsGoalRepository;
    @InjectMocks DashboardService service;

    @Test
    void shouldReturnZerosWhenUserHasNoData() {
        stubTransactionTotals(1L, "0", "0", 0);
        when(budgetRepository.sumAmountByUserAndPeriod(1L, 2026, 9)).thenReturn(BigDecimal.ZERO);
        when(budgetRepository.findCategoryIdsByUserAndPeriod(1L, 2026, 9)).thenReturn(Set.of());
        when(savingsGoalRepository.sumTargetAmountByUserId(1L)).thenReturn(BigDecimal.ZERO);
        when(savingsGoalRepository.sumSavedAmountByUserId(1L)).thenReturn(BigDecimal.ZERO);

        DashboardSummaryResponse result = service.getSummary(1L, 2026, 9);

        assertThat(result.year()).isEqualTo(2026);
        assertThat(result.month()).isEqualTo(9);
        assertThat(result.totalIncome()).isZero();
        assertThat(result.totalExpense()).isZero();
        assertThat(result.balance()).isZero();
        assertThat(result.transactionCount()).isZero();
        assertThat(result.totalBudget()).isZero();
        assertThat(result.budgetSpent()).isZero();
        assertThat(result.budgetRemaining()).isZero();
        assertThat(result.budgetUsagePercent()).isEqualByComparingTo("0.00");
        assertThat(result.budgetCount()).isZero();
        assertThat(result.totalSavingsTarget()).isZero();
        assertThat(result.totalSavingsSaved()).isZero();
        assertThat(result.savingsRemaining()).isZero();
        assertThat(result.savingsProgressPercent()).isEqualByComparingTo("0.00");
        assertThat(result.savingsGoalCount()).isZero();
        verify(transactionRepository, never()).sumExpensesByCategoriesAndPeriod(any(), any(), any(), any());
    }

    @Test
    void shouldCalculateTransactionsBudgetsAndSavingsIncludingOverruns() {
        stubTransactionTotals(1L, "3000", "1700", 5);
        when(budgetRepository.sumAmountByUserAndPeriod(1L, 2026, 9))
                .thenReturn(new BigDecimal("1000"));
        when(budgetRepository.findCategoryIdsByUserAndPeriod(1L, 2026, 9))
                .thenReturn(Set.of(10L, 11L));
        when(transactionRepository.sumExpensesByCategoriesAndPeriod(eq(1L), eq(Set.of(10L, 11L)),
                eq(LocalDate.of(2026, 9, 1)), eq(LocalDate.of(2026, 10, 1))))
                .thenReturn(new BigDecimal("1250"));
        when(budgetRepository.countByUserIdAndYearAndMonth(1L, 2026, 9)).thenReturn(2L);
        when(savingsGoalRepository.sumTargetAmountByUserId(1L)).thenReturn(new BigDecimal("2000"));
        when(savingsGoalRepository.sumSavedAmountByUserId(1L)).thenReturn(new BigDecimal("2400"));
        when(savingsGoalRepository.countByUserId(1L)).thenReturn(3L);

        DashboardSummaryResponse result = service.getSummary(1L, 2026, 9);

        assertThat(result.totalIncome()).isEqualByComparingTo("3000");
        assertThat(result.totalExpense()).isEqualByComparingTo("1700");
        assertThat(result.balance()).isEqualByComparingTo("1300");
        assertThat(result.transactionCount()).isEqualTo(5);
        assertThat(result.totalBudget()).isEqualByComparingTo("1000");
        assertThat(result.budgetSpent()).isEqualByComparingTo("1250");
        assertThat(result.budgetRemaining()).isEqualByComparingTo("-250");
        assertThat(result.budgetUsagePercent()).isEqualByComparingTo("125.00");
        assertThat(result.budgetCount()).isEqualTo(2);
        assertThat(result.totalSavingsTarget()).isEqualByComparingTo("2000");
        assertThat(result.totalSavingsSaved()).isEqualByComparingTo("2400");
        assertThat(result.savingsRemaining()).isEqualByComparingTo("-400");
        assertThat(result.savingsProgressPercent()).isEqualByComparingTo("120.00");
        assertThat(result.savingsGoalCount()).isEqualTo(3);
    }

    @Test
    void shouldAlwaysPassJwtOwnerAndRequestedPeriodToRepositories() {
        stubTransactionTotals(8L, "10", "4", 2);
        when(budgetRepository.sumAmountByUserAndPeriod(8L, 2027, 2)).thenReturn(BigDecimal.ZERO);
        when(budgetRepository.findCategoryIdsByUserAndPeriod(8L, 2027, 2)).thenReturn(Set.of());
        when(savingsGoalRepository.sumTargetAmountByUserId(8L)).thenReturn(BigDecimal.ZERO);
        when(savingsGoalRepository.sumSavedAmountByUserId(8L)).thenReturn(BigDecimal.ZERO);

        service.getSummary(8L, 2027, 2);

        LocalDate start = LocalDate.of(2027, 2, 1);
        LocalDate end = LocalDate.of(2027, 3, 1);
        verify(transactionRepository).sumByUserAndTypeAndPeriod(8L, CategoryType.INCOME, start, end);
        verify(transactionRepository).sumByUserAndTypeAndPeriod(8L, CategoryType.EXPENSE, start, end);
        verify(transactionRepository).countByUserAndPeriod(8L, start, end);
        verify(savingsGoalRepository).countByUserId(8L);
    }

    private void stubTransactionTotals(Long userId, String income, String expense, long count) {
        LocalDate start = userId == 8L ? LocalDate.of(2027, 2, 1) : LocalDate.of(2026, 9, 1);
        LocalDate end = start.plusMonths(1);
        when(transactionRepository.sumByUserAndTypeAndPeriod(userId, CategoryType.INCOME, start, end))
                .thenReturn(new BigDecimal(income));
        when(transactionRepository.sumByUserAndTypeAndPeriod(userId, CategoryType.EXPENSE, start, end))
                .thenReturn(new BigDecimal(expense));
        when(transactionRepository.countByUserAndPeriod(userId, start, end)).thenReturn(count);
    }
}

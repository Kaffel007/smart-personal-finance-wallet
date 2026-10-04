import { DashboardSummary } from './dashboard-summary.model';

export const summaryFixture: DashboardSummary = {
  year: 2026, month: 9,
  totalIncome: 3000, totalExpense: 500, balance: 2500, transactionCount: 2,
  totalBudget: 1000, budgetSpent: 500, budgetRemaining: 500, budgetUsagePercent: 50, budgetCount: 1,
  totalSavingsTarget: 2000, totalSavingsSaved: 500, savingsRemaining: 1500, savingsProgressPercent: 25, savingsGoalCount: 1,
};

export const emptySummaryFixture: DashboardSummary = Object.fromEntries(
  Object.keys(summaryFixture).map(key => [key, key === 'year' ? 2026 : key === 'month' ? 9 : 0]),
) as unknown as DashboardSummary;

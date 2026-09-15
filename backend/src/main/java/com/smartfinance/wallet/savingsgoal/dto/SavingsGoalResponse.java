package com.smartfinance.wallet.savingsgoal.dto;

import com.smartfinance.wallet.savingsgoal.entity.SavingsGoal;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalResponse(
        Long id,
        String name,
        BigDecimal targetAmount,
        BigDecimal savedAmount,
        BigDecimal remainingAmount,
        BigDecimal progressPercent,
        LocalDate targetDate
) {
    public static SavingsGoalResponse from(SavingsGoal goal, BigDecimal progressPercent) {
        return new SavingsGoalResponse(goal.getId(), goal.getName(), goal.getTargetAmount(),
                goal.getSavedAmount(), goal.getTargetAmount().subtract(goal.getSavedAmount()),
                progressPercent, goal.getTargetDate());
    }
}

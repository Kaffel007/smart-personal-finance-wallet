package com.smartfinance.wallet.savingsgoal.exception;

public class SavingsGoalNotFoundException extends RuntimeException {
    public SavingsGoalNotFoundException() {
        super("Objectif d'épargne introuvable.");
    }
}

package com.smartfinance.wallet.budget.exception;
public class BudgetNotFoundException extends RuntimeException {
    public BudgetNotFoundException() { super("Budget introuvable."); }
}

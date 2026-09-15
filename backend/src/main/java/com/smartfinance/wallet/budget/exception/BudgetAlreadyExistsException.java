package com.smartfinance.wallet.budget.exception;
public class BudgetAlreadyExistsException extends RuntimeException {
    public BudgetAlreadyExistsException() { super("Un budget existe déjà pour cette catégorie et cette période."); }
}

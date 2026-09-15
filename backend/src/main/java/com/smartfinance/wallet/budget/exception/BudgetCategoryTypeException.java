package com.smartfinance.wallet.budget.exception;
public class BudgetCategoryTypeException extends RuntimeException {
    public BudgetCategoryTypeException() { super("Un budget doit utiliser une catégorie de dépense."); }
}

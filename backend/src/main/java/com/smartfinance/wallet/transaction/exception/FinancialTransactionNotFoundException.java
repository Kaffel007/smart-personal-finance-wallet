package com.smartfinance.wallet.transaction.exception;

public class FinancialTransactionNotFoundException extends RuntimeException {
    public FinancialTransactionNotFoundException() { super("Transaction introuvable."); }
}

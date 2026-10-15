package com.smartfinance.wallet.account.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException() {
        super("Compte introuvable.");
    }
}

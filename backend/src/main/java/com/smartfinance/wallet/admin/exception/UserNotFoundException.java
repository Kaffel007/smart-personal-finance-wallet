package com.smartfinance.wallet.admin.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("Utilisateur introuvable.");
    }
}

package com.smartfinance.wallet.account.exception;

public class PasswordUnchangedException extends RuntimeException {
    public PasswordUnchangedException() {
        super("Le nouveau mot de passe doit être différent du mot de passe actuel.");
    }
}

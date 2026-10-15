package com.smartfinance.wallet.account.exception;

public class CurrentPasswordIncorrectException extends RuntimeException {
    public CurrentPasswordIncorrectException() {
        super("Le mot de passe actuel est incorrect.");
    }
}

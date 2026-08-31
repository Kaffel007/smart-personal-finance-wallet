package com.smartfinance.wallet.common.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Identifiants incorrects ou compte indisponible.");
    }
}

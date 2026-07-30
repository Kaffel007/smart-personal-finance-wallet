package com.smartfinance.wallet.common.exception;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("Cette adresse e-mail ne peut pas être utilisée.");
    }
}

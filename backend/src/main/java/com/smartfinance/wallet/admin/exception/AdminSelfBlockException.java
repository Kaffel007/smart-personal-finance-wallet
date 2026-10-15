package com.smartfinance.wallet.admin.exception;

public class AdminSelfBlockException extends RuntimeException {
    public AdminSelfBlockException() {
        super("Un administrateur ne peut pas bloquer son propre compte.");
    }
}

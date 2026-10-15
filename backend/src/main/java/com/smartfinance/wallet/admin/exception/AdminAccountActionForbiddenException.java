package com.smartfinance.wallet.admin.exception;

public class AdminAccountActionForbiddenException extends RuntimeException {
    public AdminAccountActionForbiddenException() {
        super("Le blocage et le déblocage d'un compte administrateur ne sont pas autorisés.");
    }
}

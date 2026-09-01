package com.smartfinance.wallet.category.exception;

public class CategoryInUseException extends RuntimeException {
    public CategoryInUseException() {
        super("Cette catégorie est utilisée par des transactions.");
    }
}

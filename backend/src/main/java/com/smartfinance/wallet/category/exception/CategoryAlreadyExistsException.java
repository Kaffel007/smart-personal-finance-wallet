package com.smartfinance.wallet.category.exception;

public class CategoryAlreadyExistsException extends RuntimeException {

    public CategoryAlreadyExistsException() {
        super("Cette catégorie existe déjà.");
    }
}

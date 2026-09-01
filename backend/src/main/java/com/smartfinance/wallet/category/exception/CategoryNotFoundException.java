package com.smartfinance.wallet.category.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException() {
        super("Catégorie introuvable.");
    }
}

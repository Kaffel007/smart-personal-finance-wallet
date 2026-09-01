package com.smartfinance.wallet.transaction.dto;

import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.transaction.entity.FinancialTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialTransactionResponse(Long id, BigDecimal amount, CategoryType type,
        LocalDate transactionDate, String description, Long categoryId, String categoryName) {
    public static FinancialTransactionResponse from(FinancialTransaction transaction) {
        return new FinancialTransactionResponse(transaction.getId(), transaction.getAmount(),
                transaction.getType(), transaction.getTransactionDate(), transaction.getDescription(),
                transaction.getCategory().getId(), transaction.getCategory().getName());
    }
}

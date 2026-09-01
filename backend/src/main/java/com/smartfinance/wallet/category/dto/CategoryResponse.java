package com.smartfinance.wallet.category.dto;

import com.smartfinance.wallet.category.entity.Category;
import com.smartfinance.wallet.category.entity.CategoryType;

public record CategoryResponse(
        Long id,
        String name,
        CategoryType type
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType()
        );
    }
}

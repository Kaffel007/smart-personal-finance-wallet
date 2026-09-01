package com.smartfinance.wallet.category.dto;

import com.smartfinance.wallet.category.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Le nom de la catégorie est obligatoire.")
        @Size(max = 100, message = "Le nom de la catégorie ne doit pas dépasser 100 caractères.")
        String name,

        @NotNull(message = "Le type de catégorie est obligatoire.")
        CategoryType type
) {
}

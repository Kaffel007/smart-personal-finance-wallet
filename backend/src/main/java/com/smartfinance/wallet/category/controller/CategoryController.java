package com.smartfinance.wallet.category.controller;

import com.smartfinance.wallet.category.dto.CategoryResponse;
import com.smartfinance.wallet.category.dto.CreateCategoryRequest;
import com.smartfinance.wallet.category.dto.UpdateCategoryRequest;
import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.category.service.CategoryService;
import com.smartfinance.wallet.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(principal.id(), request));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(required = false) CategoryType type
    ) {
        return ResponseEntity.ok(categoryService.getCategories(principal.id(), type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(categoryService.getCategory(principal.id(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(principal.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long id
    ) {
        categoryService.deleteCategory(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}

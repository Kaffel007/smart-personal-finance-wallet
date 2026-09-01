package com.smartfinance.wallet.category.service;

import com.smartfinance.wallet.category.dto.CategoryResponse;
import com.smartfinance.wallet.category.dto.CreateCategoryRequest;
import com.smartfinance.wallet.category.dto.UpdateCategoryRequest;
import com.smartfinance.wallet.category.entity.Category;
import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.category.exception.CategoryAlreadyExistsException;
import com.smartfinance.wallet.category.exception.CategoryNotFoundException;
import com.smartfinance.wallet.category.repository.CategoryRepository;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AppUserRepository appUserRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            AppUserRepository appUserRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public CategoryResponse createCategory(Long userId, CreateCategoryRequest request) {
        String name = cleanName(request.name());
        String normalizedName = normalizeName(name);
        rejectDuplicate(userId, request.type(), normalizedName);

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(CategoryNotFoundException::new);
        Category category = new Category(user, name, normalizedName, request.type());

        try {
            return CategoryResponse.from(categoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryAlreadyExistsException();
        }
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Long userId, CategoryType type) {
        List<Category> categories = type == null
                ? categoryRepository.findAllByUserIdOrderByNameAsc(userId)
                : categoryRepository.findAllByUserIdAndTypeOrderByNameAsc(userId, type);
        return categories.stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long userId, Long categoryId) {
        return CategoryResponse.from(findOwnedCategory(userId, categoryId));
    }

    @Transactional
    public CategoryResponse updateCategory(
            Long userId,
            Long categoryId,
            UpdateCategoryRequest request
    ) {
        Category category = findOwnedCategory(userId, categoryId);
        String name = cleanName(request.name());
        String normalizedName = normalizeName(name);
        if (categoryRepository.existsByUserIdAndTypeAndNormalizedNameAndIdNot(
                userId, request.type(), normalizedName, categoryId)) {
            throw new CategoryAlreadyExistsException();
        }

        category.setName(name);
        category.setNormalizedName(normalizedName);
        category.setType(request.type());
        try {
            return CategoryResponse.from(categoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryAlreadyExistsException();
        }
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = findOwnedCategory(userId, categoryId);
        categoryRepository.delete(category);
        categoryRepository.flush();
    }

    private Category findOwnedCategory(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);
    }

    private void rejectDuplicate(Long userId, CategoryType type, String normalizedName) {
        if (categoryRepository.existsByUserIdAndTypeAndNormalizedName(
                userId, type, normalizedName)) {
            throw new CategoryAlreadyExistsException();
        }
    }

    private String cleanName(String name) {
        return name.trim();
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}

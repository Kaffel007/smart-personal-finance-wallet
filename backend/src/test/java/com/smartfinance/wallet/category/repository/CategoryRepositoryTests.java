package com.smartfinance.wallet.category.repository;

import com.smartfinance.wallet.category.entity.Category;
import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@DataJpaTest
class CategoryRepositoryTests {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void shouldPersistCategoryRelationTypesAndTimestamps() {
        AppUser user = saveUser("category-owner@example.test");
        Category income = categoryRepository.saveAndFlush(category(
                user, "Salaire", "salaire", CategoryType.INCOME
        ));
        Category expense = categoryRepository.saveAndFlush(category(
                user, "Transport", "transport", CategoryType.EXPENSE
        ));

        assertThat(income.getId()).isNotNull();
        assertThat(income.getUser().getId()).isEqualTo(user.getId());
        assertThat(income.getType()).isEqualTo(CategoryType.INCOME);
        assertThat(expense.getType()).isEqualTo(CategoryType.EXPENSE);
        assertThat(income.getNormalizedName()).isEqualTo("salaire");
        assertThat(income.getCreatedAt()).isNotNull();
        assertThat(income.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldQueryOnlyOwnedCategoriesAndFilterByType() {
        AppUser userA = saveUser("category-a@example.test");
        AppUser userB = saveUser("category-b@example.test");
        Category owned = categoryRepository.saveAndFlush(category(
                userA, "Alimentation", "alimentation", CategoryType.EXPENSE
        ));
        categoryRepository.saveAndFlush(category(
                userA, "Salaire", "salaire", CategoryType.INCOME
        ));
        categoryRepository.saveAndFlush(category(
                userB, "Transport", "transport", CategoryType.EXPENSE
        ));

        assertThat(categoryRepository.findAllByUserIdOrderByNameAsc(userA.getId()))
                .extracting(Category::getName)
                .containsExactly("Alimentation", "Salaire");
        assertThat(categoryRepository.findAllByUserIdAndTypeOrderByNameAsc(
                userA.getId(), CategoryType.EXPENSE))
                .extracting(Category::getName)
                .containsExactly("Alimentation");
        assertThat(categoryRepository.findByIdAndUserId(owned.getId(), userA.getId()))
                .isPresent();
        assertThat(categoryRepository.findByIdAndUserId(owned.getId(), userB.getId()))
                .isEmpty();
    }

    @Test
    void shouldEnforceUniqueUserTypeAndNormalizedName() {
        AppUser user = saveUser("unique-category@example.test");
        categoryRepository.saveAndFlush(category(
                user, "Transport", "transport", CategoryType.EXPENSE
        ));

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(category(
                user, "TRANSPORT", "transport", CategoryType.EXPENSE
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSameNormalizedNameForAnotherUserOrType() {
        AppUser userA = saveUser("allowed-a@example.test");
        AppUser userB = saveUser("allowed-b@example.test");

        categoryRepository.saveAndFlush(category(
                userA, "Divers", "divers", CategoryType.EXPENSE
        ));
        categoryRepository.saveAndFlush(category(
                userA, "Divers", "divers", CategoryType.INCOME
        ));
        categoryRepository.saveAndFlush(category(
                userB, "Divers", "divers", CategoryType.EXPENSE
        ));

        assertThat(categoryRepository.count()).isEqualTo(3);
    }

    private AppUser saveUser(String email) {
        return appUserRepository.saveAndFlush(new AppUser(
                "Category", "Owner", email,
                "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ012345",
                Role.USER
        ));
    }

    private Category category(
            AppUser user,
            String name,
            String normalizedName,
            CategoryType type
    ) {
        return new Category(user, name, normalizedName, type);
    }
}

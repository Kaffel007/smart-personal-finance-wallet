package com.smartfinance.wallet.category.service;

import com.smartfinance.wallet.category.dto.CategoryResponse;
import com.smartfinance.wallet.category.dto.CreateCategoryRequest;
import com.smartfinance.wallet.category.dto.UpdateCategoryRequest;
import com.smartfinance.wallet.category.entity.Category;
import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.category.exception.CategoryAlreadyExistsException;
import com.smartfinance.wallet.category.exception.CategoryNotFoundException;
import com.smartfinance.wallet.category.exception.CategoryInUseException;
import com.smartfinance.wallet.category.repository.CategoryRepository;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import com.smartfinance.wallet.transaction.repository.FinancialTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTests {

    private static final Long USER_ID = 10L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private FinancialTransactionRepository financialTransactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldCreateTrimmedNormalizedOwnedCategory() {
        AppUser user = user(USER_ID);
        when(categoryRepository.existsByUserIdAndTypeAndNormalizedName(
                USER_ID, CategoryType.EXPENSE, "transport")).thenReturn(false);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            ReflectionTestUtils.setField(category, "id", 31L);
            return category;
        });

        CategoryResponse response = categoryService.createCategory(
                USER_ID, new CreateCategoryRequest("  Transport  ", CategoryType.EXPENSE)
        );

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).saveAndFlush(captor.capture());
        Category saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getName()).isEqualTo("Transport");
        assertThat(saved.getNormalizedName()).isEqualTo("transport");
        assertThat(saved.getType()).isEqualTo(CategoryType.EXPENSE);
        assertThat(response).isEqualTo(new CategoryResponse(31L, "Transport", CategoryType.EXPENSE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Transport", "TRANSPORT", "  transport  "})
    void shouldRejectDuplicateRegardlessOfCaseOrOuterSpaces(String name) {
        when(categoryRepository.existsByUserIdAndTypeAndNormalizedName(
                USER_ID, CategoryType.EXPENSE, "transport")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(
                USER_ID, new CreateCategoryRequest(name, CategoryType.EXPENSE)))
                .isInstanceOf(CategoryAlreadyExistsException.class);

        verify(appUserRepository, never()).findById(any());
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldAllowSameNameForDifferentTypeOrUser() {
        AppUser user = user(USER_ID);
        when(categoryRepository.existsByUserIdAndTypeAndNormalizedName(
                USER_ID, CategoryType.INCOME, "divers")).thenReturn(false);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        categoryService.createCategory(
                USER_ID, new CreateCategoryRequest("Divers", CategoryType.INCOME)
        );

        verify(categoryRepository).existsByUserIdAndTypeAndNormalizedName(
                USER_ID, CategoryType.INCOME, "divers");
        verify(categoryRepository, never()).existsByUserIdAndTypeAndNormalizedName(
                99L, CategoryType.INCOME, "divers");
    }

    @Test
    void shouldTranslateConcurrentDatabaseConflict() {
        AppUser user = user(USER_ID);
        when(categoryRepository.existsByUserIdAndTypeAndNormalizedName(
                USER_ID, CategoryType.EXPENSE, "transport")).thenReturn(false);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenThrow(new DataIntegrityViolationException("concurrent unique constraint"));

        assertThatThrownBy(() -> categoryService.createCategory(
                USER_ID, new CreateCategoryRequest("Transport", CategoryType.EXPENSE)))
                .isInstanceOf(CategoryAlreadyExistsException.class)
                .hasMessage("Cette catégorie existe déjà.");
    }

    @Test
    void shouldListOnlyRequestedUserAndOptionalType() {
        Category expense = category(user(USER_ID), 1L, "Alimentation", CategoryType.EXPENSE);
        when(categoryRepository.findAllByUserIdOrderByNameAsc(USER_ID))
                .thenReturn(List.of(expense));
        when(categoryRepository.findAllByUserIdAndTypeOrderByNameAsc(
                USER_ID, CategoryType.EXPENSE)).thenReturn(List.of(expense));

        assertThat(categoryService.getCategories(USER_ID, null))
                .containsExactly(new CategoryResponse(1L, "Alimentation", CategoryType.EXPENSE));
        assertThat(categoryService.getCategories(USER_ID, CategoryType.EXPENSE))
                .hasSize(1);
        verify(categoryRepository).findAllByUserIdOrderByNameAsc(USER_ID);
        verify(categoryRepository).findAllByUserIdAndTypeOrderByNameAsc(
                USER_ID, CategoryType.EXPENSE);
    }

    @Test
    void shouldGetOwnedCategoryAndHideOtherUsersCategory() {
        Category owned = category(user(USER_ID), 8L, "Salaire", CategoryType.INCOME);
        when(categoryRepository.findByIdAndUserId(8L, USER_ID))
                .thenReturn(Optional.of(owned));
        when(categoryRepository.findByIdAndUserId(8L, 99L))
                .thenReturn(Optional.empty());

        assertThat(categoryService.getCategory(USER_ID, 8L).name()).isEqualTo("Salaire");
        assertThatThrownBy(() -> categoryService.getCategory(99L, 8L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void shouldUpdateNameTypeAndNormalizedNameWithoutChangingOwner() {
        AppUser owner = user(USER_ID);
        Category category = category(owner, 8L, "Divers", CategoryType.INCOME);
        when(categoryRepository.findByIdAndUserId(8L, USER_ID))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsByUserIdAndTypeAndNormalizedNameAndIdNot(
                USER_ID, CategoryType.EXPENSE, "loisirs", 8L)).thenReturn(false);
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);

        CategoryResponse response = categoryService.updateCategory(
                USER_ID, 8L, new UpdateCategoryRequest("  Loisirs ", CategoryType.EXPENSE)
        );

        assertThat(category.getUser()).isSameAs(owner);
        assertThat(category.getName()).isEqualTo("Loisirs");
        assertThat(category.getNormalizedName()).isEqualTo("loisirs");
        assertThat(category.getType()).isEqualTo(CategoryType.EXPENSE);
        assertThat(response.type()).isEqualTo(CategoryType.EXPENSE);
    }

    @Test
    void shouldRejectDuplicateUpdateOrOtherUsersUpdate() {
        Category category = category(user(USER_ID), 8L, "Divers", CategoryType.INCOME);
        when(categoryRepository.findByIdAndUserId(8L, USER_ID))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsByUserIdAndTypeAndNormalizedNameAndIdNot(
                USER_ID, CategoryType.EXPENSE, "transport", 8L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(
                USER_ID, 8L, new UpdateCategoryRequest("Transport", CategoryType.EXPENSE)))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        assertThatThrownBy(() -> categoryService.updateCategory(
                99L, 8L, new UpdateCategoryRequest("Transport", CategoryType.EXPENSE)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void shouldDeleteOnlyOwnedCategory() {
        Category category = category(user(USER_ID), 8L, "Divers", CategoryType.INCOME);
        when(categoryRepository.findByIdAndUserId(8L, USER_ID))
                .thenReturn(Optional.of(category));

        categoryService.deleteCategory(USER_ID, 8L);

        verify(categoryRepository).delete(category);
        verify(categoryRepository).flush();
        assertThatThrownBy(() -> categoryService.deleteCategory(99L, 8L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void shouldRejectDeletingUsedCategory() {
        Category category = category(user(USER_ID), 8L, "Transport", CategoryType.EXPENSE);
        when(categoryRepository.findByIdAndUserId(8L, USER_ID)).thenReturn(Optional.of(category));
        when(financialTransactionRepository.existsByCategoryId(8L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(USER_ID, 8L))
                .isInstanceOf(CategoryInUseException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void shouldRejectTypeChangeButAllowRenameForUsedCategory() {
        Category category = category(user(USER_ID), 8L, "Transport", CategoryType.EXPENSE);
        when(categoryRepository.findByIdAndUserId(8L, USER_ID)).thenReturn(Optional.of(category));
        when(financialTransactionRepository.existsByCategoryId(8L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(
                USER_ID, 8L, new UpdateCategoryRequest("Salaire", CategoryType.INCOME)))
                .isInstanceOf(CategoryInUseException.class);

        when(categoryRepository.existsByUserIdAndTypeAndNormalizedNameAndIdNot(
                USER_ID, CategoryType.EXPENSE, "transport quotidien", 8L)).thenReturn(false);
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);
        CategoryResponse renamed = categoryService.updateCategory(
                USER_ID, 8L, new UpdateCategoryRequest("Transport quotidien", CategoryType.EXPENSE));
        assertThat(renamed.name()).isEqualTo("Transport quotidien");
    }

    private AppUser user(Long id) {
        AppUser user = new AppUser(
                "Test", "User", "user-" + id + "@example.test", "hash", Role.USER
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Category category(AppUser user, Long id, String name, CategoryType type) {
        Category category = new Category(user, name, name.toLowerCase(), type);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}

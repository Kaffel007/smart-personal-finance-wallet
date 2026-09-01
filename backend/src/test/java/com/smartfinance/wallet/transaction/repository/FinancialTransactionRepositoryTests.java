package com.smartfinance.wallet.transaction.repository;

import com.smartfinance.wallet.category.entity.*;
import com.smartfinance.wallet.category.repository.CategoryRepository;
import com.smartfinance.wallet.transaction.entity.FinancialTransaction;
import com.smartfinance.wallet.user.entity.*;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test") @DataJpaTest
class FinancialTransactionRepositoryTests {
    @Autowired FinancialTransactionRepository repository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired AppUserRepository userRepository;

    @Test void shouldPersistRelationsMoneyTypeDatesAndTimestamps() {
        AppUser user = user("tx-repository@example.test");
        Category category = category(user, "Transport", CategoryType.EXPENSE);
        FinancialTransaction saved = repository.saveAndFlush(new FinancialTransaction(user, category,
                category.getType(), new BigDecimal("45.5000"), LocalDate.now(), "Taxi"));
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getCategory().getId()).isEqualTo(category.getId());
        assertThat(saved.getAmount()).isEqualByComparingTo("45.5000");
        assertThat(saved.getType()).isEqualTo(CategoryType.EXPENSE);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test void shouldIsolateSortAndFilterTransactions() {
        AppUser a = user("tx-a@example.test"), b = user("tx-b@example.test");
        Category expense = category(a, "Transport", CategoryType.EXPENSE);
        Category income = category(a, "Salaire", CategoryType.INCOME);
        Category other = category(b, "Secret", CategoryType.EXPENSE);
        FinancialTransaction old = tx(a, expense, "10", LocalDate.now().minusDays(1));
        FinancialTransaction recent = tx(a, income, "20", LocalDate.now());
        tx(b, other, "99", LocalDate.now());
        assertThat(repository.findAllByUserIdOrderByTransactionDateDescIdDesc(a.getId()))
                .extracting(FinancialTransaction::getId).containsExactly(recent.getId(), old.getId());
        assertThat(repository.findAllByUserIdAndTypeOrderByTransactionDateDescIdDesc(a.getId(), CategoryType.EXPENSE))
                .containsExactly(old);
        assertThat(repository.findAllByUserIdAndCategoryIdOrderByTransactionDateDescIdDesc(a.getId(), income.getId()))
                .containsExactly(recent);
        assertThat(repository.findByIdAndUserId(old.getId(), b.getId())).isEmpty();
    }

    @Test void shouldEnforcePositiveAmountAndForeignKeys() {
        AppUser user = user("tx-constraint@example.test");
        Category category = category(user, "Transport", CategoryType.EXPENSE);
        assertThatThrownBy(() -> repository.saveAndFlush(new FinancialTransaction(user, category,
                category.getType(), BigDecimal.ZERO, LocalDate.now(), null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private AppUser user(String email) { return userRepository.saveAndFlush(new AppUser("Test", "User", email, "hash", Role.USER)); }
    private Category category(AppUser user, String name, CategoryType type) { return categoryRepository.saveAndFlush(new Category(user, name, name.toLowerCase(), type)); }
    private FinancialTransaction tx(AppUser user, Category category, String amount, LocalDate date) {
        return repository.saveAndFlush(new FinancialTransaction(user, category, category.getType(), new BigDecimal(amount), date, null));
    }
}

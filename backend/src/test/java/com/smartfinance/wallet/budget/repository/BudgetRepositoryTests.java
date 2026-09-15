package com.smartfinance.wallet.budget.repository;

import com.smartfinance.wallet.budget.entity.Budget;
import com.smartfinance.wallet.category.entity.*;
import com.smartfinance.wallet.category.repository.CategoryRepository;
import com.smartfinance.wallet.transaction.entity.FinancialTransaction;
import com.smartfinance.wallet.transaction.repository.FinancialTransactionRepository;
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
class BudgetRepositoryTests {
    @Autowired BudgetRepository repository;
    @Autowired FinancialTransactionRepository transactionRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired AppUserRepository userRepository;

    @Test void shouldPersistRelationsAmountPeriodAndTimestamps() {
        AppUser user=user("budget-repository@example.test"); Category category=category(user,"Transport");
        Budget budget=repository.saveAndFlush(new Budget(user,category,2026,9,new BigDecimal("200.0000")));
        assertThat(budget.getId()).isNotNull(); assertThat(budget.getUser().getId()).isEqualTo(user.getId());
        assertThat(budget.getCategory().getId()).isEqualTo(category.getId()); assertThat(budget.getAmount()).isEqualByComparingTo("200.0000");
        assertThat(budget.getYear()).isEqualTo(2026); assertThat(budget.getMonth()).isEqualTo(9);
        assertThat(budget.getCreatedAt()).isNotNull(); assertThat(budget.getUpdatedAt()).isNotNull();
    }

    @Test void shouldEnforceUniqueOwnerCategoryPeriodAndAllowOtherMonthOrUser() {
        AppUser a=user("budget-unique-a@example.test"), b=user("budget-unique-b@example.test");
        Category ca=category(a,"Transport"), cb=category(b,"Transport");
        repository.saveAndFlush(new Budget(a,ca,2026,9,BigDecimal.TEN));
        repository.saveAndFlush(new Budget(a,ca,2026,10,BigDecimal.ONE));
        repository.saveAndFlush(new Budget(b,cb,2026,9,BigDecimal.ONE));
        assertThatThrownBy(() -> repository.saveAndFlush(new Budget(a,ca,2026,9,BigDecimal.ONE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void shouldFilterAndIsolateBudgets() {
        AppUser a=user("budget-filter-a@example.test"),b=user("budget-filter-b@example.test");
        Category ca=category(a,"Transport"), cb=category(b,"Other");
        Budget owned=repository.saveAndFlush(new Budget(a,ca,2026,9,BigDecimal.TEN));
        repository.saveAndFlush(new Budget(a,ca,2025,8,BigDecimal.TEN)); repository.saveAndFlush(new Budget(b,cb,2026,9,BigDecimal.TEN));
        assertThat(repository.findAllFiltered(a.getId(),2026,9,ca.getId())).containsExactly(owned);
        assertThat(repository.findByIdAndUserId(owned.getId(),b.getId())).isEmpty();
    }

    @Test void shouldSumOnlyOwnerCategoryExpenseAndPeriod() {
        AppUser a=user("sum-a@example.test"),b=user("sum-b@example.test");
        Category ca=category(a,"Transport"), other=category(a,"Other"), cb=category(b,"Transport");
        tx(a,ca,"50",LocalDate.of(2026,9,1),CategoryType.EXPENSE);
        tx(a,ca,"25",LocalDate.of(2026,9,30),CategoryType.EXPENSE);
        tx(a,ca,"100",LocalDate.of(2026,10,1),CategoryType.EXPENSE);
        tx(a,other,"100",LocalDate.of(2026,9,1),CategoryType.EXPENSE);
        tx(b,cb,"100",LocalDate.of(2026,9,1),CategoryType.EXPENSE);
        tx(a,ca,"100",LocalDate.of(2026,9,1),CategoryType.INCOME);
        assertThat(transactionRepository.sumExpenses(a.getId(),ca.getId(),LocalDate.of(2026,9,1),LocalDate.of(2026,10,1)))
                .isEqualByComparingTo("75.0000");
        assertThat(transactionRepository.sumExpenses(a.getId(),ca.getId(),LocalDate.of(2025,9,1),LocalDate.of(2025,10,1)))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    private AppUser user(String email){return userRepository.saveAndFlush(new AppUser("Test","User",email,"hash",Role.USER));}
    private Category category(AppUser u,String name){return categoryRepository.saveAndFlush(new Category(u,name,name.toLowerCase(),CategoryType.EXPENSE));}
    private void tx(AppUser u,Category c,String amount,LocalDate date,CategoryType type){transactionRepository.saveAndFlush(new FinancialTransaction(u,c,type,new BigDecimal(amount),date,null));}
}

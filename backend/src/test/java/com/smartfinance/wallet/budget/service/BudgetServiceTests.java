package com.smartfinance.wallet.budget.service;

import com.smartfinance.wallet.budget.dto.*;
import com.smartfinance.wallet.budget.entity.Budget;
import com.smartfinance.wallet.budget.exception.*;
import com.smartfinance.wallet.budget.repository.BudgetRepository;
import com.smartfinance.wallet.category.entity.*;
import com.smartfinance.wallet.category.exception.CategoryNotFoundException;
import com.smartfinance.wallet.category.repository.CategoryRepository;
import com.smartfinance.wallet.transaction.repository.FinancialTransactionRepository;
import com.smartfinance.wallet.user.entity.*;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTests {
    @Mock BudgetRepository repository; @Mock CategoryRepository categoryRepository;
    @Mock AppUserRepository userRepository; @Mock FinancialTransactionRepository transactionRepository;
    @InjectMocks BudgetService service;

    @Test void shouldCreateExpenseBudgetForJwtOwnerAndCalculateZeroProgress() {
        AppUser user=user(1L); Category category=category(user,2L,CategoryType.EXPENSE);
        when(categoryRepository.findByIdAndUserId(2L,1L)).thenReturn(Optional.of(category)); when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.saveAndFlush(any())).thenAnswer(i->{Budget b=i.getArgument(0);ReflectionTestUtils.setField(b,"id",3L);return b;});
        when(transactionRepository.sumExpenses(eq(1L),eq(2L),any(),any())).thenReturn(BigDecimal.ZERO);
        BudgetResponse result=service.create(1L,new CreateBudgetRequest(2L,2026,9,new BigDecimal("200")));
        assertThat(result.spent()).isZero(); assertThat(result.remaining()).isEqualByComparingTo("200"); assertThat(result.usagePercent()).isEqualByComparingTo("0.00");
    }

    @Test void shouldRejectIncomeMissingOtherUserCategoryAndDuplicate() {
        Category income=category(user(1L),2L,CategoryType.INCOME);
        when(categoryRepository.findByIdAndUserId(2L,1L)).thenReturn(Optional.of(income));
        assertThatThrownBy(()->service.create(1L,new CreateBudgetRequest(2L,2026,9,BigDecimal.TEN))).isInstanceOf(BudgetCategoryTypeException.class);
        when(categoryRepository.findByIdAndUserId(9L,1L)).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.create(1L,new CreateBudgetRequest(9L,2026,9,BigDecimal.TEN))).isInstanceOf(CategoryNotFoundException.class);
        Category expense=category(user(1L),4L,CategoryType.EXPENSE); when(categoryRepository.findByIdAndUserId(4L,1L)).thenReturn(Optional.of(expense));
        when(repository.existsByUserIdAndCategoryIdAndYearAndMonth(1L,4L,2026,9)).thenReturn(true);
        assertThatThrownBy(()->service.create(1L,new CreateBudgetRequest(4L,2026,9,BigDecimal.TEN))).isInstanceOf(BudgetAlreadyExistsException.class);
    }

    @Test void shouldCalculateDynamicProgressIncludingOverspend() {
        Budget budget=budget(user(1L),category(user(1L),2L,CategoryType.EXPENSE),3L,"200");
        when(repository.findByIdAndUserId(3L,1L)).thenReturn(Optional.of(budget));
        when(transactionRepository.sumExpenses(eq(1L),eq(2L),eq(LocalDate.of(2026,9,1)),eq(LocalDate.of(2026,10,1))))
                .thenReturn(new BigDecimal("250.0000"));
        BudgetResponse result=service.getOne(1L,3L);
        assertThat(result.spent()).isEqualByComparingTo("250"); assertThat(result.remaining()).isEqualByComparingTo("-50");
        assertThat(result.usagePercent()).isEqualByComparingTo("125.00");
    }

    @Test void shouldListWithFiltersAndHideCrossUserDetail() {
        Budget budget=budget(user(1L),category(user(1L),2L,CategoryType.EXPENSE),3L,"100");
        when(repository.findAllFiltered(1L,2026,9,2L)).thenReturn(List.of(budget)); when(transactionRepository.sumExpenses(anyLong(),anyLong(),any(),any())).thenReturn(BigDecimal.TEN);
        assertThat(service.getAll(1L,2026,9,2L)).hasSize(1);
        when(repository.findByIdAndUserId(3L,99L)).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.getOne(99L,3L)).isInstanceOf(BudgetNotFoundException.class);
    }

    @Test void shouldUpdatePeriodCategoryAmountRecalculateAndRejectDuplicate() {
        AppUser user=user(1L); Category old=category(user,2L,CategoryType.EXPENSE), replacement=category(user,4L,CategoryType.EXPENSE);
        Budget budget=budget(user,old,3L,"100"); when(repository.findByIdAndUserId(3L,1L)).thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndUserId(4L,1L)).thenReturn(Optional.of(replacement)); when(repository.saveAndFlush(budget)).thenReturn(budget);
        when(transactionRepository.sumExpenses(anyLong(),anyLong(),any(),any())).thenReturn(new BigDecimal("50"));
        BudgetResponse result=service.update(1L,3L,new UpdateBudgetRequest(4L,2026,10,new BigDecimal("250")));
        assertThat(result.categoryId()).isEqualTo(4L); assertThat(result.remaining()).isEqualByComparingTo("200");
        when(repository.existsByUserIdAndCategoryIdAndYearAndMonthAndIdNot(1L,4L,2026,10,3L)).thenReturn(true);
        assertThatThrownBy(()->service.update(1L,3L,new UpdateBudgetRequest(4L,2026,10,BigDecimal.TEN))).isInstanceOf(BudgetAlreadyExistsException.class);
    }

    @Test void shouldDeleteOnlyOwnedBudget() {
        Budget budget=budget(user(1L),category(user(1L),2L,CategoryType.EXPENSE),3L,"100"); when(repository.findByIdAndUserId(3L,1L)).thenReturn(Optional.of(budget));
        service.delete(1L,3L); verify(repository).delete(budget); verify(repository).flush();
        assertThatThrownBy(()->service.delete(99L,3L)).isInstanceOf(BudgetNotFoundException.class);
    }

    private AppUser user(Long id){AppUser u=new AppUser("Test","User","b"+id+"@test","hash",Role.USER);ReflectionTestUtils.setField(u,"id",id);return u;}
    private Category category(AppUser u,Long id,CategoryType type){Category c=new Category(u,"Category","category",type);ReflectionTestUtils.setField(c,"id",id);return c;}
    private Budget budget(AppUser u,Category c,Long id,String amount){Budget b=new Budget(u,c,2026,9,new BigDecimal(amount));ReflectionTestUtils.setField(b,"id",id);return b;}
}

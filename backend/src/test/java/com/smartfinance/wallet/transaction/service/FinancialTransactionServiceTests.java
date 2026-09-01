package com.smartfinance.wallet.transaction.service;

import com.smartfinance.wallet.category.entity.*;
import com.smartfinance.wallet.category.exception.CategoryNotFoundException;
import com.smartfinance.wallet.category.repository.CategoryRepository;
import com.smartfinance.wallet.transaction.dto.*;
import com.smartfinance.wallet.transaction.entity.FinancialTransaction;
import com.smartfinance.wallet.transaction.exception.FinancialTransactionNotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialTransactionServiceTests {
    @Mock FinancialTransactionRepository repository;
    @Mock CategoryRepository categoryRepository;
    @Mock AppUserRepository userRepository;
    @InjectMocks FinancialTransactionService service;

    @Test void shouldCreateOwnedTransactionWithDerivedTypeAndCleanDescription() {
        AppUser user = user(1L); Category category = category(user, 2L, CategoryType.EXPENSE);
        when(categoryRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.saveAndFlush(any())).thenAnswer(i -> { FinancialTransaction tx=i.getArgument(0); ReflectionTestUtils.setField(tx,"id",3L); return tx; });
        FinancialTransactionResponse result = service.create(1L, new CreateFinancialTransactionRequest(2L,
                new BigDecimal("45.5000"), LocalDate.now(), "  Taxi  "));
        assertThat(result.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(result.description()).isEqualTo("Taxi");
        assertThat(result.categoryId()).isEqualTo(2L);
    }

    @Test void shouldRejectMissingOrOtherUsersCategory() {
        when(categoryRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(1L, new CreateFinancialTransactionRequest(9L,
                BigDecimal.ONE, LocalDate.now(), null))).isInstanceOf(CategoryNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test void shouldListWithCombinedFiltersAndUserScope() {
        AppUser user=user(1L); Category category=category(user,2L,CategoryType.INCOME);
        FinancialTransaction tx=transaction(user,category,3L);
        when(repository.findAllByUserIdAndTypeAndCategoryIdOrderByTransactionDateDescIdDesc(1L,CategoryType.INCOME,2L)).thenReturn(List.of(tx));
        assertThat(service.getAll(1L,CategoryType.INCOME,2L)).extracting(FinancialTransactionResponse::id).containsExactly(3L);
    }

    @Test void shouldHideOtherUsersTransaction() {
        when(repository.findByIdAndUserId(3L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getOne(99L,3L)).isInstanceOf(FinancialTransactionNotFoundException.class);
    }

    @Test void shouldUpdateCategoryAndRecalculateTypeWithoutChangingOwner() {
        AppUser user=user(1L); Category old=category(user,2L,CategoryType.EXPENSE), replacement=category(user,4L,CategoryType.INCOME);
        FinancialTransaction tx=transaction(user,old,3L);
        when(repository.findByIdAndUserId(3L,1L)).thenReturn(Optional.of(tx));
        when(categoryRepository.findByIdAndUserId(4L,1L)).thenReturn(Optional.of(replacement));
        when(repository.saveAndFlush(tx)).thenReturn(tx);
        FinancialTransactionResponse result=service.update(1L,3L,new UpdateFinancialTransactionRequest(4L,new BigDecimal("100"),LocalDate.now(),"  "));
        assertThat(result.type()).isEqualTo(CategoryType.INCOME);
        assertThat(result.description()).isNull();
        assertThat(tx.getUser()).isSameAs(user);
    }

    @Test void shouldDeleteOnlyOwnedTransaction() {
        AppUser user=user(1L); FinancialTransaction tx=transaction(user,category(user,2L,CategoryType.EXPENSE),3L);
        when(repository.findByIdAndUserId(3L,1L)).thenReturn(Optional.of(tx));
        service.delete(1L,3L);
        verify(repository).delete(tx); verify(repository).flush();
    }

    private AppUser user(Long id) { AppUser u=new AppUser("Test","User","u"+id+"@test","hash",Role.USER); ReflectionTestUtils.setField(u,"id",id); return u; }
    private Category category(AppUser u,Long id,CategoryType type) { Category c=new Category(u,"Category","category",type); ReflectionTestUtils.setField(c,"id",id); return c; }
    private FinancialTransaction transaction(AppUser u,Category c,Long id) { FinancialTransaction t=new FinancialTransaction(u,c,c.getType(),BigDecimal.ONE,LocalDate.now(),null); ReflectionTestUtils.setField(t,"id",id); return t; }
}

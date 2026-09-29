package com.smartfinance.wallet.transaction.repository;

import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.transaction.entity.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    Optional<FinancialTransaction> findByIdAndUserId(Long id, Long userId);
    List<FinancialTransaction> findAllByUserIdOrderByTransactionDateDescIdDesc(Long userId);
    List<FinancialTransaction> findAllByUserIdAndTypeOrderByTransactionDateDescIdDesc(Long userId, CategoryType type);
    List<FinancialTransaction> findAllByUserIdAndCategoryIdOrderByTransactionDateDescIdDesc(Long userId, Long categoryId);
    List<FinancialTransaction> findAllByUserIdAndTypeAndCategoryIdOrderByTransactionDateDescIdDesc(Long userId, CategoryType type, Long categoryId);
    boolean existsByCategoryId(Long categoryId);

    @Query("""
            select coalesce(sum(t.amount), 0) from FinancialTransaction t
            where t.user.id = :userId and t.category.id = :categoryId
              and t.type = com.smartfinance.wallet.category.entity.CategoryType.EXPENSE
              and t.transactionDate >= :startDate and t.transactionDate < :endDate
            """)
    BigDecimal sumExpenses(@Param("userId") Long userId, @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            select coalesce(sum(t.amount), 0) from FinancialTransaction t
            where t.user.id = :userId and t.type = :type
              and t.transactionDate >= :startDate and t.transactionDate < :endDate
            """)
    BigDecimal sumByUserAndTypeAndPeriod(@Param("userId") Long userId,
            @Param("type") CategoryType type, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            select count(t) from FinancialTransaction t
            where t.user.id = :userId
              and t.transactionDate >= :startDate and t.transactionDate < :endDate
            """)
    long countByUserAndPeriod(@Param("userId") Long userId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            select coalesce(sum(t.amount), 0) from FinancialTransaction t
            where t.user.id = :userId
              and t.type = com.smartfinance.wallet.category.entity.CategoryType.EXPENSE
              and t.category.id in :categoryIds
              and t.transactionDate >= :startDate and t.transactionDate < :endDate
            """)
    BigDecimal sumExpensesByCategoriesAndPeriod(@Param("userId") Long userId,
            @Param("categoryIds") Set<Long> categoryIds, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}

package com.smartfinance.wallet.budget.repository;

import com.smartfinance.wallet.budget.entity.Budget;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import java.math.BigDecimal;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndCategoryIdAndYearAndMonth(Long userId, Long categoryId, int year, int month);
    boolean existsByUserIdAndCategoryIdAndYearAndMonthAndIdNot(Long userId, Long categoryId, int year, int month, Long id);
    boolean existsByCategoryId(Long categoryId);

    @Query("""
            select b from Budget b
            where b.user.id = :userId
              and (:year is null or b.year = :year)
              and (:month is null or b.month = :month)
              and (:categoryId is null or b.category.id = :categoryId)
            order by b.year desc, b.month desc, b.category.name asc
            """)
    List<Budget> findAllFiltered(@Param("userId") Long userId, @Param("year") Integer year,
            @Param("month") Integer month, @Param("categoryId") Long categoryId);

    @Query("""
            select coalesce(sum(b.amount), 0) from Budget b
            where b.user.id = :userId and b.year = :year and b.month = :month
            """)
    BigDecimal sumAmountByUserAndPeriod(@Param("userId") Long userId,
            @Param("year") int year, @Param("month") int month);

    long countByUserIdAndYearAndMonth(Long userId, int year, int month);

    @Query("""
            select b.category.id from Budget b
            where b.user.id = :userId and b.year = :year and b.month = :month
            """)
    Set<Long> findCategoryIdsByUserAndPeriod(@Param("userId") Long userId,
            @Param("year") int year, @Param("month") int month);
}

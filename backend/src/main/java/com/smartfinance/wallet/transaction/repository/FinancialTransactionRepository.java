package com.smartfinance.wallet.transaction.repository;

import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.transaction.entity.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    Optional<FinancialTransaction> findByIdAndUserId(Long id, Long userId);
    List<FinancialTransaction> findAllByUserIdOrderByTransactionDateDescIdDesc(Long userId);
    List<FinancialTransaction> findAllByUserIdAndTypeOrderByTransactionDateDescIdDesc(Long userId, CategoryType type);
    List<FinancialTransaction> findAllByUserIdAndCategoryIdOrderByTransactionDateDescIdDesc(Long userId, Long categoryId);
    List<FinancialTransaction> findAllByUserIdAndTypeAndCategoryIdOrderByTransactionDateDescIdDesc(Long userId, CategoryType type, Long categoryId);
    boolean existsByCategoryId(Long categoryId);
}

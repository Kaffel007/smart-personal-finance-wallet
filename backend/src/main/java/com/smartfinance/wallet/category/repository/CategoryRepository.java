package com.smartfinance.wallet.category.repository;

import com.smartfinance.wallet.category.entity.Category;
import com.smartfinance.wallet.category.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByUserIdOrderByNameAsc(Long userId);

    List<Category> findAllByUserIdAndTypeOrderByNameAsc(Long userId, CategoryType type);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndTypeAndNormalizedName(
            Long userId,
            CategoryType type,
            String normalizedName
    );

    boolean existsByUserIdAndTypeAndNormalizedNameAndIdNot(
            Long userId,
            CategoryType type,
            String normalizedName,
            Long id
    );
}

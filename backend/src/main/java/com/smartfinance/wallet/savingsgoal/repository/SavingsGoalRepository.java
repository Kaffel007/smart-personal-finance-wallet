package com.smartfinance.wallet.savingsgoal.repository;

import com.smartfinance.wallet.savingsgoal.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    Optional<SavingsGoal> findByIdAndUserId(Long id, Long userId);
    List<SavingsGoal> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    @Query("select coalesce(sum(g.targetAmount), 0) from SavingsGoal g where g.user.id = :userId")
    BigDecimal sumTargetAmountByUserId(@Param("userId") Long userId);

    @Query("select coalesce(sum(g.savedAmount), 0) from SavingsGoal g where g.user.id = :userId")
    BigDecimal sumSavedAmountByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);
}

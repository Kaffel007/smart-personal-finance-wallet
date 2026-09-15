package com.smartfinance.wallet.savingsgoal.service;

import com.smartfinance.wallet.savingsgoal.dto.*;
import com.smartfinance.wallet.savingsgoal.entity.SavingsGoal;
import com.smartfinance.wallet.savingsgoal.exception.SavingsGoalNotFoundException;
import com.smartfinance.wallet.savingsgoal.repository.SavingsGoalRepository;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SavingsGoalService {
    private final SavingsGoalRepository repository;
    private final AppUserRepository userRepository;

    public SavingsGoalService(SavingsGoalRepository repository, AppUserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SavingsGoalResponse create(Long userId, CreateSavingsGoalRequest request) {
        AppUser user = userRepository.findById(userId).orElseThrow(SavingsGoalNotFoundException::new);
        BigDecimal savedAmount = request.savedAmount() == null ? BigDecimal.ZERO : request.savedAmount();
        SavingsGoal goal = new SavingsGoal(user, request.name().trim(), request.targetAmount(),
                savedAmount, request.targetDate());
        return response(repository.saveAndFlush(goal));
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> getAll(Long userId) {
        return repository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponse getOne(Long userId, Long goalId) {
        return response(findOwned(userId, goalId));
    }

    @Transactional
    public SavingsGoalResponse update(Long userId, Long goalId, UpdateSavingsGoalRequest request) {
        SavingsGoal goal = findOwned(userId, goalId);
        goal.setName(request.name().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setSavedAmount(request.savedAmount());
        goal.setTargetDate(request.targetDate());
        return response(repository.saveAndFlush(goal));
    }

    @Transactional
    public void delete(Long userId, Long goalId) {
        repository.delete(findOwned(userId, goalId));
        repository.flush();
    }

    private SavingsGoal findOwned(Long userId, Long goalId) {
        return repository.findByIdAndUserId(goalId, userId)
                .orElseThrow(SavingsGoalNotFoundException::new);
    }

    private SavingsGoalResponse response(SavingsGoal goal) {
        BigDecimal progress = goal.getSavedAmount().multiply(BigDecimal.valueOf(100))
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
        return SavingsGoalResponse.from(goal, progress);
    }
}

package com.smartfinance.wallet.savingsgoal.service;

import com.smartfinance.wallet.savingsgoal.dto.*;
import com.smartfinance.wallet.savingsgoal.entity.SavingsGoal;
import com.smartfinance.wallet.savingsgoal.exception.SavingsGoalNotFoundException;
import com.smartfinance.wallet.savingsgoal.repository.SavingsGoalRepository;
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
class SavingsGoalServiceTests {
    @Mock SavingsGoalRepository repository;
    @Mock AppUserRepository userRepository;
    @InjectMocks SavingsGoalService service;

    @Test
    void shouldCreateForJwtOwnerTrimNameAndDefaultMissingSavedAmountToZero() {
        AppUser user = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            SavingsGoal goal = invocation.getArgument(0);
            ReflectionTestUtils.setField(goal, "id", 2L);
            return goal;
        });

        SavingsGoalResponse response = service.create(1L,
                new CreateSavingsGoalRequest("  Voiture  ", new BigDecimal("30000"), null, null));

        assertThat(response.name()).isEqualTo("Voiture");
        assertThat(response.savedAmount()).isZero();
        assertThat(response.remainingAmount()).isEqualByComparingTo("30000");
        assertThat(response.progressPercent()).isEqualByComparingTo("0.00");
        ArgumentCaptor<SavingsGoal> captor = ArgumentCaptor.forClass(SavingsGoal.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void shouldCalculateNormalCompleteAndOverTargetProgressWithHalfUpRounding() {
        assertProgress("30000", "8000", "22000", "26.67");
        assertProgress("1000", "1000", "0", "100.00");
        assertProgress("1000", "1200", "-200", "120.00");
    }

    @Test
    void shouldListOnlyRepositoryResultsForOwnerAndHideCrossUserDetail() {
        SavingsGoal goal = goal(user(1L), 3L, "Voyage", "100", "25", null);
        when(repository.findAllByUserIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(goal));
        assertThat(service.getAll(1L)).extracting(SavingsGoalResponse::id).containsExactly(3L);
        when(repository.findByIdAndUserId(3L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getOne(99L, 3L)).isInstanceOf(SavingsGoalNotFoundException.class);
    }

    @Test
    void shouldUpdateEveryMutableFieldAndRecalculate() {
        SavingsGoal goal = goal(user(1L), 3L, "Ancien", "100", "10", null);
        when(repository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(goal));
        when(repository.saveAndFlush(goal)).thenReturn(goal);
        LocalDate date = LocalDate.of(2028, 1, 1);

        SavingsGoalResponse response = service.update(1L, 3L,
                new UpdateSavingsGoalRequest("  Maison  ", new BigDecimal("250"),
                        new BigDecimal("50"), date));

        assertThat(response.name()).isEqualTo("Maison");
        assertThat(response.targetAmount()).isEqualByComparingTo("250");
        assertThat(response.savedAmount()).isEqualByComparingTo("50");
        assertThat(response.targetDate()).isEqualTo(date);
        assertThat(response.remainingAmount()).isEqualByComparingTo("200");
        assertThat(response.progressPercent()).isEqualByComparingTo("20.00");
    }

    @Test
    void shouldDeleteOnlyOwnedGoal() {
        SavingsGoal goal = goal(user(1L), 3L, "Goal", "100", "10", null);
        when(repository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(goal));
        service.delete(1L, 3L);
        verify(repository).delete(goal);
        verify(repository).flush();
        assertThatThrownBy(() -> service.delete(99L, 3L)).isInstanceOf(SavingsGoalNotFoundException.class);
    }

    private void assertProgress(String target, String saved, String remaining, String progress) {
        SavingsGoal goal = goal(user(1L), 3L, "Goal", target, saved, null);
        when(repository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(goal));
        SavingsGoalResponse response = service.getOne(1L, 3L);
        assertThat(response.remainingAmount()).isEqualByComparingTo(remaining);
        assertThat(response.progressPercent()).isEqualByComparingTo(progress);
    }

    private AppUser user(Long id) {
        AppUser user = new AppUser("Test", "User", "goal" + id + "@test", "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private SavingsGoal goal(AppUser user, Long id, String name, String target, String saved, LocalDate date) {
        SavingsGoal goal = new SavingsGoal(user, name, new BigDecimal(target), new BigDecimal(saved), date);
        ReflectionTestUtils.setField(goal, "id", id);
        return goal;
    }
}

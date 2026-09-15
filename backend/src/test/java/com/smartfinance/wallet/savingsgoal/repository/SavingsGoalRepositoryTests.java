package com.smartfinance.wallet.savingsgoal.repository;

import com.smartfinance.wallet.savingsgoal.entity.SavingsGoal;
import com.smartfinance.wallet.user.entity.*;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
class SavingsGoalRepositoryTests {
    @Autowired SavingsGoalRepository repository;
    @Autowired AppUserRepository userRepository;

    @Test
    void shouldPersistOwnerAmountsOptionalDateAndTimestampsThroughMigrations() {
        AppUser user = user("goal-repository@example.test");
        SavingsGoal goal = repository.saveAndFlush(new SavingsGoal(user, "Voiture",
                new BigDecimal("30000.0000"), new BigDecimal("8000.0000"), null));

        assertThat(goal.getId()).isNotNull();
        assertThat(goal.getUser().getId()).isEqualTo(user.getId());
        assertThat(goal.getTargetAmount()).isEqualByComparingTo("30000.0000");
        assertThat(goal.getSavedAmount()).isEqualByComparingTo("8000.0000");
        assertThat(goal.getTargetDate()).isNull();
        assertThat(goal.getCreatedAt()).isNotNull();
        assertThat(goal.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindOnlyGoalsOwnedByRequestedUserInStableOrder() {
        AppUser a = user("goal-a@example.test");
        AppUser b = user("goal-b@example.test");
        SavingsGoal first = repository.saveAndFlush(goal(a, "Premier", "100", "10", null));
        SavingsGoal second = repository.saveAndFlush(goal(a, "Second", "200", "20", LocalDate.of(2027, 6, 30)));
        SavingsGoal privateGoal = repository.saveAndFlush(goal(b, "Privé", "999", "999", null));

        assertThat(repository.findByIdAndUserId(first.getId(), a.getId())).contains(first);
        assertThat(repository.findByIdAndUserId(privateGoal.getId(), a.getId())).isEmpty();
        assertThat(repository.findAllByUserIdOrderByCreatedAtDescIdDesc(a.getId()))
                .containsExactly(second, first).doesNotContain(privateGoal);
    }

    private AppUser user(String email) {
        return userRepository.saveAndFlush(new AppUser("Test", "User", email, "hash", Role.USER));
    }

    private SavingsGoal goal(AppUser user, String name, String target, String saved, LocalDate date) {
        return new SavingsGoal(user, name, new BigDecimal(target), new BigDecimal(saved), date);
    }
}

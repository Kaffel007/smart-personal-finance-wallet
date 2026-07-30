package com.smartfinance.wallet.user.repository;

import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@DataJpaTest
class AppUserRepositoryTests {

    private static final String FICTIONAL_PASSWORD_HASH =
            "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ012345";

    @Autowired
    private AppUserRepository appUserRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldPersistAndFindAppUserWithGeneratedValues() {
        AppUser user = createUser("alex.martin@example.test", Role.USER);
        user.setEnabled(true);
        user.setBlocked(false);

        AppUser savedUser = appUserRepository.saveAndFlush(user);
        entityManager.clear();

        AppUser foundUser = appUserRepository.findByEmail("alex.martin@example.test")
                .orElseThrow();
        String persistedRole = (String) entityManager
                .createNativeQuery("SELECT role FROM app_users WHERE id = :id")
                .setParameter("id", savedUser.getId())
                .getSingleResult();

        assertThat(savedUser.getId()).isNotNull();
        assertThat(foundUser.getRole()).isEqualTo(Role.USER);
        assertThat(persistedRole).isEqualTo("USER");
        assertThat(foundUser.isEnabled()).isTrue();
        assertThat(foundUser.isBlocked()).isFalse();
        assertThat(foundUser.getCreatedAt()).isNotNull();
        assertThat(foundUser.getUpdatedAt()).isNotNull();
        assertThat(foundUser.getPasswordHash()).isEqualTo(FICTIONAL_PASSWORD_HASH);
        assertThat(appUserRepository.existsByEmail("alex.martin@example.test")).isTrue();
        assertThat(appUserRepository.existsByEmail("missing@example.test")).isFalse();
    }

    @Test
    void shouldPersistAdminRoleAndAccountStatuses() {
        AppUser user = createUser("admin.fixture@example.test", Role.ADMIN);
        user.setEnabled(false);
        user.setBlocked(true);

        AppUser savedUser = appUserRepository.saveAndFlush(user);
        entityManager.clear();

        AppUser foundUser = appUserRepository.findById(savedUser.getId()).orElseThrow();

        assertThat(foundUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(foundUser.isEnabled()).isFalse();
        assertThat(foundUser.isBlocked()).isTrue();
    }

    @Test
    void shouldUpdateUpdatedAtWithoutChangingCreatedAt() {
        AppUser user = appUserRepository.saveAndFlush(
                createUser("updated.fixture@example.test", Role.USER)
        );
        Instant createdAt = user.getCreatedAt();
        Instant initialUpdatedAt = user.getUpdatedAt();

        user.setFirstName("Updated");
        appUserRepository.saveAndFlush(user);

        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    @Test
    void shouldRejectExactDuplicateEmail() {
        appUserRepository.saveAndFlush(
                createUser("duplicate@example.test", Role.USER)
        );

        AppUser duplicate = createUser("duplicate@example.test", Role.USER);

        assertThatThrownBy(() -> appUserRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private AppUser createUser(String email, Role role) {
        return new AppUser(
                "Alex",
                "Martin",
                email,
                FICTIONAL_PASSWORD_HASH,
                role
        );
    }
}

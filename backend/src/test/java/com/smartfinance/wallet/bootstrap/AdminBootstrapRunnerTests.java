package com.smartfinance.wallet.bootstrap;

import com.smartfinance.wallet.config.AdminBootstrapProperties;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTests {

    private static final String RAW_PASSWORD = "fictional-admin-passphrase";
    private static final String GENERIC_ERROR =
            "Configuration du compte administrateur initial invalide.";

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ApplicationArguments applicationArguments;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldDoNothingWhenBootstrapIsDisabled() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties();
        properties.setEnabled(false);

        runner(properties).run(applicationArguments);

        verify(appUserRepository, never()).findByEmail(any());
        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldCreateNormalizedAdminWithBcryptAndSafeDefaults() {
        AdminBootstrapProperties properties = validProperties();
        when(appUserRepository.findByEmail("admin.fixture@example.com"))
                .thenReturn(Optional.empty());
        when(appUserRepository.saveAndFlush(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        runner(properties).run(applicationArguments);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).saveAndFlush(captor.capture());
        AppUser admin = captor.getValue();
        assertThat(admin.getFirstName()).isEqualTo("Initial");
        assertThat(admin.getLastName()).isEqualTo("Administrator");
        assertThat(admin.getEmail()).isEqualTo("admin.fixture@example.com");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isEnabled()).isTrue();
        assertThat(admin.isBlocked()).isFalse();
        assertThat(admin.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, admin.getPasswordHash())).isTrue();
    }

    @Test
    void shouldBeIdempotentWithoutReplacingExistingAdminData() {
        AdminBootstrapProperties properties = validProperties();
        AtomicReference<AppUser> storedUser = new AtomicReference<>();
        when(appUserRepository.findByEmail("admin.fixture@example.com"))
                .thenAnswer(invocation -> Optional.ofNullable(storedUser.get()));
        when(appUserRepository.saveAndFlush(any(AppUser.class)))
                .thenAnswer(invocation -> {
                    AppUser user = invocation.getArgument(0);
                    storedUser.set(user);
                    return user;
                });
        AdminBootstrapRunner runner = runner(properties);

        runner.run(applicationArguments);
        String originalHash = storedUser.get().getPasswordHash();
        storedUser.get().setEnabled(false);
        storedUser.get().setBlocked(true);
        runner.run(applicationArguments);

        verify(appUserRepository, times(1)).saveAndFlush(any(AppUser.class));
        assertThat(storedUser.get().getPasswordHash()).isEqualTo(originalHash);
        assertThat(storedUser.get().getRole()).isEqualTo(Role.ADMIN);
        assertThat(storedUser.get().isEnabled()).isFalse();
        assertThat(storedUser.get().isBlocked()).isTrue();
    }

    @Test
    void shouldNeverPromoteExistingUserWithSameEmail() {
        AdminBootstrapProperties properties = validProperties();
        AppUser existingUser = new AppUser(
                "Existing", "User", "admin.fixture@example.com",
                "existing-internal-hash", Role.USER
        );
        when(appUserRepository.findByEmail("admin.fixture@example.com"))
                .thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> runner(properties).run(applicationArguments))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(GENERIC_ERROR)
                .hasMessageNotContaining(RAW_PASSWORD);

        assertThat(existingUser.getRole()).isEqualTo(Role.USER);
        assertThat(existingUser.getPasswordHash()).isEqualTo("existing-internal-hash");
        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @MethodSource("invalidConfigurations")
    void shouldRejectInvalidEnabledConfigurationWithoutExposingPassword(
            String email,
            String password,
            String firstName,
            String lastName
    ) {
        AdminBootstrapProperties properties = validProperties();
        properties.setEmail(email);
        properties.setPassword(password);
        properties.setFirstName(firstName);
        properties.setLastName(lastName);

        assertThatThrownBy(() -> runner(properties).run(applicationArguments))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(GENERIC_ERROR)
                .hasMessageNotContaining(password == null ? "unused-null-marker" : password);

        verify(appUserRepository, never()).saveAndFlush(any());
    }

    private AdminBootstrapRunner runner(AdminBootstrapProperties properties) {
        return new AdminBootstrapRunner(
                properties, appUserRepository, passwordEncoder, validator
        );
    }

    private AdminBootstrapProperties validProperties() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties();
        properties.setEnabled(true);
        properties.setEmail("  ADMIN.FIXTURE@EXAMPLE.COM  ");
        properties.setPassword(RAW_PASSWORD);
        properties.setFirstName("  Initial  ");
        properties.setLastName("  Administrator  ");
        return properties;
    }

    private static Stream<Arguments> invalidConfigurations() {
        String over72Utf8Bytes = "é".repeat(37);
        return Stream.of(
                Arguments.of(null, RAW_PASSWORD, "Initial", "Administrator"),
                Arguments.of("invalid-email", RAW_PASSWORD, "Initial", "Administrator"),
                Arguments.of("admin@example.com", null, "Initial", "Administrator"),
                Arguments.of("admin@example.com", "too-short", "Initial", "Administrator"),
                Arguments.of("admin@example.com", over72Utf8Bytes, "Initial", "Administrator"),
                Arguments.of("admin@example.com", RAW_PASSWORD, "   ", "Administrator"),
                Arguments.of("admin@example.com", RAW_PASSWORD, "Initial", "   ")
        );
    }
}

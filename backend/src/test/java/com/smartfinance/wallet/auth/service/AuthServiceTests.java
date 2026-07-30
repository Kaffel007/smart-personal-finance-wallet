package com.smartfinance.wallet.auth.service;

import com.smartfinance.wallet.auth.dto.RegisterRequest;
import com.smartfinance.wallet.auth.dto.UserSummaryResponse;
import com.smartfinance.wallet.common.exception.EmailAlreadyUsedException;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    private static final String RAW_PASSWORD = "fictional-passphrase";
    private static final String PASSWORD_HASH = "encoded-fictional-password";

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterNormalizedUserWithSafeDefaults() {
        RegisterRequest request = new RegisterRequest(
                "  Sara  ",
                "  Martin  ",
                "  SARA.EXAMPLE@EXAMPLE.COM  ",
                RAW_PASSWORD
        );
        when(appUserRepository.existsByEmail("sara.example@example.com")).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(appUserRepository.saveAndFlush(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserSummaryResponse response = authService.register(request);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).saveAndFlush(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();

        assertThat(savedUser.getFirstName()).isEqualTo("Sara");
        assertThat(savedUser.getLastName()).isEqualTo("Martin");
        assertThat(savedUser.getEmail()).isEqualTo("sara.example@example.com");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.isBlocked()).isFalse();
        assertThat(savedUser.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        verify(passwordEncoder).encode(RAW_PASSWORD);

        assertThat(response.firstName()).isEqualTo("Sara");
        assertThat(response.lastName()).isEqualTo("Martin");
        assertThat(response.email()).isEqualTo("sara.example@example.com");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(Arrays.stream(UserSummaryResponse.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("password", "passwordHash");
    }

    @Test
    void shouldRejectExistingNormalizedEmailBeforeEncoding() {
        RegisterRequest request = new RegisterRequest(
                "Sara",
                "Martin",
                " SARA@EXAMPLE.COM ",
                RAW_PASSWORD
        );
        when(appUserRepository.existsByEmail("sara@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessage("Cette adresse e-mail ne peut pas être utilisée.");

        verify(passwordEncoder, never()).encode(any());
        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldTranslateConcurrentUniqueConstraintViolation() {
        RegisterRequest request = new RegisterRequest(
                "Sara",
                "Martin",
                "sara@example.com",
                RAW_PASSWORD
        );
        when(appUserRepository.existsByEmail("sara@example.com")).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(appUserRepository.saveAndFlush(any(AppUser.class)))
                .thenThrow(new DataIntegrityViolationException("Fictional unique constraint"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessage("Cette adresse e-mail ne peut pas être utilisée.");
    }
}

package com.smartfinance.wallet.account.service;

import com.smartfinance.wallet.account.dto.ChangePasswordRequest;
import com.smartfinance.wallet.account.dto.UpdateEmailRequest;
import com.smartfinance.wallet.account.dto.UpdateProfileRequest;
import com.smartfinance.wallet.account.exception.AccountNotFoundException;
import com.smartfinance.wallet.account.exception.CurrentPasswordIncorrectException;
import com.smartfinance.wallet.common.exception.EmailAlreadyUsedException;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTests {
    private static final String FICTIONAL_PASSWORD = "fictional-account-test-passphrase";
    @Mock AppUserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AccountService service;

    @ParameterizedTest
    @ValueSource(strings = {"get", "profile", "email", "password"})
    void shouldReportMissingCurrentAccountWithoutSaving(String operation) {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> {
            switch (operation) {
                case "get" -> service.getAccount(42L);
                case "profile" -> service.updateProfile(42L, new UpdateProfileRequest("Sara", "Martin"));
                case "email" -> service.updateEmail(42L, new UpdateEmailRequest("new@example.test", FICTIONAL_PASSWORD));
                default -> service.changePassword(42L, new ChangePasswordRequest(FICTIONAL_PASSWORD, "fictional-new-password"));
            }
        }).isInstanceOf(AccountNotFoundException.class);
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldTranslateConcurrentEmailUniqueConstraintFailureWithoutLeakingSql() {
        AppUser user = new AppUser("Sara", "Martin", "old@example.test", "synthetic-hash", Role.USER);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(FICTIONAL_PASSWORD, "synthetic-hash")).thenReturn(true);
        when(userRepository.findByEmail("new@example.test")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(user)).thenThrow(new DataIntegrityViolationException("synthetic sql detail"));
        assertThatThrownBy(() -> service.updateEmail(42L, new UpdateEmailRequest(" NEW@EXAMPLE.TEST ", FICTIONAL_PASSWORD)))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessage("Cette adresse e-mail ne peut pas être utilisée.");
    }

    @Test
    void shouldVerifyCurrentPasswordBeforeLookingUpAnotherEmail() {
        AppUser user = new AppUser("Sara", "Martin", "old@example.test", "synthetic-hash", Role.USER);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(FICTIONAL_PASSWORD, "synthetic-hash")).thenReturn(false);
        assertThatThrownBy(() -> service.updateEmail(42L, new UpdateEmailRequest("taken@example.test", FICTIONAL_PASSWORD)))
                .isInstanceOf(CurrentPasswordIncorrectException.class);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).saveAndFlush(any());
    }
}

package com.smartfinance.wallet.auth.service;

import com.smartfinance.wallet.auth.dto.LoginRequest;
import com.smartfinance.wallet.auth.dto.LoginResponse;
import com.smartfinance.wallet.auth.dto.RegisterRequest;
import com.smartfinance.wallet.auth.dto.UserSummaryResponse;
import com.smartfinance.wallet.common.exception.EmailAlreadyUsedException;
import com.smartfinance.wallet.common.exception.InvalidCredentialsException;
import com.smartfinance.wallet.security.jwt.JwtService;
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
import java.util.Optional;

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

    @Mock
    private JwtService jwtService;

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
    @Test
    void shouldLoginWithNormalizedEmailUsingMatchesAndReturnSafeResponse() {
        AppUser user = createLoginUser(Role.USER);
        LoginRequest request = new LoginRequest("  SARA@EXAMPLE.COM  ", RAW_PASSWORD);
        when(appUserRepository.findByEmail("sara@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("signed-test-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(request);

        verify(appUserRepository).findByEmail("sara@example.com");
        verify(passwordEncoder).matches(RAW_PASSWORD, PASSWORD_HASH);
        verify(passwordEncoder, never()).encode(any());
        verify(jwtService).generateToken(user);
        verify(appUserRepository, never()).save(any());
        verify(appUserRepository, never()).saveAndFlush(any());
        assertThat(response.accessToken()).isEqualTo("signed-test-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
        assertThat(response.user().email()).isEqualTo("sara@example.com");
        assertThat(response.user().role()).isEqualTo(Role.USER);
        assertThat(Arrays.stream(LoginResponse.class.getRecordComponents())
                .map(component -> component.getName()))
                .containsExactly("accessToken", "tokenType", "expiresInSeconds", "user");
        assertThat(Arrays.stream(UserSummaryResponse.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("password", "passwordHash", "enabled", "blocked");
    }

    @Test
    void shouldRejectMissingUserWithGenericAuthenticationError() {
        when(appUserRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertGenericAuthenticationError(
                () -> authService.login(new LoginRequest("missing@example.com", RAW_PASSWORD))
        );

        verify(passwordEncoder, never()).matches(any(), any());
        verify(passwordEncoder, never()).encode(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldRejectWrongPasswordWithSameGenericAuthenticationError() {
        AppUser user = createLoginUser(Role.USER);
        when(appUserRepository.findByEmail("sara@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", PASSWORD_HASH)).thenReturn(false);

        assertGenericAuthenticationError(
                () -> authService.login(new LoginRequest("sara@example.com", "wrong-password"))
        );

        verify(passwordEncoder).matches("wrong-password", PASSWORD_HASH);
        verify(passwordEncoder, never()).encode(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldRejectBlockedAccountWithSameGenericAuthenticationError() {
        AppUser user = createLoginUser(Role.USER);
        user.setBlocked(true);
        when(appUserRepository.findByEmail("sara@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

        assertGenericAuthenticationError(
                () -> authService.login(new LoginRequest("sara@example.com", RAW_PASSWORD))
        );
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldRejectDisabledAccountWithSameGenericAuthenticationError() {
        AppUser user = createLoginUser(Role.USER);
        user.setEnabled(false);
        when(appUserRepository.findByEmail("sara@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

        assertGenericAuthenticationError(
                () -> authService.login(new LoginRequest("sara@example.com", RAW_PASSWORD))
        );
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldNotModifyUserDuringSuccessfulLogin() {
        AppUser user = createLoginUser(Role.ADMIN);
        when(appUserRepository.findByEmail("sara@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("signed-test-token");

        authService.login(new LoginRequest("sara@example.com", RAW_PASSWORD));

        assertThat(user.getFirstName()).isEqualTo("Sara");
        assertThat(user.getLastName()).isEqualTo("Martin");
        assertThat(user.getEmail()).isEqualTo("sara@example.com");
        assertThat(user.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isBlocked()).isFalse();
        verify(appUserRepository, never()).save(any());
        verify(appUserRepository, never()).saveAndFlush(any());
    }

    private AppUser createLoginUser(Role role) {
        AppUser user = new AppUser("Sara", "Martin", "sara@example.com", PASSWORD_HASH, role);
        user.setEnabled(true);
        user.setBlocked(false);
        return user;
    }

    private void assertGenericAuthenticationError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Identifiants incorrects ou compte indisponible.");
    }
}

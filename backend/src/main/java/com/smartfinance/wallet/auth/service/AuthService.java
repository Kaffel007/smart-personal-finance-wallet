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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserSummaryResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (appUserRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyUsedException();
        }

        String passwordHash = passwordEncoder.encode(request.password());
        AppUser user = new AppUser(
                request.firstName().trim(),
                request.lastName().trim(),
                normalizedEmail,
                passwordHash,
                Role.USER
        );
        user.setEnabled(true);
        user.setBlocked(false);

        try {
            AppUser savedUser = appUserRepository.saveAndFlush(user);
            return UserSummaryResponse.from(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyUsedException();
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        AppUser user = appUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())
                || !user.isEnabled()
                || user.isBlocked()) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateToken(user);
        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                UserSummaryResponse.from(user)
        );
    }
}

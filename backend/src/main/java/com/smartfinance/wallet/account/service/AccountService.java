package com.smartfinance.wallet.account.service;

import com.smartfinance.wallet.account.dto.AccountResponse;
import com.smartfinance.wallet.account.dto.ChangePasswordRequest;
import com.smartfinance.wallet.account.dto.UpdateEmailRequest;
import com.smartfinance.wallet.account.dto.UpdateProfileRequest;
import com.smartfinance.wallet.account.exception.AccountNotFoundException;
import com.smartfinance.wallet.account.exception.CurrentPasswordIncorrectException;
import com.smartfinance.wallet.account.exception.PasswordUnchangedException;
import com.smartfinance.wallet.common.exception.EmailAlreadyUsedException;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class AccountService {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long currentUserId) {
        return AccountResponse.from(findCurrentUser(currentUserId));
    }

    @Transactional
    public AccountResponse updateProfile(Long currentUserId, UpdateProfileRequest request) {
        AppUser user = findCurrentUser(currentUserId);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        return AccountResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public AccountResponse updateEmail(Long currentUserId, UpdateEmailRequest request) {
        AppUser user = findCurrentUser(currentUserId);
        verifyCurrentPassword(request.currentPassword(), user);
        String normalizedEmail = request.newEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(currentUserId)).isPresent()) {
            throw new EmailAlreadyUsedException();
        }
        user.setEmail(normalizedEmail);
        try {
            return AccountResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            // The database unique constraint also arbitrates concurrent email changes.
            throw new EmailAlreadyUsedException();
        }
    }

    @Transactional
    public void changePassword(Long currentUserId, ChangePasswordRequest request) {
        AppUser user = findCurrentUser(currentUserId);
        verifyCurrentPassword(request.currentPassword(), user);
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new PasswordUnchangedException();
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);
    }

    private AppUser findCurrentUser(Long currentUserId) {
        return userRepository.findById(currentUserId).orElseThrow(AccountNotFoundException::new);
    }

    private void verifyCurrentPassword(String currentPassword, AppUser user) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new CurrentPasswordIncorrectException();
        }
    }
}

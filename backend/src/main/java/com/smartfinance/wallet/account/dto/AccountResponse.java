package com.smartfinance.wallet.account.dto;

import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import java.time.Instant;

public record AccountResponse(Long id, String firstName, String lastName, String email,
        Role role, boolean enabled, boolean blocked, Instant createdAt) {
    public static AccountResponse from(AppUser user) {
        return new AccountResponse(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole(), user.isEnabled(), user.isBlocked(), user.getCreatedAt());
    }
}

package com.smartfinance.wallet.admin.dto;

import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import java.time.Instant;

public record AdminUserResponse(Long id, String firstName, String lastName, String email,
        Role role, boolean enabled, boolean blocked, Instant createdAt) {
    public static AdminUserResponse from(AppUser user) {
        return new AdminUserResponse(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole(), user.isEnabled(), user.isBlocked(), user.getCreatedAt());
    }
}
